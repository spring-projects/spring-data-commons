/*
 * Copyright 2022 the original author or authors.
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
import org.springframework.data.ManagedTypes;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation used to encapsulate common data infrastructure concerns
 * and preprocess the {@link ConfigurableListableBeanFactory} ahead of the AOT compilation in order to prepare
 * the Spring Data {@link BeanDefinition BeanDefinitions} for AOT processing.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory
 * @see org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution
 * @see org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor
 * @since 3.0
 */
public class SpringDataBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private static final Log logger = LogFactory.getLog(BeanFactoryInitializationAotProcessor.class);

	@Nullable
	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(
			@NonNull ConfigurableListableBeanFactory beanFactory) {

		processManagedTypes(beanFactory);
		return null;
	}

	private void processManagedTypes(@NonNull ConfigurableListableBeanFactory beanFactory) {

		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			for (String beanName : beanFactory.getBeanNamesForType(ManagedTypes.class)) {

				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);

				ValueHolder argumentValue = beanDefinition.getConstructorArgumentValues()
						.getArgumentValue(0, null, null, null);

				if (argumentValue.getValue() instanceof Supplier supplier) {

					if (logger.isDebugEnabled()) {
						logger.info(String.format("Replacing ManagedType bean definition %s.", beanName));
					}

					BeanDefinition beanDefinitionReplacement = BeanDefinitionBuilder
							.rootBeanDefinition(ManagedTypes.class)
							.setFactoryMethod("of")
							.addConstructorArgValue(supplier.get())
							.getBeanDefinition();

					registry.removeBeanDefinition(beanName);
					registry.registerBeanDefinition(beanName, beanDefinitionReplacement);
				}
			}
		}
	}
}
