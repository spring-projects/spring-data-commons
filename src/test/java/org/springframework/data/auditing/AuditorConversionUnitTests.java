/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.data.auditing;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit tests for converting auditor values to the type of the audited property.
 *
 * @author Alpha
 */
class AuditorConversionUnitTests {

	@Test // GH-3437
	void convertsAuditorValuesForReflectionBasedAuditing() {

		var sample = new ReflectionSample();
		var wrapper = new DefaultAuditableBeanWrapperFactory().getBeanWrapperFor(sample).orElseThrow();

		wrapper.setCreatedBy("42");
		wrapper.setLastModifiedBy(21L);

		assertThat(sample.createdBy).isEqualTo(42L);
		assertThat(sample.lastModifiedBy).isEqualTo("21");
	}

	@Test // GH-3437
	void convertsAuditorValuesForMappingBasedAuditing() {

		var context = new SampleMappingContext();
		context.getPersistentEntity(MappingSample.class);
		var entities = PersistentEntities.of(context);
		var sample = new MappingSample();
		var wrapper = new MappingAuditableBeanWrapperFactory(entities).getBeanWrapperFor(sample).orElseThrow();

		wrapper.setCreatedBy("42");
		wrapper.setLastModifiedBy(21L);

		assertThat(sample.createdBy).isEqualTo(42L);
		assertThat(sample.lastModifiedBy).isEqualTo("21");
	}

	static class ReflectionSample {

		@CreatedBy Long createdBy;
		@LastModifiedBy String lastModifiedBy;
	}

	static class MappingSample {

		@CreatedBy Long createdBy;
		@LastModifiedBy String lastModifiedBy;
	}
}
