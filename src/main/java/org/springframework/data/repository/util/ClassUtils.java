/*
 * Copyright 2008-2024 the original author or authors.
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
package org.springframework.data.repository.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.springframework.data.repository.Repository;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class to work with classes.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @deprecated since 3.5, use {@link org.springframework.data.util.ClassUtils} instead.
 */
@Deprecated(since = "3.5", forRemoval = true)
public abstract class ClassUtils {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private ClassUtils() {}

	/**
	 * Returns whether the given class contains a property with the given name.
	 *
	 * @param type
	 * @param property
	 * @return
	 */
	public static boolean hasProperty(Class<?> type, String property) {

		if (null != ReflectionUtils.findMethod(type, "get" + property)) {
			return true;
		}

		return null != ReflectionUtils.findField(type, StringUtils.uncapitalize(property));
	}

	/**
	 * Determine whether the {@link Class} identified by the supplied {@code className} is present * and can be loaded and
	 * call the {@link Consumer action} if the {@link Class} could be loaded.
	 *
	 * @param className the name of the class to check.
	 * @param classLoader the class loader to use.
	 * @param action the action callback to notify. (may be {@code null} which indicates the default class loader)
	 * @throws IllegalStateException if the corresponding class is resolvable but there was a readability mismatch in the
	 *           inheritance hierarchy of the class (typically a missing dependency declaration in a Jigsaw module
	 *           definition for a superclass or interface implemented by the class to be checked here)
	 */
	public static void ifPresent(String className, @Nullable ClassLoader classLoader, Consumer<Class<?>> action) {
		org.springframework.data.util.ClassUtils.ifPresent(className, classLoader, action);
	}

	/**
	 * Returns wthere the given type is the {@link Repository} interface.
	 *
	 * @param interfaze
	 * @return
	 */
	public static boolean isGenericRepositoryInterface(Class<?> interfaze) {

		return Repository.class.equals(interfaze);
	}

	/**
	 * Returns whether the given type name is a repository interface name.
	 *
	 * @param interfaceName
	 * @return
	 */
	public static boolean isGenericRepositoryInterface(@Nullable String interfaceName) {
		return Repository.class.getName().equals(interfaceName);
	}

	/**
	 * @deprecated Use {@link #getNumberOfOccurrences(Method, Class)}.
	 */
	public static int getNumberOfOccurences(Method method, Class<?> type) {
		return getNumberOfOccurrences(method, type);
	}

	/**
	 * Returns the number of occurrences for the given {@link Method#getParameterTypes() parameter type} in the given
	 * {@link Method}.
	 *
	 * @param method {@link Method} to evaluate.
	 * @param parameterType {@link Class} of the {@link Method} parameter type to count.
	 * @return the number of occurrences for the given {@link Method#getParameterTypes() parameter type} in the given
	 *         {@link Method}.
	 * @see java.lang.reflect.Method#getParameterTypes()
	 */
	public static int getNumberOfOccurrences(@NonNull Method method, @NonNull Class<?> parameterType) {
		return org.springframework.data.util.ReflectionUtils.getParameterCount(method, parameterType::equals);
	}

	/**
	 * Asserts the given {@link Method}'s return type to be one of the given types. Will unwrap known wrapper types before
	 * the assignment check (see {@link QueryExecutionConverters}).
	 *
	 * @param method must not be {@literal null}.
	 * @param types must not be {@literal null} or empty.
	 */
	public static void assertReturnTypeAssignable(Method method, Class<?>... types) {

		Assert.notNull(method, "Method must not be null");
		Assert.notEmpty(types, "Types must not be null or empty");

		TypeInformation<?> returnType = getEffectivelyReturnedTypeFrom(method);

		Arrays.stream(types)//
				.filter(it -> it.isAssignableFrom(returnType.getType()))//
				.findAny().orElseThrow(() -> new IllegalStateException(
						"Method has to have one of the following return types: " + Arrays.toString(types)));
	}

	/**
	 * Returns whether the given object is of one of the given types. Will return {@literal false} for {@literal null}.
	 *
	 * @param object
	 * @param types
	 * @return
	 */
	public static boolean isOfType(@Nullable Object object, Collection<Class<?>> types) {

		if (object == null) {
			return false;
		}

		return types.stream().anyMatch(it -> it.isAssignableFrom(object.getClass()));
	}

	/**
	 * Returns whether the given {@link Method} has a parameter of the given type.
	 *
	 * @param method
	 * @param type
	 * @return
	 */
	public static boolean hasParameterOfType(Method method, Class<?> type) {
		return org.springframework.data.util.ReflectionUtils.hasParameterOfType(method, type);
	}

	/**
	 * Returns whether the given {@link Method} has a parameter that is assignable to the given type.
	 *
	 * @param method
	 * @param type
	 * @return
	 */
	public static boolean hasParameterAssignableToType(Method method, Class<?> type) {
		return org.springframework.data.util.ReflectionUtils.hasParameterOfType(method, type);
	}

	/**
	 * Helper method to extract the original exception that can possibly occur during a reflection call.
	 *
	 * @param ex
	 * @throws Throwable
	 */
	public static void unwrapReflectionException(Exception ex) throws Throwable {

		if (ex instanceof InvocationTargetException ite) {
			ReflectionUtils.handleInvocationTargetException(ite);
		}

		throw ex;
	}

	// TODO: we should also consider having the owning type here so we can resolve generics better.
	private static TypeInformation<?> getEffectivelyReturnedTypeFrom(Method method) {

		TypeInformation<?> returnType = TypeInformation.fromReturnTypeOf(method);

		return QueryExecutionConverters.supports(returnType.getType()) //
				|| ReactiveWrapperConverters.supports(returnType.getType()) //
						? returnType.getRequiredComponentType() //
						: returnType;
	}
}
