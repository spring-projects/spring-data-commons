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
package org.springframework.data.aot;

import static org.assertj.core.api.Assertions.*;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.core.TypeInformation;

/**
 * Unit tests for {@link AotMappingContext}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class AotMappingContextUnitTests {

	AotMappingContext context = new AotMappingContext();

	@Test // GH-2595
	void obtainEntityWithReference() {
		context.getPersistentEntity(TypeInformation.of(DemoEntity.class));
	}

	@Test // GH-2595
	void doesNotCreateEntityForBitSet() {

		assertThat(context.getPersistentEntity(EntityWithBitSet.class)).isNotNull();
		assertThat(context.getPersistentEntity(BitSet.class)).isNull();
	}

	@Test // GH-2595
	void doesNotCreateEntityForJavaxReference() {

		assertThat(context.getPersistentEntity(EntityWithReference.class)).isNotNull();
		assertThat(context.getPersistentEntity(javax.naming.Reference.class)).isNull();
	}

	@Test // GH-3361
	void doesNotContributeGeneratedAccessorForUnsupportedType() {

		assertThatNoException().isThrownBy(() -> {
			context.contribute(ConcretePerson.class);
		});
	}

	static class DemoEntity {

		@Id String id;
		String name;

		@Reference ReferencedEntity referencedEntity;
	}

	static class ReferencedEntity {
		@Id String id;
	}

	static class EntityWithBitSet {

		@Id String id;
		BitSet bitset;
	}

	static class EntityWithReference {

		@Id String id;
		javax.naming.Reference reference;
	}

	static abstract class AbstractPerson {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class ConcretePerson extends AbstractPerson {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
