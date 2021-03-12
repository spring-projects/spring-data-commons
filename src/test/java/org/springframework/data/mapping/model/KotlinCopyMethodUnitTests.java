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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit tests for {@link KotlinCopyMethod}.
 *
 * @author Mark Paluch
 */
class KotlinCopyMethodUnitTests {

	SampleMappingContext mappingContext = new SampleMappingContext();

	@Test // #2324
	void shouldLookupPrimaryConstructor() {

		Optional<KotlinCopyMethod> optional = KotlinCopyMethod.findCopyMethod(DataClassKt.class);

		assertThat(optional).hasValueSatisfying(actual -> {
			// $this, 1 component, 1 defaulting mask, 1 Object marker
			assertThat(actual.getSyntheticCopyMethod().getParameterCount()).isEqualTo(4);

			// $this, 1 component
			assertThat(actual.getCopyFunction().getParameters()).hasSize(2);
		});
	}

	@Test // #2324
	void shouldLookupPrimaryConstructorWhenTwoCopyMethodsArePresent() {

		Optional<KotlinCopyMethod> optional = KotlinCopyMethod.findCopyMethod(WithCustomCopyMethod.class);

		assertThat(optional).hasValueSatisfying(actual -> {
			// $this, 7 components, 1 defaulting mask, 1 Object marker
			assertThat(actual.getSyntheticCopyMethod().getParameterCount()).isEqualTo(10);

			// $this, 7 components
			assertThat(actual.getCopyFunction().getParameters()).hasSize(8);

			assertThat(
					actual.shouldUsePublicCopyMethod(mappingContext.getRequiredPersistentEntity(WithCustomCopyMethod.class)))
							.isFalse();
		});
	}

	@Test // #2324
	void shouldUsePublicKotlinMethodForSinglePropertyEntities() {

		KotlinCopyMethod copyMethod = KotlinCopyMethod.findCopyMethod(DataClassKt.class).get();

		assertThat(copyMethod.shouldUsePublicCopyMethod(mappingContext.getRequiredPersistentEntity(DataClassKt.class)))
				.isTrue();
	}

	@Test // #2324
	void shouldDetermineCopyMethodForParametrizedType() {

		Optional<KotlinCopyMethod> copyMethod = KotlinCopyMethod.findCopyMethod(ImmutableKotlinPerson.class);

		assertThat(copyMethod).isPresent();
	}
}
