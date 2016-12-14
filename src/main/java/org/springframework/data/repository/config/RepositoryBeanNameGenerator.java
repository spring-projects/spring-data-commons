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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.util.ClassUtils;

/**
 * Special {@link BeanNameGenerator} to create bean names for Spring Data repositories. Will delegate to an
 * {@link AnnotationBeanNameGenerator} but let the delegate work with a customized {@link BeanDefinition} to make sure
 * the repository interface is inspected and not the actual bean definition class.
 * 
 * @author Oliver Gierke
 */
public class RepositoryBeanNameGenerator implements BeanNameGenerator, BeanClassLoaderAware {

	private static final BeanNameGenerator DELEGATE = new AnnotationBeanNameGenerator();

	private ClassLoader beanClassLoader;

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName(org.springframework.beans.factory.config.BeanDefinition, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {

		AnnotatedBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(getRepositoryInterfaceFrom(definition));
		return DELEGATE.generateBeanName(beanDefinition, registry);
	}

	/**
	 * Returns the type configured for the {@code repositoryInterface} property of the given bean definition. Uses a
	 * potential {@link Class} being configured as is or tries to load a class with the given value's {@link #toString()}
	 * representation.
	 * 
	 * @param beanDefinition
	 * @return
	 */
	private Class<?> getRepositoryInterfaceFrom(BeanDefinition beanDefinition) {

		Object value = beanDefinition.getConstructorArgumentValues().getArgumentValue(0, Class.class).getValue();

		if (value instanceof Class<?>) {
			return (Class<?>) value;
		} else {
			try {
				return ClassUtils.forName(value.toString(), beanClassLoader);
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}
	}
}
