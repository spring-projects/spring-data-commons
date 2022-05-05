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
package org.springframework.data.aot;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Scanner used to scan for {@link Class types} that will be added to the AOT processing infrastructure.
 *
 * @author Christoph Strobl
 * @author John Blum
 */
// TODO: Replace this with AnnotatedTypeScanner, maybe?
public class TypeScanner {

	/**
	 * Factory method used to construct a new {@link TypeScanner} initialized with the given {@link ClassLoader}
	 * used to resolve scanned {@link Class type}.
	 *
	 * @param classLoader {@link ClassLoader} used to resolve scanned {@link Class types}.
	 * @return a new {@link TypeScanner}.
	 */
	@NonNull
	static TypeScanner scanner(@Nullable ClassLoader classLoader) {
		return new TypeScanner(classLoader);
	}

	private final ClassLoader classLoader;

	/**
	 * Constructs a new instance of {@link TypeScanner} initialized with the given {@link ClassLoader}
	 * used to resolve scanned {@link Class types}.
	 *
	 * @param classLoader {@link ClassLoader} used to resolve scanned {@link Class types}.
	 */
	public TypeScanner(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Scans for {@link Class types} potentially annotated with any of the given {@link Annotation} types.
	 *
	 * @param annotations array of {@link Annotation} types used to filter the scanned {@link Class types};
	 * must not be {@literal null}.
	 * @return new instance of {@link Scanner}.
	 * @see #scanForTypesAnnotatedWith(Collection)
	 * @see Scanner
	 */
	@SuppressWarnings("unchecked")
	public Scanner scanForTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
		return scanForTypesAnnotatedWith(Arrays.asList(annotations));
	}

	/**
	 * Scans for {@link Class types} potentially annotated with any of the given {@link Annotation} types.
	 *
	 * @param annotations {@link Collection} of {@link Annotation} types used to filter the scanned {@link Class types}.
	 * @return new instance of {@link Scanner}.
	 * @see #scanForTypesAnnotatedWith(Class[])
	 * @see Scanner
	 */
	public Scanner scanForTypesAnnotatedWith(Collection<Class<? extends Annotation>> annotations) {
		return new ScannerImpl().includeTypesAnnotatedWith(annotations);
	}

	public interface Scanner {

		/**
		 * Collects the {@link String names} of packages to scan.
		 *
		 * @param packageNames array of {@link String package names} ot scan; must not be {@literal null}.
		 * @return the resolved, scanned {@link Class types}.
		 * @see #inPackages(Collection)
		 */
		default Set<Class<?>> inPackages(String... packageNames) {
			return inPackages(Arrays.asList(packageNames));
		}

		/**
		 * Collects the {@link String names} of packages to scan.
		 *
		 * @param packageNames {@link Collection} of {@link String package names} ot scan.
		 * @return the resolved, scanned {@link Class types}.
		 * @see #inPackages(String...)
		 */
		Set<Class<?>> inPackages(Collection<String> packageNames);
	}

	class ScannerImpl implements Scanner {

		ClassPathScanningCandidateComponentProvider componentProvider;

		ScannerImpl() {

			componentProvider = new ClassPathScanningCandidateComponentProvider(false);
			componentProvider.setEnvironment(new StandardEnvironment());
			componentProvider.setResourceLoader(new DefaultResourceLoader(classLoader));
		}

		ScannerImpl includeTypesAnnotatedWith(Collection<Class<? extends Annotation>> annotations) {

			nullSafeCollection(annotations).stream()
					.map(AnnotationTypeFilter::new)
					.forEach(componentProvider::addIncludeFilter);

			return this;
		}

		@Override
		public Set<Class<?>> inPackages(Collection<String> packageNames) {

			Set<Class<?>> types = new LinkedHashSet<>();

			nullSafeCollection(packageNames).forEach(pkg ->
				componentProvider.findCandidateComponents(pkg).forEach(it ->
					resolveType(it.getBeanClassName()).ifPresent(types::add)));

			return types;
		}

		@NonNull
		private <T> Collection<T> nullSafeCollection(@Nullable Collection<T> collection) {
			return collection != null ? collection : Collections.emptySet();
		}
	}

	private Optional<Class<?>> resolveType(String typeName) {

		if (ClassUtils.isPresent(typeName, classLoader)) {
			try {
				return Optional.of(ClassUtils.forName(typeName, classLoader));
			} catch (ClassNotFoundException ignore) {
				// just do nothing
			}
		}

		return Optional.empty();
	}
}
