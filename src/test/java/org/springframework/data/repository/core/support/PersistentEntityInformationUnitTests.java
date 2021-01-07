/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Unit tests for {@link PersistentEntityInformation}.
 *
 * @author Oliver Gierke
 */
class PersistentEntityInformationUnitTests {

	@Test // DATACMNS-480
	void obtainsIdAndIdTypeInformationFromPersistentEntity() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Sample.class);

		EntityInformation<Object, Long> information = new PersistentEntityInformation<>(entity);
		assertThat(information.getIdType()).isEqualTo(Long.class);

		Sample sample = new Sample();
		sample.id = 5L;

		assertThat(information.getId(sample)).isEqualTo(5L);
	}

	@Test // DATACMNS-596
	void returnsNullIfNoIdPropertyPresent() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context
				.getRequiredPersistentEntity(EntityWithoutId.class);

		PersistentEntityInformation<Object, Serializable> information = new PersistentEntityInformation<>(
				entity);
		assertThat(information.getId(new EntityWithoutId())).isNull();
	}

	static class Sample {

		@Id Long id;
	}

	static class EntityWithoutId {

	}
}
