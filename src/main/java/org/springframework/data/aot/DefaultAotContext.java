/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;

/**
 * Default {@link AotContext} implementation.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class DefaultAotContext implements AotContext {

	private final ConfigurableListableBeanFactory factory;

	public DefaultAotContext(BeanFactory beanFactory) {
		factory = beanFactory instanceof ConfigurableListableBeanFactory ? (ConfigurableListableBeanFactory) beanFactory
				: new DefaultListableBeanFactory(beanFactory);
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		return factory;
	}

	@Override
	public TypeIntrospector introspectType(String typeName) {
		return new DefaultTypeIntrospector(typeName);
	}

	@Override
	public IntrospectedBeanDefinition introspectBeanDefinition(String beanName) {
		return new DefaultIntrospectedBeanDefinition(beanName);
	}

	class DefaultTypeIntrospector implements TypeIntrospector {

		private final String typeName;

		DefaultTypeIntrospector(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public boolean isTypePresent() {
			return ClassUtils.isPresent(typeName, getClassLoader());
		}

		@Override
		public Class<?> resolveRequiredType() throws TypeNotPresentException {
			try {
				return ClassUtils.forName(typeName, getClassLoader());
			} catch (ClassNotFoundException cause) {
				throw new TypeNotPresentException(typeName, cause);
			}
		}

		@Override
		public Optional<Class<?>> resolveType() {
			return isTypePresent() ? Optional.of(resolveRequiredType()) : Optional.empty();
		}

		@Override
		public boolean hasBean() {
			return !getBeanNames().isEmpty();
		}

		@Override
		public List<String> getBeanNames() {
			return isTypePresent() ? Arrays.asList(factory.getBeanNamesForType(resolveRequiredType()))
					: Collections.emptyList();
		}
	}

	class DefaultIntrospectedBeanDefinition implements IntrospectedBeanDefinition {

		private final String beanName;

		DefaultIntrospectedBeanDefinition(String beanName) {
			this.beanName = beanName;
		}

		@Override
		public boolean isPresent() {
			return factory.containsBeanDefinition(beanName);
		}

		@Override
		public boolean isFactoryBean() {
			return factory.isFactoryBean(beanName);
		}

		@Override
		public BeanDefinition getBeanDefinition() throws NoSuchBeanDefinitionException {
			return factory.getBeanDefinition(beanName);
		}

		@Override
		public RootBeanDefinition getRootBeanDefinition() throws NoSuchBeanDefinitionException {
			BeanDefinition beanDefinition = getBeanDefinition();

			if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
				return rootBeanDefinition;
			}

			throw new IllegalStateException(String.format("%s is not a root bean", beanName));
		}

		@Override
		public Class<?> resolveType() {
			return factory.getType(beanName, false);
		}
	}
}
