/*
 * Copyright 2022. the original author or authors.
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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.aot.AotContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeCollector;
import org.springframework.data.util.TypeUtils;

/**
 * Default implementation of {@link AotRepositoryContext}
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see AotRepositoryContext
 * @since 3.0
 */
@SuppressWarnings("NullAway") // TODO
class DefaultAotRepositoryContext implements AotRepositoryContext {

	private final AotContext aotContext;
	private final Lazy<Set<MergedAnnotation<Annotation>>> resolvedAnnotations = Lazy.of(this::discoverAnnotations);
	private final Lazy<Set<Class<?>>> managedTypes = Lazy.of(this::discoverTypes);

	private @Nullable RepositoryInformation repositoryInformation;
	private @Nullable Set<String> basePackages;
	private @Nullable Set<Class<? extends Annotation>> identifyingAnnotations;
	private @Nullable String beanName;

	public DefaultAotRepositoryContext(AotContext aotContext) {
		this.aotContext = aotContext;
	}

	public AotContext getAotContext() {
		return aotContext;
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return getAotContext().getBeanFactory();
	}

	@Override
	public Set<String> getBasePackages() {
		return basePackages == null ? Collections.emptySet() : basePackages;
	}

	public void setBasePackages(Set<String> basePackages) {
		this.basePackages = basePackages;
	}

	@Override
	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public Set<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return identifyingAnnotations == null ? Collections.emptySet() : identifyingAnnotations;
	}

	public void setIdentifyingAnnotations(Set<Class<? extends Annotation>> identifyingAnnotations) {
		this.identifyingAnnotations = identifyingAnnotations;
	}

	@Override
	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}

	public void setRepositoryInformation(RepositoryInformation repositoryInformation) {
		this.repositoryInformation = repositoryInformation;
	}

	@Override
	public Set<MergedAnnotation<Annotation>> getResolvedAnnotations() {
		return resolvedAnnotations.get();
	}

	@Override
	public Set<Class<?>> getResolvedTypes() {
		return managedTypes.get();
	}

	@Override
	public AotContext.TypeIntrospector introspectType(String typeName) {
		return aotContext.introspectType(typeName);
	}

	@Override
	public AotContext.IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
		return aotContext.introspectBeanDefinition(beanName);
	}

	protected Set<MergedAnnotation<Annotation>> discoverAnnotations() {

		Set<MergedAnnotation<Annotation>> annotations = getResolvedTypes().stream()
				.flatMap(type -> TypeUtils.resolveUsedAnnotations(type).stream())
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (repositoryInformation != null) {
			annotations.addAll(TypeUtils.resolveUsedAnnotations(repositoryInformation.getRepositoryInterface()));
		}

		return annotations;
	}

	protected Set<Class<?>> discoverTypes() {

		Set<Class<?>> types = new LinkedHashSet<>();

		if (repositoryInformation != null) {
			types.addAll(TypeCollector.inspect(repositoryInformation.getDomainType()).list());

			repositoryInformation.getQueryMethods()
					.flatMap(it -> TypeUtils.resolveTypesInSignature(repositoryInformation.getRepositoryInterface(), it).stream())
					.flatMap(it -> TypeCollector.inspect(it).list().stream()).forEach(types::add);
		}

		if (!getIdentifyingAnnotations().isEmpty()) {

			Set<Class<?>> classes = aotContext.getTypeScanner().scanPackages(getBasePackages())
					.forTypesAnnotatedWith(getIdentifyingAnnotations()).collectAsSet();
			types.addAll(TypeCollector.inspect(classes).list());
		}

		return types;
	}
}
