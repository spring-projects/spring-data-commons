/*
 * Copyright 2023-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.model.AbstractPersistentPropertyUnitTests.SamplePersistentProperty;

/**
 * Unit tests for {@link InstanceCreatorMetadata}.
 *
 * @author Mark Paluch
 */
class InstanceCreatorMetadataDiscovererUnitTests {

	@Test
	void shouldDiscoverConstructorForKotlinValueType() {

		InstanceCreatorMetadata<SamplePersistentProperty> metadata = InstanceCreatorMetadataDiscoverer
				.discover(new BasicPersistentEntity<Object, SamplePersistentProperty>(
						(TypeInformation) TypeInformation.of(MyNullableValueClass.class)));

		assertThat(metadata).isNotNull();
		assertThat(metadata.hasParameters()).isTrue();
		assertThat(metadata.getParameters().get(0).getName()).isEqualTo("id");
	}
}
