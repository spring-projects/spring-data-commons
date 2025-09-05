/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Special {@link BeanNameGenerator} to create bean names for Spring Data repositories. Will delegate to an
 * {@link AnnotationBeanNameGenerator} but let the delegate work with a customized {@link BeanDefinition} to make sure
 * the repository interface is inspected and not the actual bean definition class.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Johannes Englmeier
 */
class RepositoryBeanNameGenerator {

	private final ClassLoader beanClassLoader;
	private final BeanNameGenerator generator;
	private final BeanDefinitionRegistry registry;

	/**
	 * Creates a new {@link RepositoryBeanNameGenerator} for the given {@link ClassLoader}, {@link BeanNameGenerator}, and
	 * {@link BeanDefinitionRegistry}.
	 *
	 * @param beanClassLoader must not be {@literal null}.
	 * @param generator must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 */
	public RepositoryBeanNameGenerator(ClassLoader beanClassLoader, BeanNameGenerator generator,
			BeanDefinitionRegistry registry) {

		Assert.notNull(beanClassLoader, "Bean ClassLoader must not be null");
		Assert.notNull(generator, "BeanNameGenerator must not be null");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

		this.beanClassLoader = beanClassLoader;
		this.generator = generator;
		this.registry = registry;
	}

	/**
	 * Generate a bean name for the given bean definition.
	 *
	 * @param definition the bean definition to generate a name for
	 * @return the generated bean name
	 * @since 2.0
	 */
	public String generateBeanName(BeanDefinition definition) {

		AnnotatedBeanDefinition beanDefinition = definition instanceof AnnotatedBeanDefinition abd //
				? abd //
				: new AnnotatedGenericBeanDefinition(getRepositoryInterfaceFrom(definition));

		return generator.generateBeanName(beanDefinition, registry);
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

		ConstructorArgumentValues.ValueHolder argumentValue = beanDefinition.getConstructorArgumentValues()
				.getArgumentValue(0, Class.class);

		if (argumentValue == null) {
			throw new IllegalStateException(
					String.format("Failed to obtain first constructor parameter value of BeanDefinition %s", beanDefinition));
		}

		Object value = argumentValue.getValue();

		if (value == null) {

			throw new IllegalStateException(
					String.format("Value of first constructor parameter value of BeanDefinition %s is null", beanDefinition));

		} else if (value instanceof Class<?>) {

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
