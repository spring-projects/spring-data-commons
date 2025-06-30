/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.repository.aot;

import static org.springframework.data.repository.aot.RepositoryRegistrationAotContributionAssert.*;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.EnableRepositories;
import org.springframework.data.repository.config.RepositoryRegistrationAotContribution;
import org.springframework.data.repository.config.RepositoryRegistrationAotProcessor;

/**
 * Integration Tests for {@link RepositoryRegistrationAotProcessor} to verify capturing generated instantiations and
 * property accessors.
 *
 * @author Mark Paluch
 */
public class GeneratedClassesCaptureIntegrationTests {

	@Test // GH-2595
	void registersGeneratedPropertyAccessorsEntityInstantiators() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = AotUtil.contributionFor(Config.class)
				.forRepository(Config.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {
					contribution.contributesReflectionFor(TypeReference.of(
							"org.springframework.data.repository.aot.GeneratedClassesCaptureIntegrationTests$Config$Person__Accessor_xj7ohs"));
					contribution.contributesReflectionFor(TypeReference.of(
							"org.springframework.data.repository.aot.GeneratedClassesCaptureIntegrationTests$Config$Person__Instantiator_xj7ohs"));

					// TODO: These should also appear
					/*
					contribution.contributesReflectionFor(TypeReference.of(
							"org.springframework.data.repository.aot.GeneratedClassesCaptureIntegrationTests$Config$Address__Accessor_xj7ohs"));
					contribution.contributesReflectionFor(TypeReference.of(
							"org.springframework.data.repository.aot.GeneratedClassesCaptureIntegrationTests$Config$Address__Instantiator_xj7ohs"));
					 */
				});
	}

	@EnableRepositories(includeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = Config.MyRepo.class) },
			basePackageClasses = Config.class, considerNestedRepositories = true)
	public class Config {

		public interface MyRepo extends CrudRepository<Person, String> {

		}

		public static class Person {

			@Nullable Address address;

		}

		public static class Address {
			String street;
		}

	}

}
