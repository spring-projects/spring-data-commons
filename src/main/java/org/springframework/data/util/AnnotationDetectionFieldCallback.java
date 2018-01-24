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
package org.springframework.data.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * A {@link FieldCallback} that will inspect each field for a given annotation. This field's type can then be accessed
 * afterwards.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class AnnotationDetectionFieldCallback implements FieldCallback {

	private final Class<? extends Annotation> annotationType;
	private @Nullable Field field;

	/**
	 * Creates a new {@link AnnotationDetectionFieldCallback} scanning for an annotation of the given type.
	 *
	 * @param annotationType must not be {@literal null}.
	 */
	public AnnotationDetectionFieldCallback(Class<? extends Annotation> annotationType) {

		Assert.notNull(annotationType, "AnnotationType must not be null!");

		this.annotationType = annotationType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.util.ReflectionUtils.FieldCallback#doWith(java.lang.reflect.Field)
	 */
	public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

		if (this.field != null) {
			return;
		}

		if (AnnotatedElementUtils.findMergedAnnotation(field, annotationType) != null) {

			ReflectionUtils.makeAccessible(field);
			this.field = field;
		}
	}

	/**
	 * Returns the detected field.
	 *
	 * @return the field
	 */
	@Nullable
	public Field getField() {
		return field;
	}

	/**
	 * Returns the field that was detected.
	 *
	 * @return
	 * @throws IllegalStateException in case no field with the configured annotation was found.
	 */
	public Field getRequiredField() {

		Field field = this.field;

		if (field == null) {
			throw new IllegalStateException(String.format("No field found for annotation %s!", annotationType));
		}

		return field;
	}

	/**
	 * Returns the type of the field.
	 *
	 * @return
	 */
	@Nullable
	public Class<?> getType() {

		Field field = this.field;

		return field == null ? null : field.getType();
	}

	/**
	 * Returns the type of the field or throws an {@link IllegalArgumentException} if no field could be found.
	 *
	 * @return
	 * @throws IllegalStateException in case no field with the configured annotation was found.
	 */
	public Class<?> getRequiredType() {
		return getRequiredField().getType();
	}

	/**
	 * Retrieves the value of the field by reflection.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getValue(Object source) {

		Assert.notNull(source, "Source object must not be null!");

		Field field = this.field;

		if (field == null) {
			return null;
		}

		return (T) ReflectionUtils.getField(field, source);
	}
}
