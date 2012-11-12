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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Base class to implement {@link ImportBeanDefinitionRegistrar}s to enable repository
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryBeanDefinitionRegistrarSupport implements ImportBeanDefinitionRegistrar {

	// see SPR-9568
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

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

		AnnotationRepositoryConfigurationSource configuration = new AnnotationRepositoryConfigurationSource(
				annotationMetadata, getAnnotation());

		RepositoryConfigurationExtension extension = getExtension();
		extension.registerBeansForRoot(registry, configuration);

		RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
		generator.setBeanClassLoader(getBeanClassLoader(registry));

		for (RepositoryConfiguration<AnnotationRepositoryConfigurationSource> repositoryConfiguration : extension
				.getRepositoryConfigurations(configuration, resourceLoader)) {

			RepositoryBeanDefinitionBuilder builder = new RepositoryBeanDefinitionBuilder(repositoryConfiguration, extension);
			BeanDefinitionBuilder definitionBuilder = builder.build(registry, resourceLoader);

			extension.postProcess(definitionBuilder, configuration);

			String beanName = generator.generateBeanName(definitionBuilder.getBeanDefinition(), registry);
			registry.registerBeanDefinition(beanName, definitionBuilder.getBeanDefinition());
		}
	}

	private ClassLoader getBeanClassLoader(BeanDefinitionRegistry registry) {

		if (registry instanceof ConfigurableListableBeanFactory) {
			return ((ConfigurableListableBeanFactory) registry).getBeanClassLoader();
		}

		return resourceLoader.getClassLoader();
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
