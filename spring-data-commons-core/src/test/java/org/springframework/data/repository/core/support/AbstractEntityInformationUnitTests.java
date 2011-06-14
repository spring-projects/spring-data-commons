/*
 * Copyright 2011 the original author or authors.
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
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.AbstractEntityInformation;


/**
 * Unit tests for {@link AbstractEntityInformation}.
 *
 * @author Oliver Gierke
 */
public class AbstractEntityInformationUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullDomainClass() throws Exception {

		new DummyAbstractEntityInformation(null);
	}


	@Test
	public void considersEntityNewIfGetIdReturnsNull() throws Exception {

		EntityInformation<Object, Serializable> metadata =
				new DummyAbstractEntityInformation(Object.class);
		assertThat(metadata.isNew(null), is(true));
		assertThat(metadata.isNew(new Object()), is(false));
	}

	private static class DummyAbstractEntityInformation extends
			AbstractEntityInformation<Object, Serializable> {

		public DummyAbstractEntityInformation(Class<Object> domainClass) {

			super(domainClass);
		}


		/*
						 * (non-Javadoc)
						 *
						 * @see
						 * org.springframework.data.repository.support.EntityMetadata#getId(
						 * java.lang.Object)
						 */
		public Serializable getId(Object entity) {

			return entity == null ? null : entity.toString();
		}

		/* (non-Javadoc)
						 * @see org.springframework.data.repository.support.EntityInformation#getIdType()
						 */
		public Class<Serializable> getIdType() {

			return Serializable.class;
		}
	}
}
