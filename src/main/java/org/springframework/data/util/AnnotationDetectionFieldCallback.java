/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
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
	private Optional<Field> field = Optional.empty();

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

		if (this.field.isPresent()) {
			return;
		}

		if (AnnotatedElementUtils.findMergedAnnotation(field, annotationType) != null) {

			ReflectionUtils.makeAccessible(field);
			this.field = Optional.of(field);
		}
	}

	/**
	 * Returns the type of the field.
	 * 
	 * @return
	 */
	public Optional<Class<?>> getType() {
		return field.map(Field::getType);
	}

	/**
	 * Returns the type of the field or throws an {@link IllegalArgumentException} if no field could be found.
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	public Class<?> getRequiredType() {

		return getType().orElseThrow(() -> new IllegalStateException(
				String.format("Unable to obtain type! Didn't find field with annotation %s!", annotationType)));
	}

	/**
	 * Retrieves the value of the field by reflection.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getValue(Object source) {

		Assert.notNull(source, "Source object must not be null!");

		return field.map(it -> (T) ReflectionUtils.getField(it, source));
	}
}
