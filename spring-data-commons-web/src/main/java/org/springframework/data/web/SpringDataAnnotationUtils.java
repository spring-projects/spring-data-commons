/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.Pageable;

/**
 * Helper class to ease sharing code between legacy {@link PageableHandlerMethodArgumentResolverSupport} and
 * {@link SortHandlerMethodArgumentResolverSupport}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
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
			throw new IllegalArgumentException(String.format("Method parameter %s is not backed by a method", parameter));
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
	 * Determine a qualifier value for a {@link MethodParameter}.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return the qualifier value if {@code @Qualifier} is present.
	 * @since 2.5
	 */
	public static @Nullable String getQualifier(@Nullable MethodParameter parameter) {

		if (parameter == null) {
			return null;
		}

		MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameter());
		MergedAnnotation<Qualifier> qualifier = annotations.get(Qualifier.class);

		return qualifier.isPresent() ? qualifier.getString("value") : null;
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
							"Ambiguous Pageable arguments in handler method; If you use multiple parameters of type Pageable you need to qualify them with @Qualifier");
				}

				if (values.contains(qualifier.value())) {
					throw new IllegalStateException("Values of the user Qualifiers must be unique");
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
	private static @Nullable Qualifier findAnnotation(Annotation[] annotations) {

		for (Annotation annotation : annotations) {
			if (annotation instanceof Qualifier q) {
				return q;
			}
		}

		return null;
	}

}
