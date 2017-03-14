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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Spring Data specific reflection utility methods and classes.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @since 1.5
 */
@UtilityClass
public class ReflectionUtils {

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
	@RequiredArgsConstructor
	public static class AnnotationFieldFilter implements DescribedFieldFilter {

		private final @NonNull Class<? extends Annotation> annotationType;

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
	 * Finds a constructor on the given type that matches the given constructor arguments.
	 * 
	 * @param type must not be {@literal null}.
	 * @param constructorArguments must not be {@literal null}.
	 * @return a {@link Constructor} that is compatible with the given arguments.
	 */
	public static Optional<Constructor<?>> findConstructor(Class<?> type, Object... constructorArguments) {

		Assert.notNull(type, "Target type must not be null!");
		Assert.notNull(constructorArguments, "Constructor arguments must not be null!");

		return Arrays.stream(type.getDeclaredConstructors())//
				.filter(constructor -> argumentsMatch(constructor.getParameterTypes(), constructorArguments))//
				.findFirst();
	}

	/**
	 * Returns a {@link Stream} of the return and parameters types of the given {@link Method}.
	 * 
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static Stream<Class<?>> returnTypeAndParameters(Method method) {

		Assert.notNull(method, "Method must not be null!");

		Stream<Class<?>> returnType = Stream.of(method.getReturnType());
		Stream<Class<?>> parameterTypes = Arrays.stream(method.getParameterTypes());

		return Stream.concat(returnType, parameterTypes);
	}

	/**
	 * Returns the {@link Method} with the given name and parameters declared on the given type, if available.
	 * 
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @param parameterTypes must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static Optional<Method> getMethod(Class<?> type, String name, ResolvableType... parameterTypes) {

		Assert.notNull(type, "Type must not be null!");
		Assert.hasText(name, "Name must not be null or empty!");
		Assert.notNull(parameterTypes, "Parameter types must not be null!");

		List<Class<?>> collect = Arrays.stream(parameterTypes)//
				.map(ResolvableType::getRawClass)//
				.collect(Collectors.toList());

		Method method = org.springframework.util.ReflectionUtils.findMethod(type, name,
				collect.toArray(new Class<?>[collect.size()]));

		return Optional.ofNullable(method)//
				.filter(it -> IntStream.range(0, it.getParameterCount())//
						.allMatch(index -> ResolvableType.forMethodParameter(it, index).equals(parameterTypes[index])));
	}

	private static boolean argumentsMatch(Class<?>[] parameterTypes, Object[] arguments) {

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
