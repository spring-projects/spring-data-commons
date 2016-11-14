/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractEntityInformation}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class AbstractEntityInformationUnitTests {

	@Rule public ExpectedException exception = ExpectedException.none();

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullDomainClass() throws Exception {

		new DummyEntityInformation<Object>(null);
	}

	@Test
	public void considersEntityNewIfGetIdReturnsNull() throws Exception {

		EntityInformation<Object, Serializable> metadata = new DummyEntityInformation<Object>(Object.class);
		assertThat(metadata.isNew(null)).isTrue();
		assertThat(metadata.isNew(new Object())).isFalse();
	}

	/**
	 * @see DATACMNS-357
	 */
	@Test
	public void detectsNewStateForPrimitiveIds() {

		FooEn<PrimitiveIdEntity, Serializable> fooEn = new FooEn<PrimitiveIdEntity, Serializable>(PrimitiveIdEntity.class);

		PrimitiveIdEntity entity = new PrimitiveIdEntity();
		assertThat(fooEn.isNew(entity)).isTrue();

		entity.id = 5L;
		assertThat(fooEn.isNew(entity)).isFalse();
	}

	/**
	 * @see DATACMNS-357
	 */
	@Test
	public void detectsNewStateForPrimitiveWrapperIds() {

		FooEn<PrimitiveWrapperIdEntity, Serializable> fooEn = new FooEn<PrimitiveWrapperIdEntity, Serializable>(
				PrimitiveWrapperIdEntity.class);

		PrimitiveWrapperIdEntity entity = new PrimitiveWrapperIdEntity();
		assertThat(fooEn.isNew(entity)).isTrue();

		entity.id = 5L;
		assertThat(fooEn.isNew(entity)).isFalse();
	}

	/**
	 * @see DATACMNS-357
	 */
	@Test
	public void rejectsUnsupportedPrimitiveIdType() {

		FooEn<UnsupportedPrimitiveIdEntity, ?> information = new FooEn<UnsupportedPrimitiveIdEntity, Boolean>(
				UnsupportedPrimitiveIdEntity.class);

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(boolean.class.getName());
		information.isNew(new UnsupportedPrimitiveIdEntity());
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

	static class FooEn<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

		private final Class<T> type;

		private FooEn(Class<T> type) {
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
