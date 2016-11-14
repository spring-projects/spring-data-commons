/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link InstantiationAwareBeanPostProcessorAdapter} to predict the bean type for {@link FactoryBean} implementations
 * by interpreting a configured property of the {@link BeanDefinition} as type to be created eventually.
 * 
 * @author Oliver Gierke
 * @since 1.12
 * @soundtrack Ron Spielmann - Lock Me Up (Electric Tales)
 */
public class FactoryBeanTypePredictingBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements BeanFactoryAware, PriorityOrdered {

	private static final Logger LOGGER = LoggerFactory.getLogger(FactoryBeanTypePredictingBeanPostProcessor.class);

	private final Map<String, Class<?>> cache = new ConcurrentHashMap<String, Class<?>>();
	private final Class<?> factoryBeanType;
	private final List<String> properties;
	private Optional<ConfigurableListableBeanFactory> context = Optional.empty();

	/**
	 * Creates a new {@link FactoryBeanTypePredictingBeanPostProcessor} predicting the type created by the
	 * {@link FactoryBean} of the given type by inspecting the {@link BeanDefinition} and considering the value for the
	 * given property as type to be created eventually.
	 * 
	 * @param factoryBeanType must not be {@literal null}.
	 * @param properties must not be {@literal null} or empty.
	 */
	public FactoryBeanTypePredictingBeanPostProcessor(Class<?> factoryBeanType, String... properties) {

		Assert.notNull(factoryBeanType, "FactoryBean type must not be null!");
		Assert.isTrue(FactoryBean.class.isAssignableFrom(factoryBeanType), "Given type is not a FactoryBean type!");
		Assert.notEmpty(properties, "Properties must not be empty!");

		for (String property : properties) {
			Assert.hasText(property, "Type property must not be null!");
		}

		this.factoryBeanType = factoryBeanType;
		this.properties = Arrays.asList(properties);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {

		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.context = Optional.of((ConfigurableListableBeanFactory) beanFactory);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter#predictBeanType(java.lang.Class, java.lang.String)
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {

		return context.map(it -> {

			if (!factoryBeanType.isAssignableFrom(beanClass)) {
				return null;
			}

			BeanDefinition definition = it.getBeanDefinition(beanName);
			Class<?> resort = Void.class;

			Class<?> resolvedBeanClass = cache.computeIfAbsent(beanName,
					name -> properties.stream()//
							.map(property -> definition.getPropertyValues().getPropertyValue(property))//
							.map(value -> getClassForPropertyValue(value, beanName, it))//
							.filter(type -> !Void.class.equals(type))//
							.findFirst().orElse((Class) resort));

			return Void.class.equals(resolvedBeanClass) ? null : resolvedBeanClass;

		}).orElse(null);
	}

	/**
	 * Returns the class which is configured in the given {@link PropertyValue}. In case it is not a
	 * {@link TypedStringValue} or the value contained cannot be interpreted as {@link Class} it will return {@link Void}.
	 * 
	 * @param propertyValue can be {@literal null}.
	 * @param beanName must not be {@literal null}.
	 * @return
	 */
	private Class<?> getClassForPropertyValue(PropertyValue propertyValue, String beanName,
			ConfigurableListableBeanFactory beanFactory) {

		if (propertyValue == null) {
			return Void.class;
		}

		Object value = propertyValue.getValue();
		String className = null;

		if (value instanceof TypedStringValue) {
			className = ((TypedStringValue) value).getValue();
		} else if (value instanceof String) {
			className = (String) value;
		} else if (value instanceof Class<?>) {
			return (Class<?>) value;
		} else if (value instanceof String[]) {

			String[] values = (String[]) value;

			if (values.length == 0) {
				return Void.class;
			} else {
				className = values[0];
			}

		} else {
			return Void.class;
		}

		try {
			return ClassUtils.resolveClassName(className, beanFactory.getBeanClassLoader());
		} catch (IllegalArgumentException ex) {
			LOGGER.warn(
					String.format("Couldn't load class %s referenced as repository interface in bean %s!", className, beanName));
			return Void.class;
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}
}
