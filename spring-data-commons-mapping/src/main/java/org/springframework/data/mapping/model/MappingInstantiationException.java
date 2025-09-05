/*
 * Copyright 2011-2025 the original author or authors.
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

import kotlin.reflect.KFunction;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.KotlinDetector;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Exception being thrown in case an entity could not be instantiated in the process of a to-object-mapping.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class MappingInstantiationException extends RuntimeException {

	private static final @Serial long serialVersionUID = 822211065035487628L;
	private static final String TEXT_TEMPLATE = "Failed to instantiate %s using constructor %s with arguments %s";

	private final @Nullable Class<?> entityType;
	private final @Nullable InstanceCreatorMetadata<?> entityCreator;
	private final List<Object> constructorArguments;

	/**
	 * Creates a new {@link MappingInstantiationException} for the given {@link PersistentEntity}, constructor arguments
	 * and the causing exception.
	 *
	 * @param entity
	 * @param arguments
	 * @param cause
	 */
	public MappingInstantiationException(@Nullable PersistentEntity<?, ?> entity, List<Object> arguments,
			Exception cause) {
		this(entity, arguments, null, cause);
	}

	/**
	 * Creates a new {@link MappingInstantiationException} for the given constructor arguments and the causing exception.
	 *
	 * @param arguments
	 * @param cause
	 */
	public MappingInstantiationException(List<Object> arguments, Exception cause) {
		this(null, arguments, null, cause);
	}

	private MappingInstantiationException(@Nullable PersistentEntity<?, ?> entity, List<Object> arguments,
			@Nullable String message, Exception cause) {

		super(buildExceptionMessage(entity, arguments, message), cause);

		this.entityType = entity != null ? entity.getType() : null;
		this.entityCreator = entity != null ? entity.getInstanceCreatorMetadata() : null;
		this.constructorArguments = arguments;
	}

	private static @Nullable String buildExceptionMessage(@Nullable PersistentEntity<?, ?> entity, List<Object> arguments,
			@Nullable String defaultMessage) {

		if (entity == null) {
			return defaultMessage;
		}

		InstanceCreatorMetadata<?> constructor = entity.getInstanceCreatorMetadata();
		List<String> toStringArgs = new ArrayList<>(arguments.size());

		for (Object o : arguments) {
			toStringArgs.add(ObjectUtils.nullSafeToString(o));
		}

		return String.format(TEXT_TEMPLATE, entity.getType().getName(),
				constructor != null ? toString(constructor) : "NO_CONSTRUCTOR", //
				String.join(",", toStringArgs));

	}

	private static String toString(InstanceCreatorMetadata<?> creator) {

		if (creator instanceof PreferredConstructor<?, ?> c) {
			return toString(c);
		}

		if (creator instanceof FactoryMethod<?, ?> m) {
			return toString(m);
		}

		return creator.toString();
	}

	private static String toString(PreferredConstructor<?, ?> preferredConstructor) {

		Constructor<?> constructor = preferredConstructor.getConstructor();

		if (KotlinDetector.isKotlinPresent()
				&& KotlinReflectionUtils.isSupportedKotlinClass(constructor.getDeclaringClass())) {

			KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(constructor);

			if (kotlinFunction != null) {
				return kotlinFunction.toString();
			}
		}

		return constructor.toString();
	}

	private static String toString(FactoryMethod<?, ?> factoryMethod) {

		Method constructor = factoryMethod.getFactoryMethod();

		if (KotlinDetector.isKotlinPresent()
				&& KotlinReflectionUtils.isSupportedKotlinClass(constructor.getDeclaringClass())) {

			KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(constructor);

			if (kotlinFunction != null) {
				return kotlinFunction.toString();
			}
		}

		return constructor.toString();
	}

	/**
	 * Returns the type of the entity that was attempted to instantiate.
	 *
	 * @return the entityType
	 */
	public @Nullable Class<?> getEntityType() {
		return entityType;
	}

	/**
	 * The entity creator used during the instantiation attempt.
	 *
	 * @return the entity creator
	 * @since 3.0
	 */
	public @Nullable InstanceCreatorMetadata<?> getEntityCreator() {
		return entityCreator;
	}

	/**
	 * The constructor arguments used to invoke the constructor.
	 *
	 * @return the constructorArguments
	 */
	public List<Object> getConstructorArguments() {
		return constructorArguments;
	}
}
