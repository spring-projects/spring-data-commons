/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.repository.init;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for {@link JacksonResourceReader}.
 *
 * @author Rocco Lagrotteria
 * @since 2.7
 */
@SpringJUnitConfig(RepositoryPopulatorPersitCallbackTests.RepositoryTestConfig.class)
class RepositoryPopulatorPersitCallbackTests {

	public static class RepositoryPopulatorCallbackTest implements RepositoryPopulatorPersistCallback {

		private Person person;

		@Override
		public void afterPersist(Repositories repositories, Object entity, Object outcome) {
			if (outcome instanceof Person) {
				this.person = (Person) outcome;
			}

		}

		public Person getLastSavedEntity() {
			return person;
		}

	}

	@Configuration
	public static class RepositoryTestConfig {

		@Bean
		public RepositoryFactoryBeanSupport<Repository<Person, Long>, Person, Long> PersonRepositoryFactory() {
			return new DummyRepositoryFactoryBean<>(PersonRepository.class);

		}

		@Bean
		public RepositoryPopulatorPersistCallback callback() {
			return new RepositoryPopulatorCallbackTest();
		}

		@Bean
		public Jackson2RepositoryPopulatorFactoryBean getRespositoryPopulator() {
			Jackson2RepositoryPopulatorFactoryBean factory = new Jackson2RepositoryPopulatorFactoryBean();
			factory.setResources(new Resource[] { new ClassPathResource("data.json", getClass()) });

			return factory;
		}

	}

	@Autowired private RepositoryPopulatorCallbackTest callback;

	@Test
	void afterPersistShouldBeCalledReceivingSaveResult() throws Exception {

		assertThat(callback.getLastSavedEntity()).isNotNull();

	}
}
