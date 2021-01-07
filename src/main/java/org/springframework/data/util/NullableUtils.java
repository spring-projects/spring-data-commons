/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.NonNullApi;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * Utility methods to introspect nullability rules declared in packages, classes and methods.
 * <p/>
 * Nullability rules are declared using {@link NonNullApi} and {@link Nullable} and JSR-305
 * {@link javax.annotation.Nonnull} annotations. By default (no annotation use), a package and its types are considered
 * allowing {@literal null} values in return values and method parameters. Nullability rules are expressed by annotating
 * a package with a JSR-305 meta annotation such as Spring's {@link NonNullApi}. All types of the package inherit the
 * package rule. Subpackages do not inherit nullability rules and must be annotated themself.
 *
 * <pre class="code">
 * &#64;org.springframework.lang.NonNullApi
 * package com.example;
 * </pre>
 *
 * {@link Nullable} selectively permits {@literal null} values for method return values or method parameters by
 * annotating the method respectively the parameters:
 *
 * <pre class="code">
 * public class ExampleClass {
 *
 * 	String shouldNotReturnNull(@Nullable String acceptsNull, String doesNotAcceptNull) {
 * 		// …
 * 	}
 *
 * 	&#64;Nullable
 * 	String nullableReturn(String parameter) {
 * 		// …
 * 	}
 * }
 * </pre>
 * <p/>
 * {@link javax.annotation.Nonnull} is suitable for composition of meta-annotations and expresses via
 * {@link Nonnull#when()} in which cases non-nullability is applicable.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see NonNullApi
 * @see Nullable
 * @see Nonnull
 */
public abstract class NullableUtils {

	private static final String NON_NULL_CLASS_NAME = "javax.annotation.Nonnull";
	private static final String TYPE_QUALIFIER_CLASS_NAME = "javax.annotation.meta.TypeQualifierDefault";

	private static final Optional<Class<Annotation>> NON_NULL_ANNOTATION_CLASS = findClass(NON_NULL_CLASS_NAME);

	private static final Set<Class<?>> NULLABLE_ANNOTATIONS = findClasses(Nullable.class.getName());
	private static final Set<Class<?>> NON_NULLABLE_ANNOTATIONS = findClasses("reactor.util.lang.NonNullApi",
			NonNullApi.class.getName());

	private static final Set<String> WHEN_NULLABLE = new HashSet<>(Arrays.asList("UNKNOWN", "MAYBE", "NEVER"));
	private static final Set<String> WHEN_NON_NULLABLE = new HashSet<>(Collections.singletonList("ALWAYS"));

	private NullableUtils() {}

	/**
	 * Determine whether {@link ElementType} in the scope of {@link Method} requires non-{@literal null} values.
	 * Non-nullability rules are discovered from class and package annotations. Non-null is applied when
	 * {@link javax.annotation.Nonnull} is set to {@link javax.annotation.meta.When#ALWAYS}.
	 *
	 * @param type the class to inspect.
	 * @param elementType the element type.
	 * @return {@literal true} if {@link ElementType} allows {@literal null} values by default.
	 * @see #isNonNull(Annotation, ElementType)
	 */
	public static boolean isNonNull(Method method, ElementType elementType) {
		return isNonNull(method.getDeclaringClass(), elementType) || isNonNull((AnnotatedElement) method, elementType);
	}

	/**
	 * Determine whether {@link ElementType} in the scope of {@code type} requires non-{@literal null} values.
	 * Non-nullability rules are discovered from class and package annotations. Non-null is applied when
	 * {@link javax.annotation.Nonnull} is set to {@link javax.annotation.meta.When#ALWAYS}.
	 *
	 * @param type the class to inspect.
	 * @param elementType the element type.
	 * @return {@literal true} if {@link ElementType} allows {@literal null} values by default.
	 * @see #isNonNull(Annotation, ElementType)
	 */
	public static boolean isNonNull(Class<?> type, ElementType elementType) {
		return isNonNull(type.getPackage(), elementType) || isNonNull((AnnotatedElement) type, elementType);
	}

	/**
	 * Determine whether {@link ElementType} in the scope of {@link AnnotatedElement} requires non-{@literal null} values.
	 * This method determines default {@link javax.annotation.Nonnull nullability} rules from the annotated element
	 *
	 * @param element the scope of declaration, may be a {@link Package}, {@link Class}, or
	 *          {@link java.lang.reflect.Method}.
	 * @param elementType the element type.
	 * @return {@literal true} if {@link ElementType} allows {@literal null} values by default.
	 */
	public static boolean isNonNull(AnnotatedElement element, ElementType elementType) {

		for (Annotation annotation : element.getAnnotations()) {

			boolean isNonNull = NON_NULL_ANNOTATION_CLASS.isPresent() ? isNonNull(annotation, elementType)
					: NON_NULLABLE_ANNOTATIONS.contains(annotation.annotationType());

			if (isNonNull) {
				return true;
			}
		}

		return false;
	}

	private static boolean isNonNull(Annotation annotation, ElementType elementType) {

		if (!NON_NULL_ANNOTATION_CLASS.isPresent()) {
			return false;
		}

		Class<Annotation> annotationClass = NON_NULL_ANNOTATION_CLASS.get();

		if (annotation.annotationType().equals(annotationClass)) {
			return true;
		}

		if (!MergedAnnotations.from(annotation.annotationType()).isPresent(annotationClass)
				|| !isNonNull(annotation)) {
			return false;
		}

		return test(annotation, TYPE_QUALIFIER_CLASS_NAME, "value",
				(ElementType[] o) -> Arrays.binarySearch(o, elementType) >= 0);
	}

	/**
	 * Determine whether a {@link MethodParameter} is explicitly annotated to be considered nullable. Nullability rules
	 * are discovered from method and parameter annotations. A {@link MethodParameter} is considered nullable when
	 * {@link javax.annotation.Nonnull} is set to one of {@link javax.annotation.meta.When#UNKNOWN},
	 * {@link javax.annotation.meta.When#NEVER}, or {@link javax.annotation.meta.When#MAYBE}.
	 *
	 * @param methodParameter the method parameter to inspect.
	 * @return {@literal true} if the parameter is nullable, {@literal false} otherwise.
	 */
	public static boolean isExplicitNullable(MethodParameter methodParameter) {

		if (methodParameter.getParameterIndex() == -1) {
			return isExplicitNullable(methodParameter.getMethodAnnotations());
		}

		return isExplicitNullable(methodParameter.getParameterAnnotations());
	}

	private static boolean isExplicitNullable(Annotation[] annotations) {

		for (Annotation annotation : annotations) {

			boolean isNullable = NON_NULL_ANNOTATION_CLASS.isPresent() ? isNullable(annotation)
					: NULLABLE_ANNOTATIONS.contains(annotation.annotationType());

			if (isNullable) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Introspect {@link Annotation} for being either a meta-annotation composed from {@link Nonnull} or {@link Nonnull}
	 * itself expressing non-nullability.
	 *
	 * @param annotation
	 * @return {@literal true} if the annotation expresses non-nullability.
	 */
	private static boolean isNonNull(Annotation annotation) {
		return test(annotation, NON_NULL_CLASS_NAME, "when", o -> WHEN_NON_NULLABLE.contains(o.toString()));
	}

	/**
	 * Introspect {@link Annotation} for being either a meta-annotation composed from {@link Nonnull} or {@link Nonnull}
	 * itself expressing nullability.
	 *
	 * @param annotation
	 * @return {@literal true} if the annotation expresses nullability.
	 */
	private static boolean isNullable(Annotation annotation) {
		return test(annotation, NON_NULL_CLASS_NAME, "when", o -> WHEN_NULLABLE.contains(o.toString()));
	}

	@SuppressWarnings("unchecked")
	private static <T> boolean test(Annotation annotation, String metaAnnotationName, String attribute,
			Predicate<T> filter) {

		if (annotation.annotationType().getName().equals(metaAnnotationName)) {

			Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);

			return !attributes.isEmpty() && filter.test((T) attributes.get(attribute));
		}

		MultiValueMap<String, Object> attributes = AnnotatedElementUtils
				.getAllAnnotationAttributes(annotation.annotationType(), metaAnnotationName);

		if (attributes == null || attributes.isEmpty()) {
			return false;
		}

		List<Object> elementTypes = attributes.get(attribute);

		for (Object value : elementTypes) {

			if (filter.test((T) value)) {
				return true;
			}
		}
		return false;
	}

	private static Set<Class<?>> findClasses(String... classNames) {

		return Arrays.stream(classNames) //
				.map(NullableUtils::findClass) //
				.filter(Optional::isPresent) //
				.map(Optional::get) //
				.collect(Collectors.toSet());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> Optional<Class<T>> findClass(String className) {

		try {
			return Optional.of((Class) ClassUtils.forName(className, NullableUtils.class.getClassLoader()));
		} catch (ClassNotFoundException e) {
			return Optional.empty();
		}
	}
}
