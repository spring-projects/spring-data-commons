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

import java.util.Collections;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation used to encapsulate common data infrastructure concerns
 * and preprocess the {@link ConfigurableListableBeanFactory} ahead of the AOT compilation in order to prepare the
 * Spring Data {@link BeanDefinition BeanDefinitions} for AOT processing.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @since 3.0
 */
public class ManagedTypesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private static final Log logger = LogFactory.getLog(BeanFactoryInitializationAotProcessor.class);

	@Nullable
	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {

		processManagedTypes(beanFactory);
		return null;
	}

	private void processManagedTypes(ConfigurableListableBeanFactory beanFactory) {

		if (beanFactory instanceof BeanDefinitionRegistry registry) {

			for (String beanName : beanFactory.getBeanNamesForType(ManagedTypes.class)) {
				postProcessManagedTypes(beanFactory, registry, beanName);
			}
		}
	}

	private void postProcessManagedTypes(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			String beanName) {

		BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);

		if (hasConstructorArguments(beanDefinition)) {

			ValueHolder argumentValue = beanDefinition.getConstructorArgumentValues().getArgumentValue(0, null, null, null);

			if (argumentValue.getValue()instanceof Supplier supplier) {

				if (logger.isDebugEnabled()) {
					logger.info(String.format("Replacing ManagedType bean definition %s.", beanName));
				}

				Object value = potentiallyWrapToIterable(supplier.get());

				BeanDefinition beanDefinitionReplacement = newManagedTypeBeanDefinition(beanDefinition.getBeanClassName(),
						value);

				registry.removeBeanDefinition(beanName);
				registry.registerBeanDefinition(beanName, beanDefinitionReplacement);
			}
		}
	}

	private static Object potentiallyWrapToIterable(Object value) {

		if (ObjectUtils.isArray(value)) {
			return CollectionUtils.arrayToList(value);
		}

		if (value instanceof Iterable<?>) {
			return value;
		}

		return Collections.singleton(value);
	}

	private boolean hasConstructorArguments(BeanDefinition beanDefinition) {
		return !beanDefinition.getConstructorArgumentValues().isEmpty();
	}

	private BeanDefinition newManagedTypeBeanDefinition(String managedTypesClassName, Object constructorArgumentValue) {

		return BeanDefinitionBuilder.rootBeanDefinition(managedTypesClassName) //
				.setFactoryMethod("fromIterable") //
				.addConstructorArgValue(constructorArgumentValue) //
				.getBeanDefinition();
	}
}
