/*
 * Copyright 2018-2021 the original author or authors.
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

import lombok.AllArgsConstructor;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.SampleMappingContext;

/**
 * Unit tests for {@link PersistentEntityIsNewStrategy}.
 *
 * @author Oliver Gierke
 * @soundtrack Scary Pockets - Crash Into Me (Dave Matthews Band Cover feat. Julia Nunes) -
 *             https://www.youtube.com/watch?v=syGlBNVGEqU
 */
class PersistentEntityIsNewStrategyUnitTests {

	private SampleMappingContext context = new SampleMappingContext();

	@Test // DATACMNS-133
	void detectsNewEntityForPrimitiveId() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(PrimitiveIdEntity.class);

		PrimitiveIdEntity bean = new PrimitiveIdEntity();
		assertThat(entity.isNew(bean)).isTrue();
	}

	@Test // DATACMNS-133
	void detectsNotNewEntityForPrimitiveId() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(PrimitiveIdEntity.class);

		PrimitiveIdEntity bean = new PrimitiveIdEntity();

		bean.id = 1L;
		assertThat(entity.isNew(bean)).isFalse();
	}

	@Test // DATACMNS-133
	void detectsNewEntityForWrapperId() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(PrimitiveWrapperIdEntity.class);

		PrimitiveWrapperIdEntity bean = new PrimitiveWrapperIdEntity();
		assertThat(entity.isNew(bean)).isTrue();
	}

	@Test // DATACMNS-133
	void detectsNotNewEntityForWrapperId() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(PrimitiveWrapperIdEntity.class);

		PrimitiveWrapperIdEntity bean = new PrimitiveWrapperIdEntity();

		bean.id = 0L;
		assertThat(entity.isNew(bean)).isFalse();
	}

	@Test // DATACMNS-133
	void rejectsUnsupportedIdentifierType() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(UnsupportedPrimitiveIdEntity.class);

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> PersistentEntityIsNewStrategy.of(entity)) //
				.withMessageContaining(boolean.class.getSimpleName());
	}

	@Test // DATACMNS-1333
	void discoversNewPersistableEntity() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(PersistableEntity.class);

		assertThat(entity.isNew(new PersistableEntity(false))).isFalse();
	}

	@Test // DATACMNS-1333
	void discoversNonNewPersistableEntity() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(PersistableEntity.class);

		assertThat(entity.isNew(new PersistableEntity(true))).isTrue();
	}

	@Test // DATACMNS-1333
	void prefersVersionOverIdentifier() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(VersionedEntity.class);

		VersionedEntity bean = new VersionedEntity();

		bean.id = 1L;
		assertThat(entity.isNew(bean)).isTrue();

		bean.version = 1L;
		assertThat(entity.isNew(bean)).isFalse();
	}

	@Test // DATACMNS-1333
	void considersEntityWithoutIdNew() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(NoIdEntity.class);

		assertThat(entity.isNew(new NoIdEntity())).isTrue();
	}

	static class PrimitiveIdEntity {

		@Id long id;
	}

	static class PrimitiveWrapperIdEntity {

		@Id Long id;
	}

	static class UnsupportedPrimitiveIdEntity {

		@Id boolean id;
	}

	static class VersionedEntity {

		@Version Long version;
		@Id Long id;
	}

	@AllArgsConstructor
	static class PersistableEntity implements Persistable<Long> {

		boolean isNew;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Persistable#isNew()
		 */
		@Override
		public boolean isNew() {
			return isNew;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.domain.Persistable#getId()
		 */
		@Override
		public Long getId() {
			return null;
		}
	}

	private static class NoIdEntity {}
}
