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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Integration test for {@link DomainClassConverter}.
 *
 * @author Oliver Gierke
 * @author Alessandro Nistico
 */
@ExtendWith(MockitoExtension.class)
class DomainClassConverterIntegrationTests {

	@Mock @SuppressWarnings("rawtypes") RepositoryFactoryBeanSupport factory;
	@Mock PersonRepository repository;
	@Mock RepositoryInformation information;

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void findsRepositoryFactories() {

		var beanFactory = new DefaultListableBeanFactory() {
			@Override
			protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {
				return beanName.equals("repoFactory") ? new BeanWrapperImpl(factory) : super.instantiateBean(beanName, mbd);
			}
		};

		beanFactory.registerBeanDefinition("postProcessor", new RootBeanDefinition(PredictingProcessor.class));
		beanFactory.registerBeanDefinition("repoFactory", new RootBeanDefinition(RepositoryFactoryBeanSupport.class));

		doReturn(TypeInformation.of(Person.class)).when(information).getDomainTypeInformation();
		doReturn(TypeInformation.of(Serializable.class)).when(information).getIdTypeInformation();
		doCallRealMethod().when(information).getDomainType();
		doReturn(PersonRepository.class).when(factory).getObjectType();
		doReturn(information).when(factory).getRepositoryInformation();

		var context = new GenericApplicationContext(beanFactory);
		context.refresh();
		assertThat(context.getBeansOfType(RepositoryFactoryInformation.class).values()).hasSize(1);

		var converter = new DomainClassConverter(new DefaultConversionService());
		converter.setApplicationContext(context);

		assertThat(converter.matches(TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Person.class))).isTrue();
	}

	static class Person {

	}

	static interface PersonRepository extends CrudRepository<Person, Serializable> {

	}

	static class PredictingProcessor implements SmartInstantiationAwareBeanPostProcessor {

		@Override
		public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
			return RepositoryFactoryBeanSupport.class.equals(beanClass) ? PersonRepository.class : null;
		}
	}
}
