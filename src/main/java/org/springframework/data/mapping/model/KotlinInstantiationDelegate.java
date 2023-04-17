/*
 * Copyright 2023 the original author or authors.
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
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;

/**
 * Delegate to allocate instantiation arguments and to resolve the actual constructor to call for inline/value class
 * instantiation. This class captures all Kotlin-specific quirks, from default constructor resolution up to mangled
 * inline-type handling so instantiator-components can delegate to this class for constructor translation and
 * constructor argument extraction.
 *
 * @author Mark Paluch
 * @since 3.1
 */
class KotlinInstantiationDelegate {

	private final KFunction<?> constructor;
	private final List<KParameter> kParameters;
	private final List<Function<Object, Object>> wrappers = new ArrayList<>();
	private final Constructor<?> constructorToInvoke;
	private final boolean hasDefaultConstructorMarker;

	public KotlinInstantiationDelegate(PreferredConstructor<?, ?> preferredConstructor,
			Constructor<?> constructorToInvoke) {

		KFunction<?> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(preferredConstructor.getConstructor());

		if (kotlinConstructor == null) {
			throw new IllegalArgumentException(
					"No corresponding Kotlin constructor found for " + preferredConstructor.getConstructor());
		}

		this.constructor = kotlinConstructor;
		this.kParameters = kotlinConstructor.getParameters();
		this.constructorToInvoke = constructorToInvoke;
		this.hasDefaultConstructorMarker = hasDefaultConstructorMarker(constructorToInvoke.getParameters());

		for (KParameter kParameter : kParameters) {
			wrappers.add(getWrapper(kParameter, true));
		}
	}

	/**
	 * @param kParameter the kotlin parameter to wrap.
	 * @param domainTypeUsage optional in the domain type require value type casting. Inner/nested ones don't. This is
	 *          because calling the synthetic constructor with an optional parameter requires an inline class while
	 *          optional parameters via reflection are handled within Kotlin-Reflection.
	 * @return
	 */
	private Function<Object, Object> getWrapper(KParameter kParameter, boolean domainTypeUsage) {

		if (kParameter.getType().getClassifier()instanceof KClass<?> kc && kc.isValue()
				&& (!domainTypeUsage || kParameter.isOptional())) {

			KFunction<?> ctor = kc.getConstructors().iterator().next();

			// using reflection to construct a value class wrapper. Everything
			// else would require too many levels of indirections.

			KParameter nested = ctor.getParameters().get(0);
			Function<Object, Object> wrapper = getWrapper(nested, false);

			return o -> ctor.call(wrapper.apply(o));
		}

		return Function.identity();
	}

	static boolean hasDefaultConstructorMarker(java.lang.reflect.Parameter[] parameters) {

		return parameters.length > 0
				&& parameters[parameters.length - 1].getType().getName().equals("kotlin.jvm.internal.DefaultConstructorMarker");
	}

	/**
	 * @return number of constructor arguments.
	 */
	public int getRequiredParameterCount() {

		return hasDefaultConstructorMarker ? constructorToInvoke.getParameterCount()
				: (constructorToInvoke.getParameterCount()
						+ KotlinDefaultMask.getMaskCount(constructorToInvoke.getParameterCount())
						+ /* DefaultConstructorMarker */1);
	}

	/**
	 * Extract the actual construction arguments for a direct constructor call.
	 *
	 * @param params
	 * @param entityCreator
	 * @param provider
	 * @return
	 * @param <P>
	 */
	public <P extends PersistentProperty<P>> Object[] extractInvocationArguments(Object[] params,
			@Nullable InstanceCreatorMetadata<P> entityCreator, ParameterValueProvider<P> provider) {

		if (entityCreator == null) {
			throw new IllegalArgumentException("EntityCreator must not be null");
		}

		int userParameterCount = kParameters.size();

		List<Parameter<Object, P>> parameters = entityCreator.getParameters();

		// Prepare user-space arguments
		for (int i = 0; i < userParameterCount; i++) {

			Parameter<Object, P> parameter = parameters.get(i);
			params[i] = provider.getParameterValue(parameter);
		}

		KotlinDefaultMask defaultMask = KotlinDefaultMask.forConstructor(constructor, it -> {

			int index = kParameters.indexOf(it);

			Parameter<Object, P> parameter = parameters.get(index);
			Class<Object> type = parameter.getType().getType();

			if (it.isOptional() && (params[index] == null)) {
				if (type.isPrimitive()) {

					// apply primitive defaulting to prevent NPE on primitive downcast
					params[index] = ReflectionUtils.getPrimitiveDefault(type);
				}
				return false;
			}

			return true;
		});

		// late rewrapping to indicate potential absence of parameters for defaulting
		for (int i = 0; i < userParameterCount; i++) {
			params[i] = wrappers.get(i).apply(params[i]);
		}

		int[] defaulting = defaultMask.getDefaulting();
		// append nullability masks to creation arguments
		for (int i = 0; i < defaulting.length; i++) {
			params[userParameterCount + i] = defaulting[i];
		}

		return params;
	}

