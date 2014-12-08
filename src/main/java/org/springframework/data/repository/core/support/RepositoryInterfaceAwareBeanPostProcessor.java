/*
 * Copyright 2008-2014 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;

/**
 * A {@link org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor} implementing
 * {@code #predictBeanType(Class, String)} to return the configured repository interface from
 * {@link RepositoryFactoryBeanSupport}s. This is done as shortcut to prevent the need of instantiating
 * {@link RepositoryFactoryBeanSupport}s just to find out what repository interface they actually create.
 * 
 * @author Oliver Gierke
 */
class RepositoryInterfaceAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements
		BeanFactoryAware, PriorityOrdered {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryInterfaceAwareBeanPostProcessor.class);
	private static final Class<?> REPOSITORY_TYPE = RepositoryFactoryBeanSupport.class;

	private final Map<String, Class<?>> cache = new ConcurrentHashMap<String, Class<?>>();
	private ConfigurableListableBeanFactory context;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {

		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.context = (ConfigurableListableBeanFactory) beanFactory;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter#predictBeanType(java.lang.Class, java.lang.String)
	 */
	@Override
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {

		if (null == context || !REPOSITORY_TYPE.isAssignableFrom(beanClass)) {
			return null;
		}

		Class<?> resolvedBeanClass = cache.get(beanName);

		if (resolvedBeanClass != null) {
			return resolvedBeanClass == Void.class ? null : resolvedBeanClass;
		}

		BeanDefinition definition = context.getBeanDefinition(beanName);
		PropertyValue value = definition.getPropertyValues().getPropertyValue("repositoryInterface");

		resolvedBeanClass = getClassForPropertyValue(value, beanName);
		cache.put(beanName, resolvedBeanClass);

		return resolvedBeanClass == Void.class ? null : resolvedBeanClass;
	}

	/**
	 * Returns the class which is configured in the given {@link PropertyValue}. In case it is not a
	 * {@link TypedStringValue} or the value contained cannot be interpreted as {@link Class} it will return null.
	 * 
	 * @param propertyValue
	 * @param beanName
	 * @return
	 */
	private Class<?> getClassForPropertyValue(PropertyValue propertyValue, String beanName) {

		Object value = propertyValue.getValue();
		String className = null;

		if (value instanceof TypedStringValue) {
			className = ((TypedStringValue) value).getValue();
		} else if (value instanceof String) {
			className = (String) value;
		} else if (value instanceof Class<?>) {
			return (Class<?>) value;
		} else {
			return Void.class;
		}

		try {
			return ClassUtils.resolveClassName(className, context.getBeanClassLoader());
		} catch (IllegalArgumentException ex) {
			LOGGER.warn(String.format("Couldn't load class %s referenced as repository interface in bean %s!", className,
					beanName));
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
