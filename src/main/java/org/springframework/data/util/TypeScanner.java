/*
 * Copyright 2022 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * A scanner that searches the classpath for matching types within given target packages.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
public interface TypeScanner {

	/**
	 * Create a new {@link TypeScanner} using the given {@link ClassLoader}.
	 *
	 * @param classLoader must not be {@literal null}.
	 * @return new instance of {@link TypeScanner}.
	 */
	static TypeScanner typeScanner(ClassLoader classLoader) {

		Assert.notNull(classLoader, "ClassLoader must not be null!");
		return typeScanner(new DefaultResourceLoader(classLoader));
	}

	/**
	 * Create a new {@link TypeScanner} using the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader must not be {@literal null}.
	 * @return new instance of {@link TypeScanner}.
	 */
	static TypeScanner typeScanner(ResourceLoader resourceLoader) {

		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");
		return new DelegatingTypeScanner(resourceLoader);
	}

	/**
	 * Create a new {@link TypeScanner} using the given {@link ApplicationContext}.
	 *
	 * @param context must not be {@literal null}.
	 * @return new instance of {@link TypeScanner}.
	 */
	static TypeScanner typeScanner(ApplicationContext context) {

		Assert.notNull(context, "Context must not be null!");
		return new DelegatingTypeScanner(context.getEnvironment(), context);
	}

	/**
	 * Collects the {@link String names} of packages to scan.
	 *
	 * @param packageNames array of {@link String package names} to scan; Must not be {@literal null}.
	 * @return new instance of {@link TypeScanner}.
	 * @see #scanPackages(Collection)
	 */
	default TypeScanner scanPackages(String... packageNames) {
		return scanPackages(Arrays.asList(packageNames));
	}

	/**
	 * Collects the {@link String names} of packages to scan.
	 *
	 * @param packageNames {@link Collection} of {@link String package names} to scan.
	 * @return new instance of {@link TypeScanner}.
	 * @see #scanPackages(String...)
	 */
	TypeScanner scanPackages(Collection<String> packageNames);

	/**
	 * Define annotations identifying types to include in the scan result.
	 *
	 * @param annotations must not be {@literal null}.
	 * @return new instance of {@link TypeScanner}.
	 * @see #forTypesAnnotatedWith(Collection)
	 */
	default TypeScanner forTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
		return forTypesAnnotatedWith(Arrays.asList(annotations));
	}

	/**
	 * Define annotations identifying types to include in the scan result.
	 *
	 * @param annotations must not be {@literal null}.
	 * @return new instance of {@link TypeScanner}.
	 */
	TypeScanner forTypesAnnotatedWith(Collection<Class<? extends Annotation>> annotations);

	/**
	 * Define what happens in the case of a {@link ClassNotFoundException}.
	 *
	 * @param action must not be {@literal null}.
	 * @return
	 */
	TypeScanner onClassNotFound(Consumer<ClassNotFoundException> action);

	/**
	 * Obtain the scan result.
	 *
	 * @return never {@literal null}.
	 */
	default Set<Class<?>> collectAsSet() {

		LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
		forEach(result::add);
		return result;
	}

	/**
	 * Performs the given action for each element found while scanning.
	 *
	 * @param action must not be {@literal null}.
	 */
	void forEach(Consumer<Class<?>> action);
}
