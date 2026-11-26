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
package org.springframework.data.repository.aot;

import static org.springframework.data.repository.aot.RepositoryRegistrationAotContributionAssert.assertThatContribution;

import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.data.aot.sample.ConfigWithCustomCollectionReturnTypes;
import org.springframework.data.repository.config.RepositoryRegistrationAotContribution;

import io.vavr.collection.List;
import io.vavr.collection.Seq;

/**
 * Integration tests for AOT repository support of {@link org.springframework.data.core.CustomCollections}.
 *
 * @author hippi345
 */
class CustomCollectionsAotRepositoryIntegrationTests {

	@Test // GH-3416
	void registersReflectionForVavrCollectionReturnTypes() {

		RepositoryRegistrationAotContribution contribution = computeAotConfiguration(
				ConfigWithCustomCollectionReturnTypes.class)
				.forRepository(ConfigWithCustomCollectionReturnTypes.CustomCollectionReturnTypesRepository.class);

		assertThatContribution(contribution).codeContributionSatisfies(it -> {
			it.contributesReflectionFor(List.class, Seq.class);
		});
	}

	@Test // GH-3416
	void registersReflectionForEclipseCollectionReturnTypes() {

		RepositoryRegistrationAotContribution contribution = computeAotConfiguration(
				ConfigWithCustomCollectionReturnTypes.class)
				.forRepository(ConfigWithCustomCollectionReturnTypes.CustomCollectionReturnTypesRepository.class);

		assertThatContribution(contribution).codeContributionSatisfies(it -> {
			it.contributesReflectionFor(ImmutableList.class);
		});
	}

	AotUtil.RepositoryRegistrationAotContributionBuilder computeAotConfiguration(Class<?> configuration) {
		return AotUtil.contributionFor(configuration);
	}
}
