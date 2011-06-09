/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mapping;

import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.util.TypeInformation;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class MappingMetadataTests {

	SampleMappingContext ctx;

	@Before
	public void setup() {
		ctx = new SampleMappingContext();
	}


	@Test
	public void testPojoWithId() {
		PersistentEntity<?, SampleProperty> person = ctx.addPersistentEntity(PersonWithId.class);
		assertNotNull(person.getIdProperty());
		assertEquals(String.class, person.getIdProperty().getType());
	}

	@Test
	public void testAssociations() {
		PersistentEntity<?, SampleProperty> person = ctx.addPersistentEntity(PersonWithChildren.class);
		assertNotNull(person.getAssociations());

		for (Association<SampleProperty> association : person.getAssociations()) {
			assertEquals(Child.class, association.getInverse().getComponentType());
		}
	}

	public interface SampleProperty extends PersistentProperty<SampleProperty> {
	}

	public class SampleMappingContext extends AbstractMappingContext<MutablePersistentEntity<?, SampleProperty>, SampleProperty> {

		@Override
		protected <T> MutablePersistentEntity<?, SampleProperty> createPersistentEntity(
				TypeInformation<T> typeInformation) {

			return new BasicPersistentEntity<T, MappingMetadataTests.SampleProperty>(typeInformation);
		}

		@Override
		protected SampleProperty createPersistentProperty(Field field,
																											PropertyDescriptor descriptor,
																											MutablePersistentEntity<?, SampleProperty> owner,
																											SimpleTypeHolder simpleTypeHolder) {
			return new SamplePropertyImpl(field, descriptor, owner, simpleTypeHolder);
		}
	}

	public class SamplePropertyImpl extends AnnotationBasedPersistentProperty<SampleProperty> implements SampleProperty {

		public SamplePropertyImpl(Field field,
															PropertyDescriptor propertyDescriptor,
															PersistentEntity<?, SampleProperty> owner,
															SimpleTypeHolder simpleTypeHolder) {

			super(field, propertyDescriptor, owner, simpleTypeHolder);
		}

		@Override
		protected Association<SampleProperty> createAssociation() {

			return new Association<MappingMetadataTests.SampleProperty>(this, null);
		}
	}
}