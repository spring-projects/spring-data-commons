/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.repository.sample;

import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.support.Repositories;

@Configuration
public class SampleConfiguration {

	@Autowired ApplicationContext context;

	@Bean
	Repositories repositories() {
		return new Repositories(context);
	}

	@Bean
	RepositoryFactoryBeanSupport<Repository<User, Long>, User, Long> userRepositoryFactory() {

		return new DummyRepositoryFactoryBean<>(UserRepository.class);
	}

	@Bean
	RepositoryFactoryBeanSupport<Repository<Product, Long>, Product, Long> productRepositoryFactory(
			ProductRepository productRepository) {

		var factory = new DummyRepositoryFactoryBean<Repository<Product, Long>, Product, Long>(
				ProductRepository.class);
		factory.setCustomImplementation(productRepository);

		return factory;
	}

	@Bean
	ProductRepository productRepository() {
		return mock(ProductRepository.class);
	}
}
