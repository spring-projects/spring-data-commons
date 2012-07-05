/*
 * Copyright 2012 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link RepositoryConfigurationExtension} to ease the implementation of the interface. Will
 * default the default named query location based on a module prefix provided by implementors (see
 * {@link #getModulePrefix()}). Stubs out the post-processing methods as they might not be needed by default.
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryConfigurationExtensionSupport implements RepositoryConfigurationExtension {

	protected static final String REPOSITORY_INTERFACE_POST_PROCESSOR = "org.springframework.data.repository.core.support.RepositoryInterfaceAwareBeanPostProcessor";

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getRepositoryConfigurations(org.springframework.data.repository.config.RepositoryConfigurationSource, org.springframework.core.io.ResourceLoader)
	 */
	public <T extends RepositoryConfigurationSource> Collection<RepositoryConfiguration<T>> getRepositoryConfigurations(
			T configSource, ResourceLoader loader) {

		Assert.notNull(configSource);
		Assert.notNull(loader);

		Set<RepositoryConfiguration<T>> result = new HashSet<RepositoryConfiguration<T>>();

		for (String candidate : configSource.getCandidates(loader)) {
			result.add(getRepositoryConfiguration(candidate, configSource));
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
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		AbstractBeanDefinition definition = BeanDefinitionBuilder.rootBeanDefinition(REPOSITORY_INTERFACE_POST_PROCESSOR)
				.getBeanDefinition();

		registerWithSourceAndGeneratedBeanName(registry, definition, configurationSource.getSource());
	}

	/**
	 * Returns the prefix of the module to be used to create the default location for Spring Data named queries.
	 * 
	 * @return must not be {@literal null}.
	 */
	protected abstract String getModulePrefix();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource)
	 */
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.XmlRepositoryConfigurationSource)
	 */
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

	}

	/**
	 * Sets the given source on the given {@link AbstractBeanDefinition} and registers it inside the given
	 * {@link BeanDefinitionRegistry}.
	 * 
	 * @param registry
	 * @param bean
	 * @param source
	 * @return
	 */
	public static String registerWithSourceAndGeneratedBeanName(BeanDefinitionRegistry registry,
			AbstractBeanDefinition bean, Object source) {

		bean.setSource(source);

		String beanName = generateBeanName(bean, registry);
		registry.registerBeanDefinition(beanName, bean);

		return beanName;
	}

	/**
	 * Returns whether the given {@link BeanDefinitionRegistry} already contains a bean of the given type assuming the
	 * bean name has been autogenerated.
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
	 * customize the behaviour.
	 * 
	 * @param interfaceName will never be {@literal null} or empty.
	 * @param configSource will never be {@literal null}.
	 * @return
	 */
	protected <T extends RepositoryConfigurationSource> RepositoryConfiguration<T> getRepositoryConfiguration(
			String interfaceName, T configSource) {
		return new DefaultRepositoryConfiguration<T>(configSource, interfaceName);
	}
}
