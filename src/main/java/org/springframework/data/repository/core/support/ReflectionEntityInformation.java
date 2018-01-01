/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EntityInformation} implementation that inspects fields for an annotation and looks up this field's value to
 * retrieve the id.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class ReflectionEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {

	private static final Class<Id> DEFAULT_ID_ANNOTATION = Id.class;

	private Field field;

	/**
	 * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the
	 * {@link Id} annotation.
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public ReflectionEntityInformation(Class<T> domainClass) {
		this(domainClass, DEFAULT_ID_ANNOTATION);
	}

	/**
	 * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the given
	 * annotation.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	public ReflectionEntityInformation(Class<T> domainClass, final Class<? extends Annotation> annotation) {

		super(domainClass);

		Assert.notNull(annotation, "Annotation must not be null!");

		AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(annotation);
		ReflectionUtils.doWithFields(domainClass, callback);

		try {
			this.field = callback.getRequiredField();
		} catch (IllegalStateException o_O) {
			throw new IllegalArgumentException(String.format("Couldn't find field with annotation %s!", annotation), o_O);
		}

		ReflectionUtils.makeAccessible(this.field);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public ID getId(Object entity) {

		Assert.notNull(entity, "Entity must not be null!");

		return (ID) ReflectionUtils.getField(field, entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) field.getType();
	}
}
