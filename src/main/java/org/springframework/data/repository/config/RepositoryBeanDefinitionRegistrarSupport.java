/*
 * Copyright 2012-2013 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base class to implement {@link ImportBeanDefinitionRegistrar}s to enable repository
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryBeanDefinitionRegistrarSupport implements ImportBeanDefinitionRegistrar,
		BeanClassLoaderAware, ResourceLoaderAware, EnvironmentAware {

	private ResourceLoader resourceLoader;
	private ClassLoader beanClassLoader;
	private Environment environment;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.EnvironmentAware#setEnvironment(org.springframework.core.env.Environment)
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

		Assert.notNull(annotationMetadata);
		Assert.notNull(registry);

		// Guard against calls for sub-classes
		if (annotationMetadata.getAnnotationAttributes(getAnnotation().getName()) == null) {
			return;
		}

		defaultExternalResources(registry);

		AnnotationRepositoryConfigurationSource configuration = new AnnotationRepositoryConfigurationSource(
				annotationMetadata, getAnnotation(), environment);

		RepositoryConfigurationExtension extension = getExtension();
		extension.registerBeansForRoot(registry, configuration);

		RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
		generator.setBeanClassLoader(beanClassLoader);

		for (RepositoryConfiguration<AnnotationRepositoryConfigurationSource> repositoryConfiguration : extension
				.getRepositoryConfigurations(configuration, resourceLoader)) {

			RepositoryBeanDefinitionBuilder builder = new RepositoryBeanDefinitionBuilder(repositoryConfiguration, extension);
			BeanDefinitionBuilder definitionBuilder = builder.build(registry, resourceLoader);

			extension.postProcess(definitionBuilder, configuration);

			String beanName = generator.generateBeanName(definitionBuilder.getBeanDefinition(), registry);
			registry.registerBeanDefinition(beanName, definitionBuilder.getBeanDefinition());
		}
	}

	/**
	 * Workaround the lack of injectability of external resources into {@link ImportBeanDefinitionRegistrar}s in the
	 * Spring 3.1 timeline. We populate {@link #beanClassLoader} and default the {@link #resourceLoader} in case they
	 * haven't been set until a call to this method.
	 * 
	 * @param registry must not be {@literal null}.
	 * @see SPR-9568
	 */
	private void defaultExternalResources(BeanDefinitionRegistry registry) {

		if (beanClassLoader == null) {
			this.beanClassLoader = getBeanClassLoader(registry);
		}

		if (resourceLoader == null) {
			this.resourceLoader = new DefaultResourceLoader(this.beanClassLoader);
		}
	}

	/**
	 * Returns the bean class loader contained in the given registry if it is a {@link ConfigurableBeanFactory}. Falls
	 * back to the {@link ResourceLoader}'s {@link ClassLoader} or the global default one if that one is {@literal null}
	 * in turn.
	 * 
	 * @param registry must not be {@literal null}.
	 * @return
	 */
	private ClassLoader getBeanClassLoader(BeanDefinitionRegistry registry) {

		if (registry instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) registry).getBeanClassLoader();
		}

		return resourceLoader == null ? ClassUtils.getDefaultClassLoader() : resourceLoader.getClassLoader();
	}

	/**
	 * Return the annotation to obtain configuration information from. Will be wrappen into an
	 * {@link AnnotationRepositoryConfigurationSource} so have a look at the constants in there for what annotation
	 * attributes it expects.
	 * 
	 * @return
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * Returns the {@link RepositoryConfigurationExtension} for store specific callbacks and {@link BeanDefinition}
	 * post-processing.
	 * 
	 * @see RepositoryConfigurationExtensionSupport
	 * @return
	 */
	protected abstract RepositoryConfigurationExtension getExtension();
}
