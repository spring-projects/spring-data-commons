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

		@Autowired
		ApplicationContext context;

		@Bean
		public Repositories repositories() {
			return new Repositories(context);
		}

		@Bean
		public RepositoryFactoryBeanSupport<Repository<User, Long>, User, Long> repositoryFactory() {

			DummyRepositoryFactoryBean<Repository<User, Long>, User, Long> factory = new DummyRepositoryFactoryBean<Repository<User, Long>, User, Long>();
			factory.setRepositoryInterface(UserRepository.class);

			return factory;
		}
	}

	@Autowired
	Repositories repositories;

	@Test
	public void foo() {
		assertThat(repositories, is(notNullValue()));
		assertThat(repositories.hasRepositoryFor(User.class), is(true));
	}

	static class User {

	}

	interface UserRepository extends CrudRepository<User, Long> {

	}
}
