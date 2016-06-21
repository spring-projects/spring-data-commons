/*
 * Copyright 2008-2015 the original author or authors.
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
package org.springframework.data.repository.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.data.repository.Repository;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class to work with classes.
 * 
 * @author Oliver Gierke
 */
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
	public static boolean isGenericRepositoryInterface(String interfaceName) {

		return Repository.class.getName().equals(interfaceName);
	}

	/**
	 * Returns the number of occurences of the given type in the given {@link Method}s parameters.
	 * 
	 * @param method
	 * @param type
	 * @return
	 */
	public static int getNumberOfOccurences(Method method, Class<?> type) {

		int result = 0;
		for (Class<?> clazz : method.getParameterTypes()) {
			if (type.equals(clazz)) {
				result++;
			}
		}

		return result;
	}

	/**
	 * Asserts the given {@link Method}'s return type to be one of the given types. Will unwrap known wrapper types before
	 * the assignment check (see {@link QueryExecutionConverters}).
	 * 
	 * @param method must not be {@literal null}.
	 * @param types must not be {@literal null} or empty.
	 */
	public static void assertReturnTypeAssignable(Method method, Class<?>... types) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notEmpty(types, "Types must not be null or empty!");

		TypeInformation<?> returnType = getEffectivelyReturnedTypeFrom(method);

		Arrays.stream(types)//
				.filter(it -> it.isAssignableFrom(returnType.getType()))//
				.findAny().orElseThrow(() -> new IllegalStateException(
						"Method has to have one of the following return types! " + Arrays.toString(types)));
	}

	/**
	 * Returns whether the given object is of one of the given types. Will return {@literal false} for {@literal null}.
	 * 
	 * @param object
	 * @param types
	 * @return
	 */
	public static boolean isOfType(Object object, Collection<Class<?>> types) {

		if (null == object) {
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
		return Arrays.asList(method.getParameterTypes()).contains(type);
	}

	/**
	 * Helper method to extract the original exception that can possibly occur during a reflection call.
	 * 
	 * @param ex
	 * @throws Throwable
	 */
	public static void unwrapReflectionException(Exception ex) throws Throwable {

		if (ex instanceof InvocationTargetException) {
			throw ((InvocationTargetException) ex).getTargetException();
		}

		throw ex;
	}

	private static TypeInformation<?> getEffectivelyReturnedTypeFrom(Method method) {

		TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);
		return QueryExecutionConverters.supports(returnType.getType()) ? returnType.getRequiredComponentType() : returnType;
	}
}
