/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KParameter.Kind;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.util.Assert;

/**
 * Value object to represent a Kotlin {@code copy} method.
 *
 * @author Mark Paluch
 * @since 2.1
 */
class KotlinCopyMethod {

	private final Method publicCopyMethod;
	private final Method syntheticCopyMethod;
	private final int parameterCount;
	private final KFunction<?> copyFunction;

	/**
	 * @param publicCopyMethod Compiler-generated public {@code copy} method accepting all properties.
	 * @param syntheticCopyMethod Compiler-generated synthetic {@code copy$default} variant of the copy method accepting
	 *          the original instance and defaulting masks.
	 */
	private KotlinCopyMethod(Method publicCopyMethod, Method syntheticCopyMethod) {

		this.publicCopyMethod = publicCopyMethod;
		this.syntheticCopyMethod = syntheticCopyMethod;
		this.copyFunction = ReflectJvmMapping.getKotlinFunction(publicCopyMethod);
		this.parameterCount = copyFunction.getParameters().size();
	}

	/**
	 * Attempt to lookup the Kotlin {@code copy} method. Lookup happens in two stages: Find the synthetic copy method and
	 * then attempt to resolve its public variant.
	 *
	 * @param type the class.
	 * @return {@link Optional} {@link KotlinCopyMethod}.
	 */
	public static Optional<KotlinCopyMethod> findCopyMethod(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		Optional<Method> syntheticCopyMethod = findSyntheticCopyMethod(type);

		if (!syntheticCopyMethod.isPresent()) {
			return Optional.empty();
		}

		Optional<Method> publicCopyMethod = syntheticCopyMethod.flatMap(KotlinCopyMethod::findPublicCopyMethod);

		return publicCopyMethod.map(method -> new KotlinCopyMethod(method, syntheticCopyMethod.get()));
	}

	public Method getPublicCopyMethod() {
		return this.publicCopyMethod;
	}

	public Method getSyntheticCopyMethod() {
		return this.syntheticCopyMethod;
	}

	public int getParameterCount() {
		return this.parameterCount;
	}

	public KFunction<?> getCopyFunction() {
		return this.copyFunction;
	}

	/**
	 * Check whether the {@link PersistentProperty} is accepted as part of the copy method.
	 *
	 * @param property
	 * @return
	 */
	boolean supportsProperty(PersistentProperty<?> property) {
		return forProperty(property).isPresent();
	}

	/**
	 * Create metadata for {@literal copy$default} invocation.
	 *
	 * @param property
	 * @return
	 */
	Optional<KotlinCopyByProperty> forProperty(PersistentProperty<?> property) {

		int index = KotlinCopyByProperty.findIndex(copyFunction, property.getName());

		if (index == -1) {
			return Optional.empty();
		}

		return Optional.of(new KotlinCopyByProperty(copyFunction, property));
	}

	boolean shouldUsePublicCopyMethod(PersistentEntity<?, ?> entity) {

		List<PersistentProperty<?>> persistentProperties = new ArrayList<>();
		entity.doWithProperties((SimplePropertyHandler) persistentProperties::add);

		if (persistentProperties.size() > 1) {
			return false;
		}

		if (publicCopyMethod.getParameterCount() != 1) {
			return false;
		}

		if (Modifier.isStatic(publicCopyMethod.getModifiers())) {
			return false;
		}

		Class<?>[] parameterTypes = publicCopyMethod.getParameterTypes();

		for (int i = 0; i < parameterTypes.length; i++) {
			if (!parameterTypes[i].equals(persistentProperties.get(i).getType())) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	private static Optional<Method> findPublicCopyMethod(Method defaultKotlinMethod) {

		Class<?> type = defaultKotlinMethod.getDeclaringClass();
		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(type);

		KFunction<?> primaryConstructor = KClasses.getPrimaryConstructor(kotlinClass);

		if (primaryConstructor == null) {
			return Optional.empty();
		}

		List<KParameter> constructorArguments = primaryConstructor.getParameters() //
				.stream() //
				.filter(it -> it.getKind() == Kind.VALUE) //
				.collect(Collectors.toList());

		return Arrays.stream(type.getDeclaredMethods()).filter(it -> it.getName().equals("copy") //
				&& !it.isSynthetic() //
				&& !Modifier.isStatic(it.getModifiers()) //
				&& it.getReturnType().equals(type) //
				&& it.getParameterCount() == constructorArguments.size()) //
				.filter(it -> {

					KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(it);

					if (kotlinFunction == null) {
						return false;
					}

					return parameterMatches(constructorArguments, kotlinFunction);
				}).findFirst();
	}

	private static boolean parameterMatches(List<KParameter> constructorArguments, KFunction<?> kotlinFunction) {

		boolean foundInstance = false;
		int constructorArgIndex = 0;

		for (KParameter parameter : kotlinFunction.getParameters()) {

			if (parameter.getKind() == Kind.INSTANCE) {
				foundInstance = true;
				continue;
			}

			if (constructorArguments.size() <= constructorArgIndex) {
				return false;
			}

			KParameter constructorParameter = constructorArguments.get(constructorArgIndex);

			if (!constructorParameter.getName().equals(parameter.getName())
					|| !constructorParameter.getType().equals(parameter.getType())) {
				return false;
			}

			constructorArgIndex++;
		}

		return foundInstance;
	}

	private static Optional<Method> findSyntheticCopyMethod(Class<?> type) {

		return Arrays.stream(type.getDeclaredMethods()) //
				.filter(it -> it.getName().equals("copy$default") //
						&& Modifier.isStatic(it.getModifiers()) //
						&& it.getReturnType().equals(type))
				.filter(Method::isSynthetic) //
				.findFirst();
	}

	/**
	 * Value object to represent Kotlin {@literal copy$default} invocation metadata.
	 *
	 * @author Mark Paluch
	 */
	static class KotlinCopyByProperty {

		private final int parameterPosition;
		private final int parameterCount;
		private final KotlinDefaultMask defaultMask;

		KotlinCopyByProperty(KFunction<?> copyFunction, PersistentProperty<?> property) {

			this.parameterPosition = findIndex(copyFunction, property.getName());
			this.parameterCount = copyFunction.getParameters().size();
			this.defaultMask = KotlinDefaultMask.from(copyFunction, it -> property.getName().equals(it.getName()));
		}

		static int findIndex(KFunction<?> function, String parameterName) {

			for (KParameter parameter : function.getParameters()) {
				if (parameterName.equals(parameter.getName())) {
					return parameter.getIndex();
				}
			}

			return -1;
		}

		public int getParameterPosition() {
			return this.parameterPosition;
		}

		public int getParameterCount() {
			return this.parameterCount;
		}

		public KotlinDefaultMask getDefaultMask() {
			return this.defaultMask;
		}
	}
}
