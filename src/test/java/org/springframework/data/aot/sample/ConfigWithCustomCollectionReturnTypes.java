/*
 * Copyright 2025-present the original author or authors.
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

import org.eclipse.collections.api.list.ImmutableList;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.config.EnableRepositories;

import io.vavr.collection.List;
import io.vavr.collection.Seq;

/**
 * @author hippi345
 */
@Configuration
@EnableRepositories(considerNestedRepositories = true, includeFilters = {
		@Filter(type = FilterType.REGEX, pattern = ".*CustomCollectionReturnTypesRepository") })
public class ConfigWithCustomCollectionReturnTypes {

	public interface CustomCollectionReturnTypesRepository extends PagingAndSortingRepository<Person, Long> {

		List<Person> findByFirstname(String firstname);

		Seq<Person> findAllByLastname(String lastname);

		ImmutableList<Person> findAllEclipse();
	}

	public static class Person {
		String firstname;
		String lastname;
	}
}
