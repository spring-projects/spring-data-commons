/*
 * Copyright 2012-present the original author or authors.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
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
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 1.5
 */
public final class ReflectionUtils {

	private ReflectionUtils() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * Returns whether the given {@link Method} has a parameter of the given type.
	 *
	 * @param method the method to check, must not be {@literal null}.
	 * @param type parameter type to query for, must not be {@literal null}.
	 * @return {@literal true} the given {@link Method} has a parameter of the given type.
	 * @since 3.5
	 */
	public static boolean hasParameterOfType(Method method, Class<?> type) {
		return Arrays.asList(method.getParameterTypes()).contains(type);
	}

	/**
	 * Returns whether the given {@link Method} has a parameter that is assignable to the given type.
	 *
	 * @param method the method to check, must not be {@literal null}.
	 * @param type parameter type to query for, must not be {@literal null}.
	 * @return {@literal true} the given {@link Method} has a parameter that is assignable to the given type.
	 * @since 3.5
	 */
	public static boolean hasParameterAssignableToType(Method method, Class<?> type) {

		for (Class<?> parameterType : method.getParameterTypes()) {
			if (type.isAssignableFrom(parameterType)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the number of matching parameters {@link Method} for {@link Predicate}.
	 *
	 * @param method {@link Method} to evaluate.
	 * @param predicate the predicate matching {@link Method}
	 * @return the resulting number of matching parameters.
	 * @see java.lang.reflect.Method#getParameterTypes()
	 * @since 3.5
	 */
	public static int getParameterCount(Method method, Predicate<Class<?>> predicate) {
		return (int) Arrays.stream(method.getParameterTypes()).filter(predicate).count();
	}

	/**
	 * Check whether the given {@code type} represents a void type such as {@code void}, {@link Void} or Kotlin
	 * {@code Unit}.
	 *
	 * @param type must not be {@literal null}.
	 * @return whether the given the type is a void type.
	 * @since 2.4
	 */
	public static boolean isVoid(Class<?> type) {

		if (ClassUtils.isVoidType(type)) {
			return true;
		}

		if (type.getName().equals("kotlin.Unit")) {
			return true;
		}

		return false;
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

		public AnnotationFieldFilter(Class<? extends Annotation> annotationType) {
			this.annotationType = annotationType;
		}

		@Override
		public boolean matches(Field field) {
			return AnnotationUtils.getAnnotation(field, annotationType) != null;
		}

		@Override
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
	public @Nullable static Field findField(Class<?> type, FieldFilter filter) {

		return findField(type, new DescribedFieldFilter() {

			public boolean matches(Field field) {
				return filter.matches(field);
			}

			@Override
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
	public @Nullable static Field findField(Class<?> type, DescribedFieldFilter filter) {
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
	public @Nullable static Field findField(Class<?> type, DescribedFieldFilter filter, boolean enforceUniqueness) {

		Assert.notNull(type, "Type must not be null");
		Assert.notNull(filter, "Filter must not be null");

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

				if (foundField != null) {
					throw new IllegalStateException(filter.getDescription());
				}

				foundField = field;
			}

			targetClass = targetClass.getSuperclass();
		}

		return foundField;
	}

	/**
	 * Obtains the required field of the given name on the given type or throws {@link IllegalArgumentException} if the
	 * found could not be found.
	 *
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return the required field.
	 * @throws IllegalArgumentException in case the field can't be found.
	 */
	public static Field getRequiredField(Class<?> type, String name) {

		Field result = org.springframework.util.ReflectionUtils.findField(type, name);

		if (result == null) {
			throw new IllegalArgumentException(String.format("Unable to find field %s on %s", name, type));
		}

		return result;
	}

	/**
	 * Sets the given field on the given object to the given value. Will make sure the given field is accessible.
	 *
	 * @param field must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param value
	 */
	public static void setField(Field field, Object target, @Nullable Object value) {

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
	@SuppressWarnings("unchecked")
	public static <T> @Nullable Constructor<T> findConstructor(Class<T> type, Object... constructorArguments) {

		Assert.notNull(type, "Target type must not be null");
		Assert.notNull(constructorArguments, "Constructor arguments must not be null");

		for (Constructor<?> declaredConstructor : type.getDeclaredConstructors()) {
			if (argumentsMatch(declaredConstructor.getParameterTypes(), constructorArguments)) {
				return (Constructor<T>) declaredConstructor;
			}
		}

		return null;
	}

	/**
	 * Returns the method with the given name of the given class and parameter types. Prefers regular methods over
	 * {@link Method#isBridge() bridge} and {@link Method#isSynthetic() synthetic} ones.
	 *
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null}.
	 * @param parameterTypes must not be {@literal null}.
	 * @return the method object.
	 * @throws IllegalArgumentException in case the method cannot be resolved.
	 * @deprecated since 3.5, use {@link #getRequiredMethod(Class, String, Class[])} instead.
	 */
	@Deprecated
	public static Method findRequiredMethod(Class<?> type, String name, Class<?>... parameterTypes) {
		return getRequiredMethod(type, name, parameterTypes);
	}

	/**
	 * Returns the method with the given name of the given class and parameter types. Prefers regular methods over
	 * {@link Method#isBridge() bridge} and {@link Method#isSynthetic() synthetic} ones.
	 *
	 * @param type must not be {@literal null}.
	 * @param name must not be {@literal null}.
	 * @param parameterTypes must not be {@literal null}.
	 * @return the method object.
	 * @throws IllegalArgumentException in case the method cannot be resolved.
	 * @since 3.5
	 */
	public static Method getRequiredMethod(Class<?> type, String name, Class<?>... parameterTypes) {

		Assert.notNull(type, "Class must not be null");
		Assert.notNull(name, "Method name must not be null");

		Method result = null;
		Class<?> searchType = type;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods()
					: org.springframework.util.ReflectionUtils.getDeclaredMethods(searchType));
			for (Method method : methods) {
				if (name.equals(method.getName()) && hasSameParams(method, parameterTypes)) {
					if (result == null || result.isSynthetic() || result.isBridge()) {
						result = method;
					}
				}
			}
			searchType = searchType.getSuperclass();
		}

		if (result == null) {

			String parameterTypeNames = Arrays.stream(parameterTypes) //
					.map(Object::toString) //
					.collect(Collectors.joining(", "));

			throw new IllegalArgumentException(
					String.format("Unable to find method %s(%s) on %s", name, parameterTypeNames, type));
		}

		return result;
	}

	private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
		return (paramTypes.length == method.getParameterCount() && Arrays.equals(paramTypes, method.getParameterTypes()));
	}

	/**
	 * Returns a {@link Stream} of the return and parameters types of the given {@link Method}.
	 *
	 * @param method must not be {@literal null}.
	 * @return stream of return and parameter types.
	 * @since 2.0
	 */
	public static Stream<Class<?>> returnTypeAndParameters(Method method) {

		Assert.notNull(method, "Method must not be null");

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
	 * @return the required method.
	 * @since 3.5
	 */
	public static @Nullable Method findMethod(Class<?> type, String name, ResolvableType... parameterTypes) {

		Assert.notNull(type, "Type must not be null");
		Assert.hasText(name, "Name must not be null or empty");
		Assert.notNull(parameterTypes, "Parameter types must not be null");

		List<Class<?>> collect = parameterTypes.length == 0 ? Collections.emptyList()
				: new ArrayList<>(parameterTypes.length);
		for (ResolvableType parameterType : parameterTypes) {
			Class<?> rawClass = parameterType.getRawClass();
			collect.add(rawClass);
		}

		Method method = org.springframework.util.ReflectionUtils.findMethod(type, name, collect.toArray(new Class<?>[0]));

		if (method != null) {

			for (int i = 0; i < parameterTypes.length; i++) {
				if (!ResolvableType.forMethodParameter(method, i).equals(parameterTypes[i])) {
					return null;
				}
			}

			return method;
		}

		return null;
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

	/**
	 * Returns a string representation of the given method including its name and parameter using fully-qualified class
	 * names.
	 * <p>
	 * In contrast to {@link Method#toString()} this method omits the declaring type, the return type, any generics and
	 * modifiers.
	 *
	 * @param method the method to render to string.
	 * @return a string representation of the given method, i.e. {@code toString(java.lang.reflect.Method)}.
	 * @since 4.0
	 */
	public static String toString(Method method) {
		return toString(method, Type::getTypeName);
	}

	/**
	 * Returns a string representation of the given method including its name and parameter types.
	 * <p>
	 * In contrast to {@link Method#toString()} this method omits the declaring type, the return type, any generics and
	 * modifiers.
	 *
	 * @param method the method to render to string.
	 * @param typeNameMapper mapping function to obtain the type name from a {@link Class}.
	 * @return a string representation of the given method, i.e. {@code toString(java.lang.reflect.Method)} when using a
	 *         {@code Type::getTypeName typeNameMapper}.
	 * @since 4.0
	 */
	public static String toString(Method method, Function<Class<?>, String> typeNameMapper) {

		return method.getName() + Arrays.stream(method.getParameterTypes()) //
				.map(typeNameMapper) //
				.collect(Collectors.joining(",", "(", ")"));
	}

	/**
	 * Returns {@literal} whether the given {@link MethodParameter} is nullable. Nullable parameters are reference types
	 * and ones that are defined in Kotlin as such.
	 *
	 * @return {@literal true} if {@link MethodParameter} is nullable.
	 * @since 2.0
	 */
	public static boolean isNullable(MethodParameter parameter) {

		if (isVoid(parameter.getParameterType())) {
			return true;
		}

		if (KotlinDetector.isKotlinPresent()
				&& KotlinReflectionUtils.isSupportedKotlinClass(parameter.getDeclaringClass())) {
			return KotlinReflectionUtils.isNullable(parameter);
		}

		return !parameter.getParameterType().isPrimitive();
	}

	/**
	 * Get default value for a primitive type.
	 *
	 * @param type must not be {@literal null}.
	 * @return boxed primitive default value.
	 * @since 2.1
	 */
	public static Object getPrimitiveDefault(Class<?> type) {

		if (type == Byte.TYPE || type == Byte.class) {
			return (byte) 0;
		}

		if (type == Short.TYPE || type == Short.class) {
			return (short) 0;
		}

		if (type == Integer.TYPE || type == Integer.class) {
			return 0;
		}

		if (type == Long.TYPE || type == Long.class) {
			return 0L;
		}

		if (type == Float.TYPE || type == Float.class) {
			return 0F;
		}

		if (type == Double.TYPE || type == Double.class) {
			return 0D;
		}

		if (type == Character.TYPE || type == Character.class) {
			return '\u0000';
		}

		if (type == Boolean.TYPE) {
			return Boolean.FALSE;
		}

		throw new IllegalArgumentException(String.format("Primitive type %s not supported", type));
	}

}
