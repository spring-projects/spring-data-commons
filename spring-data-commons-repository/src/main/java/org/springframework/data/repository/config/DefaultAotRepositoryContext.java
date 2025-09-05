/*
 * Copyright 2022-2025 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
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
 * @author Mark Paluch
 * @see AotRepositoryContext
 * @since 3.0
 */
@SuppressWarnings("NullAway") // TODO
class DefaultAotRepositoryContext implements AotRepositoryContext {

	private final RegisteredBean bean;
	private final String moduleName;
	private final RepositoryConfigurationSource configurationSource;
	private final AotContext aotContext;
	private final RepositoryInformation repositoryInformation;
	private final Lazy<Set<MergedAnnotation<Annotation>>> resolvedAnnotations = Lazy.of(this::discoverAnnotations);
	private final Lazy<Set<Class<?>>> managedTypes = Lazy.of(this::discoverTypes);

	private Set<String> basePackages = Collections.emptySet();
	private Collection<Class<? extends Annotation>> identifyingAnnotations = Collections.emptySet();
	private String beanName;

	public DefaultAotRepositoryContext(RegisteredBean bean, RepositoryInformation repositoryInformation,
			String moduleName, AotContext aotContext, RepositoryConfigurationSource configurationSource) {
		this.bean = bean;
		this.repositoryInformation = repositoryInformation;
		this.moduleName = moduleName;
		this.configurationSource = configurationSource;
		this.aotContext = aotContext;
		this.beanName = bean.getBeanName();
		this.basePackages = configurationSource.getBasePackages().toSet();
	}

	public AotContext getAotContext() {
		return aotContext;
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}

	@Override
	public RepositoryConfigurationSource getConfigurationSource() {
		return configurationSource;
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return getAotContext().getBeanFactory();
	}

	@Override
	public Environment getEnvironment() {
		return getAotContext().getEnvironment();
	}

	@Override
	public Set<String> getBasePackages() {
		return basePackages;
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
	public Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return identifyingAnnotations;
	}

	public void setIdentifyingAnnotations(Collection<Class<? extends Annotation>> identifyingAnnotations) {
		this.identifyingAnnotations = identifyingAnnotations;
	}

	@Override
	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
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

		annotations.addAll(TypeUtils.resolveUsedAnnotations(repositoryInformation.getRepositoryInterface()));

		return annotations;
	}

	protected Set<Class<?>> discoverTypes() {

		Set<Class<?>> types = new LinkedHashSet<>(TypeCollector.inspect(repositoryInformation.getDomainType()).list());

		repositoryInformation.getQueryMethods().stream()
				.flatMap(it -> TypeUtils.resolveTypesInSignature(repositoryInformation.getRepositoryInterface(), it).stream())
				.flatMap(it -> TypeCollector.inspect(it).list().stream()).forEach(types::add);

		if (!getIdentifyingAnnotations().isEmpty()) {

			Set<Class<?>> classes = aotContext.getTypeScanner().scanPackages(getBasePackages())
					.forTypesAnnotatedWith(getIdentifyingAnnotations()).collectAsSet();
			types.addAll(TypeCollector.inspect(classes).list());
		}

		return types;
	}

}
