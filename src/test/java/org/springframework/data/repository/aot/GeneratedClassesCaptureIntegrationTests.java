/*
 * Copyright 2022-present the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.aot.types.BaseEntity;
import org.springframework.data.aot.types.CyclicPropertiesA;
import org.springframework.data.aot.types.CyclicPropertiesB;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.aot.GeneratedClassesCaptureIntegrationTests.ConfigWithMultipleRepositories.Repo1;
import org.springframework.data.repository.aot.GeneratedClassesCaptureIntegrationTests.ConfigWithMultipleRepositories.Repo2;
import org.springframework.data.repository.config.EnableRepositories;
import org.springframework.data.repository.config.RepositoryRegistrationAotContribution;
import org.springframework.data.repository.config.RepositoryRegistrationAotProcessor;

/**
 * Integration Tests for {@link RepositoryRegistrationAotProcessor} to verify capturing generated instantiations and
 * property accessors.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public class GeneratedClassesCaptureIntegrationTests {

	@Test // GH-2595
	void registersGeneratedPropertyAccessorsEntityInstantiators() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = AotUtil.contributionFor(Config.class)
				.forRepository(Config.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.BaseEntity__Accessor_m5hoaa"));
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.BaseEntity__Instantiator_m5hoaa"));

					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.Address__Accessor_rf1iey"));
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.Address__Instantiator_rf1iey"));
				});
	}

	@Test // GH-2595
	@Disabled("caching issue in ClassGeneratingEntityInstantiator")
	void registersGeneratedPropertyAccessorsEntityInstantiatorsForCyclicProperties() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = AotUtil
				.contributionFor(ConfigWithCyclicReferences.class).forRepository(ConfigWithCyclicReferences.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesB__Accessor_o13htw"));
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesB__Instantiator_o13htw"));

					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesA__Accessor_o13htx"));
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesA__Instantiator_o13htx"));
				});
	}

	@Test // GH-2595
	void registersGeneratedPropertyAccessorsEntityInstantiatorsForMultipleRepositoriesReferencingEachOther() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = AotUtil
				.contributionFor(ConfigWithMultipleRepositories.class)
				.forRepositories(ConfigWithMultipleRepositories.Repo1.class, ConfigWithMultipleRepositories.Repo2.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesB__Accessor_o13htw"));
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesB__Instantiator_o13htw"));

					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesA__Accessor_o13htx"));
					contribution.contributesReflectionFor(
							TypeReference.of("org.springframework.data.aot.types.CyclicPropertiesA__Instantiator_o13htx"));
				});
	}

	@EnableRepositories(includeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = Config.MyRepo.class) },
			basePackageClasses = Config.class, considerNestedRepositories = true)
	public static class Config {

		public interface MyRepo extends CrudRepository<BaseEntity, String> {

		}
	}

	@EnableRepositories(
			includeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = ConfigWithCyclicReferences.MyRepo.class) },
			basePackageClasses = ConfigWithCyclicReferences.class, considerNestedRepositories = true)
	public static class ConfigWithCyclicReferences {

		public interface MyRepo extends CrudRepository<CyclicPropertiesA, String> {

		}
	}

	@EnableRepositories(
			includeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = { Repo1.class, Repo2.class }) },
			basePackageClasses = ConfigWithCyclicReferences.class, considerNestedRepositories = true)
	public static class ConfigWithMultipleRepositories {

		public interface Repo1 extends CrudRepository<CyclicPropertiesA, String> {

		}

		public interface Repo2 extends CrudRepository<CyclicPropertiesB, String> {

		}
	}

}
