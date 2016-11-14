/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.repository.support;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Simply helper to reference a dedicated attribute of an {@link Annotation}.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
@RequiredArgsConstructor
class AnnotationAttribute {

	private final @NonNull Class<? extends Annotation> annotationType;
	private final @NonNull Optional<String> attributeName;

	/**
	 * Creates a new {@link AnnotationAttribute} to the {@code value} attribute of the given {@link Annotation} type.
	 * 
	 * @param annotationType must not be {@literal null}.
	 */
	public AnnotationAttribute(Class<? extends Annotation> annotationType) {
		this(annotationType, Optional.empty());
	}

	/**
	 * Returns the annotation type.
	 * 
	 * @return the annotationType
	 */
	public Class<? extends Annotation> getAnnotationType() {
		return annotationType;
	}

	/**
	 * Reads the {@link Annotation} attribute's value from the given {@link MethodParameter}.
	 * 
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	public Optional<Object> getValueFrom(MethodParameter parameter) {

		Assert.notNull(parameter, "MethodParameter must not be null!");
		Annotation annotation = parameter.getParameterAnnotation(annotationType);

		return Optional.ofNullable(annotation).map(it -> getValueFrom(it));
	}

	/**
	 * Reads the {@link Annotation} attribute's value from the given {@link AnnotatedElement}.
	 * 
	 * @param annotatedElement must not be {@literal null}.
	 * @return
	 */
	public Optional<Object> getValueFrom(AnnotatedElement annotatedElement) {

		Assert.notNull(annotatedElement, "Annotated element must not be null!");
		Annotation annotation = annotatedElement.getAnnotation(annotationType);

		return Optional.ofNullable(annotation).map(it -> getValueFrom(annotation));
	}

	/**
	 * Returns the {@link Annotation} attribute's value from the given {@link Annotation}.
	 * 
	 * @param annotation must not be {@literal null}.
	 * @return
	 */
	public Object getValueFrom(Annotation annotation) {

		Assert.notNull(annotation, "Annotation must not be null!");
		return (String) attributeName.map(it -> AnnotationUtils.getValue(annotation, it))
				.orElseGet(() -> AnnotationUtils.getValue(annotation));
	}
}
