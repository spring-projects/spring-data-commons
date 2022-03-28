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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.aot.TypeScanner.Scanner;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.Lazy;

/**
 * Default implementation of {@link AotRepositoryContext}
 *
 * @author Christoph Strobl
 * @since 3.0
 */
class DefaultRepositoryContext implements AotRepositoryContext {

	private AotContext aotContext;

	private String beanName;
	private java.util.Set<String> basePackages;
	private RepositoryInformation repositoryInformation;
	private Set<Class<? extends Annotation>> identifyingAnnotations;
	private Lazy<Set<MergedAnnotation<Annotation>>> resolvedAnnotations = Lazy.of(this::discoverAnnotations);
	private Lazy<Set<Class<?>>> managedTypes = Lazy.of(this::discoverTypes);

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return aotContext.getBeanFactory();
	}

	@Override
	public String getBeanName() {
		return beanName;
	}

	@Override
	public Set<String> getBasePackages() {
		return basePackages;
	}

	@Override
	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	@Override
	public Set<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return identifyingAnnotations;
	}

	@Override
	public Set<Class<?>> getResolvedTypes() {
		return managedTypes.get();
	}

	@Override
	public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
		return resolvedAnnotations.get();
	}

	public AotContext getAotContext() {
		return aotContext;
	}

	public void setAotContext(AotContext aotContext) {
		this.aotContext = aotContext;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setBasePackages(Set<String> basePackages) {
		this.basePackages = basePackages;
	}

	public void setRepositoryInformation(RepositoryInformation repositoryInformation) {
		this.repositoryInformation = repositoryInformation;
	}

	public void setIdentifyingAnnotations(Set<Class<? extends Annotation>> identifyingAnnotations) {
		this.identifyingAnnotations = identifyingAnnotations;
	}

	protected Set<MergedAnnotation<Annotation>> discoverAnnotations() {

		Set<MergedAnnotation<Annotation>> annotations = new LinkedHashSet<>(getResolvedTypes().stream().flatMap(type -> {
			return TypeUtils.resolveUsedAnnotations(type).stream();
		}).collect(Collectors.toSet()));
		annotations.addAll(TypeUtils.resolveUsedAnnotations(repositoryInformation.getRepositoryInterface()));
		return annotations;
	}

	protected Set<Class<?>> discoverTypes() {

		Set<Class<?>> types = new LinkedHashSet<>(TypeCollector.inspect(repositoryInformation.getDomainType()).list());

		repositoryInformation.getQueryMethods()
				.flatMap(it -> TypeUtils.resolveTypesInSignature(repositoryInformation.getRepositoryInterface(), it).stream())
				.flatMap(it -> TypeCollector.inspect(it).list().stream()).forEach(types::add);

		if (!getIdentifyingAnnotations().isEmpty()) {

			Scanner typeScanner = aotContext.getTypeScanner().scanForTypesAnnotatedWith(getIdentifyingAnnotations());
			Set<Class<?>> classes = typeScanner.inPackages(getBasePackages());
			TypeCollector.inspect(classes).list().stream().forEach(types::add);
		}

		// context.get
		return types;
	}
}
