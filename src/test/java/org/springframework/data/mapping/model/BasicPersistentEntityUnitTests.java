/*
 * Copyright 2011-2015 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentEntitySpec;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for {@link BasicPersistentEntity}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicPersistentEntityUnitTests<T extends PersistentProperty<T>> {

	@Rule public ExpectedException exception = ExpectedException.none();

	@Mock T property, anotherProperty;

	@Test
	public void assertInvariants() {
		PersistentEntitySpec.assertInvariants(createEntity(Person.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeInformation() {
		new BasicPersistentEntity<Object, T>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProperty() {
		createEntity(Person.class, null).addPersistentProperty(null);
	}

	@Test
	public void returnsNullForTypeAliasIfNoneConfigured() {

		PersistentEntity<Entity, T> entity = createEntity(Entity.class);
		assertThat(entity.getTypeAlias(), is(nullValue()));
	}

	@Test
	public void returnsTypeAliasIfAnnotated() {

		PersistentEntity<AliasedEntity, T> entity = createEntity(AliasedEntity.class);
		assertThat(entity.getTypeAlias(), is((Object) "foo"));
	}

	/**
	 * @see DATACMNS-50
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void considersComparatorForPropertyOrder() {

		BasicPersistentEntity<Person, T> entity = createEntity(Person.class, new Comparator<T>() {
			public int compare(T o1, T o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		T lastName = (T) Mockito.mock(PersistentProperty.class);
		when(lastName.getName()).thenReturn("lastName");

		T firstName = (T) Mockito.mock(PersistentProperty.class);
		when(firstName.getName()).thenReturn("firstName");

		T ssn = (T) Mockito.mock(PersistentProperty.class);
		when(ssn.getName()).thenReturn("ssn");

		entity.addPersistentProperty(lastName);
		entity.addPersistentProperty(firstName);
		entity.addPersistentProperty(ssn);
		entity.verify();

		List<T> properties = (List<T>) ReflectionTestUtils.getField(entity, "properties");

		assertThat(properties.size(), is(3));
		Iterator<T> iterator = properties.iterator();
		assertThat(iterator.next(), is(entity.getPersistentProperty("firstName")));
		assertThat(iterator.next(), is(entity.getPersistentProperty("lastName")));
		assertThat(iterator.next(), is(entity.getPersistentProperty("ssn")));
	}

	/**
	 * @see DATACMNS-186
	 */
	@Test
	public void addingAndIdPropertySetsIdPropertyInternally() {

		MutablePersistentEntity<Person, T> entity = createEntity(Person.class);
		assertThat(entity.getIdProperty(), is(nullValue()));

		when(property.isIdProperty()).thenReturn(true);
		entity.addPersistentProperty(property);
		assertThat(entity.getIdProperty(), is(property));
	}

	/**
	 * @see DATACMNS-186
	 */
	@Test
	public void rejectsIdPropertyIfAlreadySet() {

		MutablePersistentEntity<Person, T> entity = createEntity(Person.class);

		when(property.isIdProperty()).thenReturn(true);
		when(anotherProperty.isIdProperty()).thenReturn(true);

		entity.addPersistentProperty(property);
		exception.expect(MappingException.class);
		entity.addPersistentProperty(anotherProperty);
	}

	/**
	 * @see DATACMNS-365
	 */
	@Test
	public void detectsPropertyWithAnnotation() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Entity.class);

		PersistentProperty<?> property = entity.getPersistentProperty(LastModifiedBy.class);
		assertThat(property, is(notNullValue()));
		assertThat(property.getName(), is("field"));

		property = entity.getPersistentProperty(CreatedBy.class);
		assertThat(property, is(notNullValue()));
		assertThat(property.getName(), is("property"));

		assertThat(entity.getPersistentProperty(CreatedDate.class), is(nullValue()));
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void returnsBeanWrapperForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Entity.class);

		Entity value = new Entity();
		PersistentPropertyAccessor accessor = entity.getPropertyAccessor(value);

		assertThat(accessor, is(instanceOf(BeanWrapper.class)));
		assertThat(accessor.getBean(), is((Object) value));
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullBeanForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Entity.class);

		entity.getPropertyAccessor(null);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonMatchingBeanForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Entity.class);

		entity.getPropertyAccessor("foo");
	}

	/**
	 * @see DATACMNS-597
	 */
	@Test
	public void supportsSubtypeInstancesOnPropertyAccessorLookup() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Entity.class);

		assertThat(entity.getPropertyAccessor(new Subtype()), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-866
	 */
	@Test
	public void invalidBeanAccessCreatesDescriptiveErrorMessage() {

		PersistentEntity<Entity, T> entity = createEntity(Entity.class);

		exception.expectMessage(Entity.class.getName());
		exception.expectMessage(Object.class.getName());

		entity.getPropertyAccessor(new Object());
	}

	/**
	 * @see DATACMNS-934
	 */
	@Test
	public void doesNotThrowAnExceptionForNullAssociation() {

		BasicPersistentEntity<Entity, T> entity = createEntity(Entity.class);
		entity.addAssociation(null);

		entity.doWithAssociations(new SimpleAssociationHandler() {

			@Override
			public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {
				Assert.fail("Expected the method to never be called!");
			}
		});
	}

	private <S> BasicPersistentEntity<S, T> createEntity(Class<S> type) {
		return createEntity(type, null);
	}

	private <S> BasicPersistentEntity<S, T> createEntity(Class<S> type, Comparator<T> comparator) {
		return new BasicPersistentEntity<S, T>(ClassTypeInformation.from(type), comparator);
	}

	@TypeAlias("foo")
	static class AliasedEntity {

	}

	static class Entity {

		@LastModifiedBy String field;
		String property;

		/**
		 * @return the property
		 */
		@CreatedBy
		public String getProperty() {
			return property;
		}
	}

	static class Subtype extends Entity {}
}
