/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link Repositories}.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RepositoriesIntegrationTests {

	@Configuration
	static class Config {

		@Autowired ApplicationContext context;

		@Bean
		public Repositories repositories() {
			return new Repositories(context);
		}

		@Bean
		public RepositoryFactoryBeanSupport<Repository<User, Long>, User, Long> userRepositoryFactory() {

			DummyRepositoryFactoryBean<Repository<User, Long>, User, Long> factory = new DummyRepositoryFactoryBean<Repository<User, Long>, User, Long>();
			factory.setRepositoryInterface(UserRepository.class);

			return factory;
		}

		@Bean
		public RepositoryFactoryBeanSupport<Repository<Product, Long>, Product, Long> productRepositoryFactory() {

			DummyRepositoryFactoryBean<Repository<Product, Long>, Product, Long> factory = new DummyRepositoryFactoryBean<Repository<Product, Long>, Product, Long>();
			factory.setRepositoryInterface(ProductRepository.class);

			return factory;
		}
	}

	@Autowired Repositories repositories;

	@Test
	public void detectsRepositories() {

		assertThat(repositories, is(notNullValue()));
		assertThat(repositories.hasRepositoryFor(User.class), is(true));
		assertThat(repositories.hasRepositoryFor(Product.class), is(true));
	}

	@Test
	public void createsCrudInvokersCorrectly() {

		assertThat(repositories, is(notNullValue()));
		assertThat(repositories.getCrudInvoker(User.class), is(instanceOf(CrudRepositoryInvoker.class)));
		assertThat(repositories.getCrudInvoker(Product.class), is(instanceOf(ReflectionRepositoryInvoker.class)));
	}

	/**
	 * @see DATACMNS-376
	 */
	@Test
	public void returnsPersistentEntityForProxiedClass() {

		User user = mock(User.class);
		assertThat(repositories.getPersistentEntity(user.getClass()), is(notNullValue()));
	}

	static class User {

	}

	interface UserRepository extends CrudRepository<User, Long> {

	}

	static class Product {}

	interface ProductRepository extends Repository<Product, Long> {

		Product findOne(Long id);

		Product save(Product product);
	}
}
