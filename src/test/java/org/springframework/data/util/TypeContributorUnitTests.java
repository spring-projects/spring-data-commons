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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.data.aot.types.CyclicPropertiesA;
import org.springframework.data.aot.types.CyclicPropertiesB;

/**
 * Unit tests for {@link TypeContributor}.
 *
 * @author Blaz Snuderl
 */
class TypeContributorUnitTests {

	@Test
	void contributesTypesOnlyOncePerGenerationContext() {

		GenerationContext generationContext = new TestGenerationContext();

		TypeContributor.contribute(CyclicPropertiesA.class, it -> true, generationContext);

		List<TypeReference> afterFirstContribution = registeredTypes(generationContext);

		TypeContributor.contribute(CyclicPropertiesA.class, it -> true, generationContext);

		assertThat(registeredTypes(generationContext)).containsExactlyElementsOf(afterFirstContribution);
	}

	@Test
	void contributingCollectionRegistersSameHintsAsIndividualContributions() {

		GenerationContext individually = new TestGenerationContext();

		TypeContributor.contribute(CyclicPropertiesA.class, it -> true, individually);
		TypeContributor.contribute(CyclicPropertiesB.class, it -> true, individually);

		GenerationContext batched = new TestGenerationContext();

		TypeContributor.contribute(List.of(CyclicPropertiesA.class, CyclicPropertiesB.class), it -> true, batched);

		assertThat(registeredTypes(batched)).containsExactlyInAnyOrderElementsOf(registeredTypes(individually));
	}

	private static List<TypeReference> registeredTypes(GenerationContext generationContext) {
		return generationContext.getRuntimeHints().reflection().typeHints().map(it -> it.getType()).sorted().toList();
	}
}
