/*
 * Copyright 2011-2021 the original author or authors.
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
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractEntityInformation}.
 *
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Mark Paluch
 */
class AbstractEntityInformationUnitTests {

	@Test
	void rejectsNullDomainClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DummyEntityInformation<>(null));
	}

	@Test
	void considersEntityNewIfGetIdReturnsNull() throws Exception {

		EntityInformation<Object, Serializable> metadata = new DummyEntityInformation<>(Object.class);

		assertThat(metadata.isNew(null)).isTrue();
		assertThat(metadata.isNew(new Object())).isFalse();
	}

	@Test // DATACMNS-357
	void detectsNewStateForPrimitiveIds() {

		CustomEntityInformation<PrimitiveIdEntity, Serializable> fooEn = new CustomEntityInformation<>(
				PrimitiveIdEntity.class);

		PrimitiveIdEntity entity = new PrimitiveIdEntity();
		assertThat(fooEn.isNew(entity)).isTrue();

		entity.id = 5L;
		assertThat(fooEn.isNew(entity)).isFalse();
	}

	@Test // DATACMNS-357
	void detectsNewStateForPrimitiveWrapperIds() {

		CustomEntityInformation<PrimitiveWrapperIdEntity, Serializable> fooEn = new CustomEntityInformation<>(
				PrimitiveWrapperIdEntity.class);

		PrimitiveWrapperIdEntity entity = new PrimitiveWrapperIdEntity();
		assertThat(fooEn.isNew(entity)).isTrue();

		entity.id = 5L;
		assertThat(fooEn.isNew(entity)).isFalse();
	}

	@Test // DATACMNS-357
	void rejectsUnsupportedPrimitiveIdType() {

		CustomEntityInformation<UnsupportedPrimitiveIdEntity, ?> information = new CustomEntityInformation<UnsupportedPrimitiveIdEntity, Boolean>(
				UnsupportedPrimitiveIdEntity.class);

		assertThatIllegalArgumentException()//
				.isThrownBy(() -> information.isNew(new UnsupportedPrimitiveIdEntity()))//
				.withMessageContaining(boolean.class.getName());
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

	static class CustomEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {

		private final Class<T> type;

		private CustomEntityInformation(Class<T> type) {
			super(type);
			this.type = type;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ID getId(T entity) {
			return (ID) ReflectionTestUtils.getField(entity, "id");
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<ID> getIdType() {
			return (Class<ID>) ReflectionUtils.findField(type, "id").getType();
		}
	}
}
