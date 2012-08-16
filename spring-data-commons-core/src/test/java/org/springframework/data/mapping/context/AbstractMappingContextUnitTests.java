/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.AbstractPersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * Unit test for {@link AbstractMappingContext}.
 * 
 * @author Oliver Gierke
 */
public class AbstractMappingContextUnitTests {

	final SimpleTypeHolder holder = new SimpleTypeHolder();
	DummyMappingContext context;

	@Before
	public void setUp() {
		context = new DummyMappingContext();
		context.setSimpleTypeHolder(holder);
	}

	@Test
	public void doesNotTryToLookupPersistentEntityForLeafProperty() {
		PersistentPropertyPath<DummyPersistenProperty> path = context.getPersistentPropertyPath(PropertyPath.from("name",
				Person.class));
		assertThat(path, is(notNullValue()));
	}

	/**
	 * @see DATACMNS-92
	 */
	@Test(expected = MappingException.class)
	public void doesNotAddInvalidEntity() {

		try {
			context.getPersistentEntity(Unsupported.class);
		} catch (MappingException e) {
			// expected
		}

		context.getPersistentEntity(Unsupported.class);
	}

	@Test
	public void registersEntitiesOnContextRefreshedEvent() {

		ApplicationContext context = mock(ApplicationContext.class);

		DummyMappingContext mappingContext = new DummyMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
		mappingContext.setApplicationContext(context);

		verify(context, times(0)).publishEvent(Mockito.any(ApplicationEvent.class));

		mappingContext.onApplicationEvent(new ContextRefreshedEvent(context));
		verify(context, times(1)).publishEvent(Mockito.any(ApplicationEvent.class));
	}

	class Person {
		String name;
	}

	class Unsupported {

	}

	class DummyMappingContext extends
			AbstractMappingContext<BasicPersistentEntity<Object, DummyPersistenProperty>, DummyPersistenProperty> {

		@Override
		@SuppressWarnings("unchecked")
		protected <S> BasicPersistentEntity<Object, DummyPersistenProperty> createPersistentEntity(
				TypeInformation<S> typeInformation) {
			return new BasicPersistentEntity<Object, DummyPersistenProperty>((TypeInformation<Object>) typeInformation) {

				@Override
				public void verify() {
					if (holder.isSimpleType(getType()) || Unsupported.class.equals(getType())) {
						throw new MappingException("Invalid!");
					}
				}
			};
		}

		@Override
		protected DummyPersistenProperty createPersistentProperty(final Field field, final PropertyDescriptor descriptor,
				final BasicPersistentEntity<Object, DummyPersistenProperty> owner, final SimpleTypeHolder simpleTypeHolder) {

			return new DummyPersistenProperty(field, descriptor, owner, simpleTypeHolder);
		}
	}

	class DummyPersistenProperty extends AbstractPersistentProperty<DummyPersistenProperty> {

		public DummyPersistenProperty(Field field, PropertyDescriptor propertyDescriptor,
				BasicPersistentEntity<?, DummyPersistenProperty> owner, SimpleTypeHolder simpleTypeHolder) {
			super(field, propertyDescriptor, owner, simpleTypeHolder);
		}

		public boolean isIdProperty() {
			return false;
		}

		protected Association<DummyPersistenProperty> createAssociation() {
			return new Association<DummyPersistenProperty>(this, null);
		}
	}
}
