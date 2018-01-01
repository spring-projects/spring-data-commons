/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Helper class to ease sharing code between legacy {@link PageableArgumentResolver} and
 * {@link PageableHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 */
abstract class SpringDataAnnotationUtils {

	private SpringDataAnnotationUtils() {}

	/**
	 * Asserts uniqueness of all {@link Pageable} parameters of the method of the given {@link MethodParameter}.
	 *
	 * @param parameter must not be {@literal null}.
	 */
	public static void assertPageableUniqueness(MethodParameter parameter) {

		Method method = parameter.getMethod();

		if (method == null) {
			throw new IllegalArgumentException(String.format("Method parameter %s is not backed by a method.", parameter));
		}

		if (containsMoreThanOnePageableParameter(method)) {
			Annotation[][] annotations = method.getParameterAnnotations();
			assertQualifiersFor(method.getParameterTypes(), annotations);
		}
	}

	/**
	 * Returns whether the given {@link Method} has more than one {@link Pageable} parameter.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private static boolean containsMoreThanOnePageableParameter(Method method) {

		boolean pageableFound = false;

		for (Class<?> type : method.getParameterTypes()) {

			if (pageableFound && type.equals(Pageable.class)) {
				return true;
			}

			if (type.equals(Pageable.class)) {
				pageableFound = true;
			}
		}

		return false;
	}

	/**
	 * Returns the value of the given specific property of the given annotation. If the value of that property is the
	 * properties default, we fall back to the value of the {@code value} attribute.
	 *
	 * @param annotation must not be {@literal null}.
	 * @param property must not be {@literal null} or empty.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getSpecificPropertyOrDefaultFromValue(Annotation annotation, String property) {

		Object propertyDefaultValue = AnnotationUtils.getDefaultValue(annotation, property);
		Object propertyValue = AnnotationUtils.getValue(annotation, property);

		Object result = ObjectUtils.nullSafeEquals(propertyDefaultValue, propertyValue) //
				? AnnotationUtils.getValue(annotation) //
				: propertyValue;

		if (result == null) {
			throw new IllegalStateException("Exepected to be able to look up an annotation property value but failed!");
		}

		return (T) result;
	}

	/**
	 * Asserts that every {@link Pageable} parameter of the given parameters carries an {@link Qualifier} annotation to
	 * distinguish them from each other.
	 *
	 * @param parameterTypes must not be {@literal null}.
	 * @param annotations must not be {@literal null}.
	 */
	public static void assertQualifiersFor(Class<?>[] parameterTypes, Annotation[][] annotations) {

		Set<String> values = new HashSet<>();

		for (int i = 0; i < annotations.length; i++) {

			if (Pageable.class.equals(parameterTypes[i])) {

				Qualifier qualifier = findAnnotation(annotations[i]);

				if (null == qualifier) {
					throw new IllegalStateException(
							"Ambiguous Pageable arguments in handler method. If you use multiple parameters of type Pageable you need to qualify them with @Qualifier");
				}

				if (values.contains(qualifier.value())) {
					throw new IllegalStateException("Values of the user Qualifiers must be unique!");
				}

				values.add(qualifier.value());
			}
		}
	}

	/**
	 * Returns a {@link Qualifier} annotation from the given array of {@link Annotation}s. Returns {@literal null} if the
	 * array does not contain a {@link Qualifier} annotation.
	 *
	 * @param annotations must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private static Qualifier findAnnotation(Annotation[] annotations) {

		for (Annotation annotation : annotations) {
			if (annotation instanceof Qualifier) {
				return (Qualifier) annotation;
			}
		}

		return null;
	}
}
