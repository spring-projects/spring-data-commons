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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * A caching {@link TypeScanner} implementation delegating to {@link AnnotatedTypeScanner}.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class DelegatingTypeScanner implements TypeScanner {

	private final ResourceLoader resourceLoader;
	private final Environment environment;

	private Collection<String> packageNames;
	private Collection<TypeFilter> includeFilters;

	private Consumer<ClassNotFoundException> classNotFoundAction;

	private Lazy<Set<Class<?>>> scanResult = Lazy.of(this::collect);

	DelegatingTypeScanner(ResourceLoader resourceLoader) {
		this(new StandardEnvironment(), resourceLoader);
	}

	DelegatingTypeScanner(Environment environment, ResourceLoader resourceLoader) {

		this(environment, resourceLoader, Collections.emptyList(),
				Collections.singleton((metadataReader, readerFactory) -> true), error -> {});
	}

	DelegatingTypeScanner(Environment environment, ResourceLoader resourceLoader, Collection<String> packageNames,
			Collection<TypeFilter> includeFilters, Consumer<ClassNotFoundException> classNotFoundAction) {

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.packageNames = new ArrayList<>(packageNames);
		this.includeFilters = new ArrayList<>(includeFilters);
		this.classNotFoundAction = classNotFoundAction;
	}

	@Override
	public TypeScanner scanPackages(Collection<String> packageNames) {
		return new DelegatingTypeScanner(environment, resourceLoader, packageNames, includeFilters, classNotFoundAction);
	}

	@Override
	public TypeScanner forTypesAnnotatedWith(Collection<Class<? extends Annotation>> annotations) {

		return new DelegatingTypeScanner(environment, resourceLoader, packageNames,
				annotations.stream().map(DelegatingTypeScanner::annotationFilter).collect(Collectors.toSet()),
				classNotFoundAction);
	}

	@Override
	public TypeScanner onClassNotFound(Consumer<ClassNotFoundException> action) {
		return new DelegatingTypeScanner(environment, resourceLoader, packageNames, includeFilters, action);
	}

	@Override
	public Set<Class<?>> collectAsSet() {
		return scanResult.get();
	}

	@Override
	public void forEach(Consumer<Class<?>> action) {
		collectAsSet().forEach(action);
	}

	private Set<Class<?>> collect() {

		AnnotatedTypeScanner annotatedTypeScanner = new AnnotatedTypeScanner(false, Collections.emptyList());
		annotatedTypeScanner.setResourceLoader(resourceLoader);
		annotatedTypeScanner.setEnvironment(environment);
		if (classNotFoundAction != null) {
			annotatedTypeScanner.setClassNotFoundAction(classNotFoundAction);
		}
		return annotatedTypeScanner.findTypes(packageNames, includeFilters);
	}

	private static AnnotationTypeFilter annotationFilter(Class<? extends Annotation> annotation) {
		return new AnnotationTypeFilter(annotation, true, false);
	}
}
