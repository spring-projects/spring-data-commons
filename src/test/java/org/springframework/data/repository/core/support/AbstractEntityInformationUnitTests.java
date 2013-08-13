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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;

import org.junit.Test;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Unit tests for {@link AbstractEntityInformation}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class AbstractEntityInformationUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullDomainClass() throws Exception {

		new DummyEntityInformation<Object>(null);
	}

	@Test
	public void considersEntityNewIfGetIdReturnsNull() throws Exception {

		EntityInformation<Object, Serializable> metadata = new DummyEntityInformation<Object>(Object.class);
		assertThat(metadata.isNew(null), is(true));
		assertThat(metadata.isNew(new Object()), is(false));
	}

	// See DATACMNS-357
	@Test
	public void considersEntityNewIfGetIdReturnsNullAndIdTypeIsPrimitiveNumber() throws Exception {

		EntityInformation<LongIdEntity, Long> metadata1 =
				new DummyEntityAndIdInformation<LongIdEntity, Long>(LongIdEntity.class, long.class);
		assertThat(metadata1.isNew(null), is(true));
		assertThat(metadata1.isNew(new LongIdEntity(null)), is(true));
		assertThat(metadata1.isNew(new LongIdEntity(-1L)), is(true));
		assertThat(metadata1.isNew(new LongIdEntity(0L)), is(true));
		assertThat(metadata1.isNew(new LongIdEntity(1L)), is(false));

		EntityInformation<IntIdEntity, Integer> metadata2 =
				new DummyEntityAndIdInformation<IntIdEntity, Integer>(IntIdEntity.class, int.class);
		assertThat(metadata2.isNew(null), is(true));
		assertThat(metadata2.isNew(new IntIdEntity(null)), is(true));
		assertThat(metadata2.isNew(new IntIdEntity(-1)), is(true));
		assertThat(metadata2.isNew(new IntIdEntity(0)), is(true));
		assertThat(metadata2.isNew(new IntIdEntity(1)), is(false));
	}

	// See DATACMNS-357
	@Test
	public void considersEntityNewIfGetIdReturnsNullAndIdTypeIsPrimitiveWrapperNumber() throws Exception {

		EntityInformation<LongIdEntity, Long> metadata1 =
				new DummyEntityAndIdInformation<LongIdEntity, Long>(LongIdEntity.class, Long.class);
		assertThat(metadata1.isNew(null), is(true));
		assertThat(metadata1.isNew(new LongIdEntity(null)), is(true));
		assertThat(metadata1.isNew(new LongIdEntity(-1L)), is(false));
		assertThat(metadata1.isNew(new LongIdEntity(0L)), is(false));
		assertThat(metadata1.isNew(new LongIdEntity(1L)), is(false));

		EntityInformation<IntIdEntity, Integer> metadata2 =
				new DummyEntityAndIdInformation<IntIdEntity, Integer>(IntIdEntity.class, Integer.class);
		assertThat(metadata2.isNew(null), is(true));
		assertThat(metadata2.isNew(new IntIdEntity(null)), is(true));
		assertThat(metadata2.isNew(new IntIdEntity(-1)), is(false));
		assertThat(metadata2.isNew(new IntIdEntity(0)), is(false));
		assertThat(metadata2.isNew(new IntIdEntity(1)), is(false));
	}

	private static abstract class NumberIdEntity<T extends Number> implements Persistable<T> {

		private final T id;

		public NumberIdEntity(T id) {
			this.id = id;
		}

		@Override
		public T getId() {
			return this.id;
		}

		@Override
		public boolean isNew() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class LongIdEntity extends NumberIdEntity<Long> {

		public LongIdEntity(Long id) {
			super(id);
		}
	}

	private static final class IntIdEntity extends NumberIdEntity<Integer> {

		public IntIdEntity(Integer id) {
			super(id);
		}
	}
}
