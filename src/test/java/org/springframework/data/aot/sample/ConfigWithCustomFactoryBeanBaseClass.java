/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.aot.sample;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.aot.sample.ConfigWithCustomFactoryBeanBaseClass.MyFixedRepoFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.EnableRepositories;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;

/**
 * @author Christoph Strobl
 */
@Configuration
@EnableRepositories(repositoryFactoryBeanClass = MyFixedRepoFactory.class, considerNestedRepositories = true,
		includeFilters = { @Filter(type = FilterType.REGEX, pattern = ".*FixedFactoryRepository") })
public class ConfigWithCustomFactoryBeanBaseClass {

	public interface FixedFactoryRepository extends CrudRepository<Person, String> {

	}

	public static class Person {

		Address address;

	}

	public static class Address {
		String street;
	}

	public static class MyFixedRepoFactory extends DummyRepositoryFactoryBean<FixedFactoryRepository, Person, String> {

		public MyFixedRepoFactory(Class<? extends FixedFactoryRepository> repositoryInterface) {
			super(FixedFactoryRepository.class);
		}
	}
}
