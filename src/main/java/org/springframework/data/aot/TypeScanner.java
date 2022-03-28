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
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 */
public class TypeScanner { // TODO: replace this with AnnotatedTypeScanner maybe?

	private final ClassLoader classLoader;

	public TypeScanner(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	static TypeScanner scanner(ClassLoader classLoader) {
		return new TypeScanner(classLoader);
	}

	public Scanner scanForTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
		return scanForTypesAnnotatedWith(Arrays.asList(annotations));
	}

	public Scanner scanForTypesAnnotatedWith(Collection<Class<? extends Annotation>> annotations) {
		return new ScannerImpl().includeTypesAnnotatedWith(annotations);
	}

	public interface Scanner {

		default Set<Class<?>> inPackages(String... packageNames) {
			return inPackages(Arrays.asList(packageNames));
		}

		Set<Class<?>> inPackages(Collection<String> packageNames);
	}

	class ScannerImpl implements Scanner {

		ClassPathScanningCandidateComponentProvider componentProvider;

		public ScannerImpl() {

			componentProvider = new ClassPathScanningCandidateComponentProvider(false);
			componentProvider.setEnvironment(new StandardEnvironment());
			componentProvider.setResourceLoader(new DefaultResourceLoader(classLoader));
		}

		ScannerImpl includeTypesAnnotatedWith(Collection<Class<? extends Annotation>> annotations) {
			annotations.stream().map(AnnotationTypeFilter::new).forEach(componentProvider::addIncludeFilter);
			return this;
		}

		@Override
		public Set<Class<?>> inPackages(Collection<String> packageNames) {

			Set<Class<?>> types = new LinkedHashSet<>();

			packageNames.forEach(pkg -> {
				componentProvider.findCandidateComponents(pkg).forEach(it -> {
					resolveType(it.getBeanClassName()).ifPresent(types::add);
				});
			});

			return types;
		}
	}

	private Optional<Class<?>> resolveType(String typeName) {

		if (!ClassUtils.isPresent(typeName, classLoader)) {
			return Optional.empty();
		}
		try {
			return Optional.of(ClassUtils.forName(typeName, classLoader));
		} catch (ClassNotFoundException e) {
			// just do nothing
		}
		return Optional.empty();
	}

}
