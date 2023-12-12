/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.repository.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mapping.Person;
import org.springframework.data.repository.CrudRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for @Lookup.
 *
 * @author Yanming Zhou
 */
class LookupRepositoryIntegrationTests {

	@Test
	void lookupBeansFromBeanFactory() {

		var context = new AnnotationConfigApplicationContext(Config.class);
		var repository = context.getBean(PersonRepository.class);
		assertThat(repository.getSelf()).isEqualTo(repository);
	}

	@Configuration
	@EnableRepositories(considerNestedRepositories = true, //
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PersonRepository.class))
	static class Config {}


	interface PersonRepository extends CrudRepository<Person, Long> {

		@Lookup
		PersonRepository getSelf();
	}

}
