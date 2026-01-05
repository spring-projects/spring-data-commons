/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.model.EntityInstantiators;

/**
 * Unit tests for {@link DtoInstantiatingConverter}.
 *
 * @author Mark Paluch
 */
class DtoInstantiatingConverterUnitTests {

	@Test // GH- 3104
	void dtoProjectionShouldConsiderPropertiesAndAssociations() {

		TheOtherThing ref = new TheOtherThing();
		MyAssociativeEntity entity = new MyAssociativeEntity("1", ref, "foo");
		DtoInstantiatingConverter converter = new DtoInstantiatingConverter(DtoProjection.class, new SampleMappingContext(),
				new EntityInstantiators());

		DtoProjection projection = (DtoProjection) converter.convert(entity);

		assertThat(projection.id).isEqualTo("1");
		assertThat(projection.ref).isSameAs(ref);
	}

	static class MyAssociativeEntity {

		String id;

		@Reference TheOtherThing ref;

		String somethingElse;

		public MyAssociativeEntity(String id, TheOtherThing ref, String somethingElse) {
			this.id = id;
			this.ref = ref;
			this.somethingElse = somethingElse;
		}
	}

	static class DtoProjection {

		String id;

		@Reference TheOtherThing ref;
	}

	static class TheOtherThing {
		String id;
	}

}
