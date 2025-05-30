/*
 * Copyright 2019-2025 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.EnableRepositories;
import org.springframework.stereotype.Component;

/**
 * @author Christoph Strobl
 */
@Configuration
@EnableRepositories(considerNestedRepositories = true,
		includeFilters = { @Filter(type = FilterType.REGEX, pattern = ".*RepositoryWithFragments") })
public class ConfigWithFragments {

	public interface RepositoryWithFragments
			extends Repository<Person, String>, CustomImplInterface1, CustomImplInterface2 {

	}

	public interface CustomImplInterface1 {

		List<Customer> findMyCustomer();
	}

	@Component
	public static class CustomImplInterface1Impl implements CustomImplInterface1 {

		@Override
		public List<Customer> findMyCustomer() {
			return Collections.emptyList();
		}
	}

	public interface CustomImplInterface2 {

	}

	@Component
	public static class CustomImplInterface2Impl implements CustomImplInterface2 {

	}

	public static class Person {

		Address address;

	}

	public static class Address {
		String street;
	}

	public static class Customer {

	}
}
