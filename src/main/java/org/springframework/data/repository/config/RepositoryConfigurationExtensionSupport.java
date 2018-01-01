/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.repository.config;

import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.*;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base implementation of {@link RepositoryConfigurationExtension} to ease the implementation of the interface. Will
 * default the default named query location based on a module prefix provided by implementors (see
 * {@link #getModulePrefix()}). Stubs out the post-processing methods as they might not be needed by default.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public abstract class RepositoryConfigurationExtensionSupport implements RepositoryConfigurationExtension {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryConfigurationExtensionSupport.class);
	private static final String CLASS_LOADING_ERROR = "%s - Could not load type %s using class loader %s.";
	private static final String MULTI_STORE_DROPPED = "Spring Data {} - Could not safely identify store assignment for repository candidate {}.";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return StringUtils.capitalize(getModulePrefix());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getRepositoryConfigurations(org.springframework.data.repository.config.RepositoryConfigurationSource, org.springframework.core.io.ResourceLoader)
	 */
	public <T extends RepositoryConfigurationSource> Collection<RepositoryConfiguration<T>> getRepositoryConfigurations(
			T configSource, ResourceLoader loader) {
		return getRepositoryConfigurations(configSource, loader, false);
	}

	/*
	 *
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getRepositoryConfigurations(org.springframework.data.repository.config.RepositoryConfigurationSource, org.springframework.core.io.ResourceLoader, boolean)
	 */
	public <T extends RepositoryConfigurationSource> Collection<RepositoryConfiguration<T>> getRepositoryConfigurations(
			T configSource, ResourceLoader loader, boolean strictMatchesOnly) {

		Assert.notNull(configSource, "ConfigSource must not be null!");
		Assert.notNull(loader, "Loader must not be null!");

		Set<RepositoryConfiguration<T>> result = new HashSet<>();

		for (BeanDefinition candidate : configSource.getCandidates(loader)) {

			RepositoryConfiguration<T> configuration = getRepositoryConfiguration(candidate, configSource);
			Class<?> repositoryInterface = loadRepositoryInterface(configuration, loader);

			if (repositoryInterface == null) {
				result.add(configuration);
				continue;
			}

			RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(repositoryInterface);

			if (!useRepositoryConfiguration(metadata)) {
				continue;
			}

			if (!strictMatchesOnly || configSource.usesExplicitFilters()) {
				result.add(configuration);
				continue;
			}

			if (isStrictRepositoryCandidate(metadata)) {
				result.add(configuration);
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getDefaultNamedQueryLocation()
	 */
	public String getDefaultNamedQueryLocation() {
		return String.format("classpath*:META-INF/%s-named-queries.properties", getModulePrefix());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	public void registerBeansForRoot(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {}

	/**
	 * Returns the prefix of the module to be used to create the default location for Spring Data named queries.
	 *
	 * @return must not be {@literal null}.
	 */
	protected abstract String getModulePrefix();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource)
	 */
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.XmlRepositoryConfigurationSource)
	 */
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {}

	/**
	 * Return the annotations to scan domain types for when evaluating repository interfaces for store assignment. Modules
	 * should return the annotations that identify a domain type as managed by the store explicitly.
	 *
	 * @return
	 * @since 1.9
	 */
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.emptySet();
	}

	/**
	 * Returns the types that indicate a store match when inspecting repositories for strict matches.
	 *
	 * @return
	 * @since 1.9
	 */
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.emptySet();
	}

	/**
	 * Sets the given source on the given {@link AbstractBeanDefinition} and registers it inside the given
	 * {@link BeanDefinitionRegistry}. For {@link BeanDefinition}s to be registerd once-and-only-once for all
	 * configuration elements (annotation or XML), prefer calling
	 * {@link #registerIfNotAlreadyRegistered(AbstractBeanDefinition, BeanDefinitionRegistry, String, Object)} with a
	 * dedicated bean name to avoid the bead definition being registered multiple times. *
	 *
	 * @param registry must not be {@literal null}.
	 * @param bean must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 * @return the bean name generated for the given {@link BeanDefinition}
	 */
	public static String registerWithSourceAndGeneratedBeanName(BeanDefinitionRegistry registry,
			AbstractBeanDefinition bean, Object source) {

		bean.setSource(source);

		String beanName = generateBeanName(bean, registry);
		registry.registerBeanDefinition(beanName, bean);

		return beanName;
	}

	/**
	 * Registers the given {@link AbstractBeanDefinition} with the given registry with the given bean name unless the
	 * registry already contains a bean with that name.
	 *
	 * @param bean must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 * @param beanName must not be {@literal null} or empty.
	 * @param source must not be {@literal null}.
	 */
	public static void registerIfNotAlreadyRegistered(AbstractBeanDefinition bean, BeanDefinitionRegistry registry,
			String beanName, Object source) {

		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		bean.setSource(source);
		registry.registerBeanDefinition(beanName, bean);
	}

	/**
	 * Returns whether the given {@link BeanDefinitionRegistry} already contains a bean of the given type assuming the
	 * bean name has been auto-generated.
	 *
	 * @param type
	 * @param registry
	 * @return
	 */
	public static boolean hasBean(Class<?> type, BeanDefinitionRegistry registry) {

		String name = String.format("%s%s0", type.getName(), GENERATED_BEAN_NAME_SEPARATOR);
		return registry.containsBeanDefinition(name);
	}

	/**
	 * Creates a actual {@link RepositoryConfiguration} instance for the given {@link RepositoryConfigurationSource} and
	 * interface name. Defaults to the {@link DefaultRepositoryConfiguration} but allows sub-classes to override this to
	 * customize the behavior.
	 *
	 * @param definition will never be {@literal null} or empty.
	 * @param configSource will never be {@literal null}.
	 * @return
	 */
	protected <T extends RepositoryConfigurationSource> RepositoryConfiguration<T> getRepositoryConfiguration(
			BeanDefinition definition, T configSource) {
		return new DefaultRepositoryConfiguration<>(configSource, definition, this);
	}

	/**
	 * Returns whether the given repository metadata is a candidate for bean definition creation in the strict repository
	 * detection mode. The default implementation inspects the domain type managed for a set of well-known annotations
	 * (see {@link #getIdentifyingAnnotations()}). If none of them is found, the candidate is discarded. Implementations
	 * should make sure, the only return {@literal true} if they're really sure the interface handed to the method is
	 * really a store interface.
	 *
	 * @param repositoryInterface
	 * @return
	 * @since 1.9
	 */
	protected boolean isStrictRepositoryCandidate(RepositoryMetadata metadata) {

		Collection<Class<?>> types = getIdentifyingTypes();
		Class<?> repositoryInterface = metadata.getRepositoryInterface();

		for (Class<?> type : types) {
			if (type.isAssignableFrom(repositoryInterface)) {
				return true;
			}
		}

		Class<?> domainType = metadata.getDomainType();
		Collection<Class<? extends Annotation>> annotations = getIdentifyingAnnotations();

		if (annotations.isEmpty()) {
			return true;
		}

		for (Class<? extends Annotation> annotationType : annotations) {
			if (AnnotationUtils.findAnnotation(domainType, annotationType) != null) {
				return true;
			}
		}

		LOGGER.info(MULTI_STORE_DROPPED, getModuleName(), repositoryInterface);

		return false;
	}

	/**
	 * Return whether to use the configuration for the repository with the given metadata. Defaults to {@literal true}.
	 *
	 * @param metadata will never be {@literal null}.
	 * @return
	 */
	protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
		return true;
	}

	/**
	 * Loads the repository interface contained in the given {@link RepositoryConfiguration} using the given
	 * {@link ResourceLoader}.
	 *
	 * @param configuration must not be {@literal null}.
	 * @param loader must not be {@literal null}.
	 * @return the repository interface or {@literal null} if it can't be loaded.
	 */
	@Nullable
	private Class<?> loadRepositoryInterface(RepositoryConfiguration<?> configuration, ResourceLoader loader) {

		String repositoryInterface = configuration.getRepositoryInterface();
		ClassLoader classLoader = loader.getClassLoader();

		try {
			return org.springframework.util.ClassUtils.forName(repositoryInterface, classLoader);
		} catch (ClassNotFoundException | LinkageError e) {
			LOGGER.warn(String.format(CLASS_LOADING_ERROR, getModuleName(), repositoryInterface, classLoader), e);
		}

		return null;
	}
}
