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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.util.StringUtils;

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
	public MappingInstantiationException(PersistentEntity<?, ?> entity, List<Object> arguments, Exception cause) {
		this(entity, arguments, null, cause);
	}

	private MappingInstantiationException(PersistentEntity<?, ?> entity, List<Object> arguments, String message,
			Exception cause) {

		super(buildExceptionMessage(entity, arguments, message), cause);

		this.entityType = entity == null ? null : entity.getType();
		this.constructor = entity == null || entity.getPersistenceConstructor() == null ? null : entity
				.getPersistenceConstructor().getConstructor();
		this.constructorArguments = arguments;
	}

	private static final String buildExceptionMessage(PersistentEntity<?, ?> entity, List<Object> arguments,
			String defaultMessage) {

		if (entity == null) {
			return defaultMessage;
		}

		PreferredConstructor<?, ?> constructor = entity.getPersistenceConstructor();

		return String.format(TEXT_TEMPLATE, entity.getType().getName(), constructor == null ? "NO_CONSTRUCTOR"
				: constructor.getConstructor().toString(), StringUtils.collectionToCommaDelimitedString(arguments));
	}

	/**
	 * Returns the type of the entity that was attempted to instantiate.
	 * 
	 * @return the entityType
	 */
	public Class<?> getEntityType() {
		return entityType;
	}

	/**
	 * The constructor used during the instantiation attempt.
	 * 
	 * @return the constructor
	 */
	public Constructor<?> getConstructor() {
		return constructor;
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