	/**
	 * Resolves a {@link PreferredConstructor} to a synthetic Kotlin constructor accepting the same user-space parameters
	 * suffixed by Kotlin-specifics required for defaulting and the {@code kotlin.jvm.internal.DefaultConstructorMarker}.
	 *
	 * @since 2.0
	 * @author Mark Paluch
	 */

	@SuppressWarnings("unchecked")
	@Nullable
	public static PreferredConstructor<?, ?> resolveKotlinJvmConstructor(
			PreferredConstructor<?, ?> preferredConstructor) {

		Constructor<?> hit = doResolveKotlinConstructor(preferredConstructor.getConstructor());

		if (hit != null) {
			return new PreferredConstructor<>(hit, preferredConstructor.getParameters().toArray(new Parameter[0]));
		}

		return null;
	}

	@Nullable
	private static Constructor<?> doResolveKotlinConstructor(Constructor<?> detectedConstructor) {

		Class<?> entityType = detectedConstructor.getDeclaringClass();
		Constructor<?> hit = null;
		KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(detectedConstructor);

		for (Constructor<?> candidate : entityType.getDeclaredConstructors()) {

			// use only synthetic constructors
			if (!candidate.isSynthetic()) {
				continue;
			}

			java.lang.reflect.Parameter[] detectedConstructorParameters = detectedConstructor.getParameters();
			java.lang.reflect.Parameter[] candidateParameters = candidate.getParameters();

			if (!KotlinInstantiationDelegate.hasDefaultConstructorMarker(detectedConstructorParameters)) {

				// candidates must contain at least two additional parameters (int, DefaultConstructorMarker).
				// Number of defaulting masks derives from the original constructor arg count
				int syntheticParameters = KotlinDefaultMask.getMaskCount(detectedConstructor.getParameterCount())
						+ /* DefaultConstructorMarker */ 1;

				if ((detectedConstructor.getParameterCount() + syntheticParameters) != candidate.getParameterCount()) {
					continue;
				}
			} else {

				int optionalParameterCount = (int) kotlinFunction.getParameters().stream().filter(it -> it.isOptional())
						.count();
				int syntheticParameters = KotlinDefaultMask.getExactMaskCount(optionalParameterCount);

				if ((detectedConstructor.getParameterCount() + syntheticParameters) != candidate.getParameterCount()) {
					continue;
				}
			}

			if (!KotlinInstantiationDelegate.hasDefaultConstructorMarker(candidateParameters)) {
				continue;
			}

			int userParameterCount = kotlinFunction != null ? kotlinFunction.getParameters().size()
					: detectedConstructor.getParameterCount();
			if (parametersMatch(detectedConstructorParameters, candidateParameters, userParameterCount)) {
				hit = candidate;
			}
		}

		return hit;
	}

	private static boolean parametersMatch(java.lang.reflect.Parameter[] constructorParameters,
			java.lang.reflect.Parameter[] candidateParameters, int userParameterCount) {

		return IntStream.range(0, userParameterCount)
				.allMatch(i -> parametersMatch(constructorParameters[i], candidateParameters[i]));
	}

	static boolean parametersMatch(java.lang.reflect.Parameter constructorParameter,
			java.lang.reflect.Parameter candidateParameter) {

		if (constructorParameter.getType().equals(candidateParameter.getType())) {
			return true;
		}

		// candidate can be also a wrapper

		Class<?> aClass = unwrapValueClass(candidateParameter.getType());

		return constructorParameter.getType().equals(aClass);
	}

	private static Class<?> unwrapValueClass(Class<?> type) {

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(type);
		if (kotlinClass != null && kotlinClass.isValue()) {

			KFunction<?> next = kotlinClass.getConstructors().iterator().next();
			KParameter kParameter = next.getParameters().get(0);
			Type javaType = ReflectJvmMapping.getJavaType(kParameter.getType());
			if (javaType instanceof Class<?>) {
				return unwrapValueClass((Class<?>) javaType);
			}
		}

		return type;
	}

}
