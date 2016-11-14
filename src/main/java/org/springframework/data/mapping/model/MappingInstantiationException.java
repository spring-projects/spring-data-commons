/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PreferredConstructor;

/**
 * Exception being thrown in case an entity could not be instantiated in the process of a to-object-mapping.
 * 
 * @author Oliver Gierke
 * @author Jon Brisbin
 */
public class MappingInstantiationException extends RuntimeException {

	private static final long serialVersionUID = 822211065035487628L;
	private static final String TEXT_TEMPLATE = "Failed to instantiate %s using constructor %s with arguments %s";

	private final Class<?> entityType;
	private final Constructor<?> constructor;
	private final List<Object> constructorArguments;

	/**
	 * Creates a new {@link MappingInstantiationException} for the given {@link PersistentEntity}, constructor arguments
	 * and the causing exception.
	 * 
	 * @param entity
	 * @param arguments
	 * @param cause
	 */
	public MappingInstantiationException(Optional<PersistentEntity<?, ?>> entity, List<Object> arguments,
			Exception cause) {
		this(entity, arguments, null, cause);
	}

	private MappingInstantiationException(Optional<PersistentEntity<?, ?>> entity, List<Object> arguments, String message,
			Exception cause) {

		super(buildExceptionMessage(entity, arguments.stream(), message), cause);

		this.entityType = entity.map(it -> it.getType()).orElse(null);
		this.constructor = entity.flatMap(it -> it.getPersistenceConstructor()).map(c -> c.getConstructor()).orElse(null);
		this.constructorArguments = arguments;
	}

	private static final String buildExceptionMessage(Optional<PersistentEntity<?, ?>> entity, Stream<Object> arguments,
			String defaultMessage) {

		return entity.map(it -> {

			Optional<? extends PreferredConstructor<?, ?>> constructor = it.getPersistenceConstructor();

			return String.format(TEXT_TEMPLATE, it.getType().getName(),
					constructor.map(c -> c.getConstructor().toString()).orElse("NO_CONSTRUCTOR"),
					String.join(",", arguments.map(Object::toString).collect(Collectors.toList())));

		}).orElse(defaultMessage);
	}

	/**
	 * Returns the type of the entity that was attempted to instantiate.
	 * 
	 * @return the entityType
	 */
	public Optional<Class<?>> getEntityType() {
		return Optional.ofNullable(entityType);
	}

	/**
	 * The constructor used during the instantiation attempt.
	 * 
	 * @return the constructor
	 */
	public Optional<Constructor<?>> getConstructor() {
		return Optional.ofNullable(constructor);
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
