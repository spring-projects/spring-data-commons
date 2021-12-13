/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.convert;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * {@link PropertyValueConverterFactory} implementation that leverages the {@link BeanFactory} to create the desired
 * {@link PropertyValueConverter}. This allows the {@link PropertyValueConverter} to make use of DI.
 *
 * @author Christoph Strobl
 */
public class BeanFactoryAwarePropertyValueConverterFactory implements PropertyValueConverterFactory, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public <S, T> PropertyValueConverter<S, T> getConverter(Class<? extends PropertyValueConverter<S, T>> converterType) {

		Assert.state(beanFactory != null, "BeanFactory must not be null. Did you forget to set it!");
		Assert.notNull(converterType, "ConverterType must not be null!");

		if (beanFactory instanceof AutowireCapableBeanFactory) {
			return (PropertyValueConverter<S, T>) ((AutowireCapableBeanFactory) beanFactory).createBean(converterType,
					AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
		}

		return beanFactory.getBean(converterType);
	}
}
