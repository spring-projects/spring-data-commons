/*
 * Copyright 2012-2015 the original author or authors.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Spring Data specific reflection utility methods and classes.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.5
 */
public abstract class ReflectionUtils {

	private static final Class<?> JAVA8_STREAM_TYPE;

	static {

		Class<?> cls = null;

		try {
			cls = Class.forName("java.util.stream.Stream");
		} catch (ClassNotFoundException ignore) {}

		JAVA8_STREAM_TYPE = cls;
	}

	private ReflectionUtils() {}

	/**
	 * Creates an instance of the class with the given fully qualified name or returns the given default instance if the
	 * class cannot be loaded or instantiated.
	 * 
	 * @param classname the fully qualified class name to create an instance for.
	 * @param defaultInstance the instance to fall back to in case the given class cannot be loaded or instantiated.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createInstanceIfPresent(String classname, T defaultInstance) {

		try {
			Class<?> type = ClassUtils.getDefaultClassLoader().loadClass(classname);
			return (T) BeanUtils.instantiateClass(type);
		} catch (Exception e) {
			return defaultInstance;
		}
	}

	/**
	 * Back-port of Java 8's {@code isDefault()} method on {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 * @return
	 */
	public static boolean isDefaultMethod(Method method) {

		return ((method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)
				&& method.getDeclaringClass().isInterface();
	}

	/**
	 * A {@link FieldFilter} that has a description.
	 * 
	 * @author Oliver Gierke
	 */
	public interface DescribedFieldFilter extends FieldFilter {

		/**
		 * Returns the description of the field filter. Used in exceptions being thrown in case uniqueness shall be enforced
		 * on the field filter.
		 * 
		 * @return
		 */
		String getDescription();
	}

	/**
	 * A {@link FieldFilter} for a given annotation.
	 * 
	 * @author Oliver Gierke
	 */
	public static class AnnotationFieldFilter implements DescribedFieldFilter {

		private final Class<? extends Annotation> annotationType;

		/**
		 * Creates a new {@link AnnotationFieldFilter} for the given annotation type.
		 */
		public AnnotationFieldFilter(Class<? extends Annotation> annotationType) {

			Assert.notNull(annotationType, "Annotation type must not be null!");
			this.annotationType = annotationType;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.util.ReflectionUtils.FieldFilter#matches(java.lang.reflect.Field)
		 */
		public boolean matches(Field field) {
			return AnnotationUtils.getAnnotation(field, annotationType) != null;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ReflectionUtils.DescribedFieldFilter#getDescription()
		 */
		public String getDescription() {
			return String.format("Annotation filter for %s", annotationType.getName());
		}
	}

	/**
	 * Finds the first field on the given class matching the given {@link FieldFilter}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return the field matching the filter or {@literal null} in case no field could be found.
	 */
	public static Field findField(Class<?> type, final FieldFilter filter) {

		return findField(type, new DescribedFieldFilter() {

			public boolean matches(Field field) {
				return filter.matches(field);
			}

			public String getDescription() {
				return String.format("FieldFilter %s", filter.toString());
			}
		}, false);
	}

	/**
	 * Finds the field matching the given {@link DescribedFieldFilter}. Will make sure there's only one field matching the
	 * filter.
	 * 
	 * @see #findField(Class, DescribedFieldFilter, boolean)
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @return the field matching the given {@link DescribedFieldFilter} or {@literal null} if none found.
	 * @throws IllegalStateException in case more than one matching field is found
	 */
	public static Field findField(Class<?> type, DescribedFieldFilter filter) {
		return findField(type, filter, true);
	}

	/**
	 * Finds the field matching the given {@link DescribedFieldFilter}. Will make sure there's only one field matching the
	 * filter in case {@code enforceUniqueness} is {@literal true}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param filter must not be {@literal null}.
	 * @param enforceUniqueness whether to enforce uniqueness of the field
	 * @return the field matching the given {@link DescribedFieldFilter} or {@literal null} if none found.
	 * @throws IllegalStateException if enforceUniqueness is true and more than one matching field is found
	 */
	public static Field findField(Class<?> type, DescribedFieldFilter filter, boolean enforceUniqueness) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(filter, "Filter must not be null!");

		Class<?> targetClass = type;
		Field foundField = null;

		while (targetClass != Object.class) {

			for (Field field : targetClass.getDeclaredFields()) {

				if (!filter.matches(field)) {
					continue;
				}

				if (!enforceUniqueness) {
					return field;
				}

				if (foundField != null && enforceUniqueness) {
					throw new IllegalStateException(filter.getDescription());
				}

				foundField = field;
			}

			targetClass = targetClass.getSuperclass();
		}

		return foundField;
	}

	/**
	 * Sets the given field on the given object to the given value. Will make sure the given field is accessible.
	 * 
	 * @param field must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param value
	 */
	public static void setField(Field field, Object target, Object value) {

		org.springframework.util.ReflectionUtils.makeAccessible(field);
		org.springframework.util.ReflectionUtils.setField(field, target, value);
	}

	/**
	 * Tests whether the given type is assignable to a Java 8 {@link Stream}.
	 * 
	 * @param type can be {@literal null}.
	 * @return
	 */
	public static boolean isJava8StreamType(Class<?> type) {

		if (type == null || JAVA8_STREAM_TYPE == null) {
			return false;
		}

		return JAVA8_STREAM_TYPE.isAssignableFrom(type);
	}

	/**
	 * Finds a constructoron the given type that matches the given constructor arguments.
	 * 
	 * @param type must not be {@literal null}.
	 * @param constructorArguments must not be {@literal null}.
	 * @return a {@link Constructor} that is compatible with the given arguments or {@literal null} if none found.
	 */
	public static Constructor<?> findConstructor(Class<?> type, Object... constructorArguments) {

		Assert.notNull(type, "Target type must not be null!");
		Assert.notNull(constructorArguments, "Constructor arguments must not be null!");

		for (Constructor<?> candidate : type.getDeclaredConstructors()) {

			Class<?>[] parameterTypes = candidate.getParameterTypes();

			if (argumentsMatch(parameterTypes, constructorArguments)) {
				return candidate;
			}
		}

		return null;
	}

	private static final boolean argumentsMatch(Class<?>[] parameterTypes, Object[] arguments) {

		if (parameterTypes.length != arguments.length) {
			return false;
		}

		int index = 0;

		for (Class<?> argumentType : parameterTypes) {

			Object argument = arguments[index];

			// Reject nulls for primitives
			if (argumentType.isPrimitive() && argument == null) {
				return false;
			}

			// Type check if argument is not null
			if (argument != null && !ClassUtils.isAssignableValue(argumentType, argument)) {
				return false;
			}

			index++;
		}

		return true;
	}
}
