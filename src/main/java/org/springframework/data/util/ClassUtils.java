/*
 * Copyright 2008-present the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Utility class to work with classes.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 3.5
 */
public abstract class ClassUtils {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private ClassUtils() {}

	/**
	 * Determine whether the {@link Class} identified by the supplied {@code className} is present and can be loaded and
	 * call the {@link Consumer action} if the {@link Class} could be loaded.
	 *
	 * @param className the name of the class to check.
	 * @param classLoader the class loader to use (can be {@literal null}, which indicates the default class loader).
	 * @param action the action callback to notify.
	 * @throws IllegalStateException if the corresponding class is resolvable but there was a readability mismatch in the
	 *           inheritance hierarchy of the class (typically a missing dependency declaration in a Jigsaw module
	 *           definition for a superclass or interface implemented by the class to be checked here)
	 */
	public static void ifPresent(String className, @Nullable ClassLoader classLoader, Consumer<Class<?>> action) {

		try {
			Class<?> theClass = org.springframework.util.ClassUtils.forName(className, classLoader);
			action.accept(theClass);
		} catch (IllegalAccessError err) {
			throw new IllegalStateException(
					"Readability mismatch in inheritance hierarchy of class [" + className + "]: " + err.getMessage(), err);
		} catch (Throwable ex) {
			// Typically ClassNotFoundException or NoClassDefFoundError...
		}
	}

	/**
	 * Loads the class with the given name using the given {@link ClassLoader}.
	 *
	 * @param name the name of the class to be loaded.
	 * @param classLoader the class loader to use (can be {@literal null}, which indicates the default class loader).
	 * @return the {@link Class} or {@literal null} in case the class can't be loaded for any reason.
	 */
	public static @Nullable Class<?> loadIfPresent(String name, @Nullable ClassLoader classLoader) {

		try {
			return org.springframework.util.ClassUtils.forName(name, classLoader);
		} catch (Exception o_O) {
			return null;
		}
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature, and return it if available
	 * (else return {@code null}).
	 * <p>
	 * Essentially translates {@code NoSuchMethodException} to {@code null}.
	 *
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return the constructor, or {@code null} if not found
	 * @see Class#getDeclaredConstructor
	 * @since 4.0
	 */
	public static <T> @Nullable Constructor<T> getDeclaredConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {

		Assert.notNull(clazz, "Class must not be null");

		try {
			return clazz.getDeclaredConstructor(paramTypes);
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}

}
