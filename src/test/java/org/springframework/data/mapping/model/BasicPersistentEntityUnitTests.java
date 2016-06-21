/*
 * Copyright 2011-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentEntitySpec;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.Person;
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
		assertThat(entity.getTypeAlias()).isEqualTo(Alias.NONE);
	}

	@Test
	public void returnsTypeAliasIfAnnotated() {

		PersistentEntity<AliasedEntity, T> entity = createEntity(AliasedEntity.class);
		assertThat(entity.getTypeAlias()).isEqualTo(Alias.of("foo"));
	}

	/**
	 * @see DATACMNS-50
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void considersComparatorForPropertyOrder() {

		BasicPersistentEntity<Person, T> entity = createEntity(Person.class,
				Comparator.comparing(PersistentProperty::getName));

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

		assertThat(properties).hasSize(3);
		Iterator<T> iterator = properties.iterator();

		assertThat(entity.getPersistentProperty("firstName")).hasValue(iterator.next());
		assertThat(entity.getPersistentProperty("lastName")).hasValue(iterator.next());
		assertThat(entity.getPersistentProperty("ssn")).hasValue(iterator.next());
	}

	/**
	 * @see DATACMNS-186
	 */
	@Test
	public void addingAndIdPropertySetsIdPropertyInternally() {

		MutablePersistentEntity<Person, T> entity = createEntity(Person.class);
		assertThat(entity.getIdProperty()).isNotPresent();

		when(property.isIdProperty()).thenReturn(true);
		entity.addPersistentProperty(property);
		assertThat(entity.getIdProperty()).hasValue(property);
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
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		Optional<SamplePersistentProperty> property = entity.getPersistentProperty(LastModifiedBy.class);

		assertThat(property).hasValueSatisfying(it -> {
			assertThat(it.getName()).isEqualTo("field");
		});

		property = entity.getPersistentProperty(CreatedBy.class);

		assertThat(property).hasValueSatisfying(it -> {
			assertThat(it.getName()).isEqualTo("property");
		});

		assertThat(entity.getPersistentProperty(CreatedDate.class)).isNotPresent();
	}

	/**
	 * @see DATACMNS-809
	 */
	@Test
	public void returnsGeneratedPropertyAccessorForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		Entity value = new Entity();
		PersistentPropertyAccessor accessor = entity.getPropertyAccessor(value);

		assertThat(accessor).isNotInstanceOf(BeanWrapper.class);
		assertThat(accessor.getClass().getName()).contains("_Accessor_");
		assertThat(accessor.getBean()).isEqualTo(value);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullBeanForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		entity.getPropertyAccessor(null);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonMatchingBeanForPropertyAccessor() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		entity.getPropertyAccessor("foo");
	}

	/**
	 * @see DATACMNS-597
	 */
	@Test
	public void supportsSubtypeInstancesOnPropertyAccessorLookup() {

		SampleMappingContext context = new SampleMappingContext();
		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Entity.class);

		assertThat(entity.getPropertyAccessor(new Subtype())).isNotNull();
	}

	/**
	 * @see DATACMNS-825
	 */
	@Test
	public void returnsTypeAliasIfAnnotatedUsingComposedAnnotation() {

		PersistentEntity<AliasEntityUsingComposedAnnotation, T> entity = createEntity(
				AliasEntityUsingComposedAnnotation.class);
		assertThat(entity.getTypeAlias()).isEqualTo(Alias.of("bar"));
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

	private <S> BasicPersistentEntity<S, T> createEntity(Class<S> type) {
		return createEntity(type, null);
	}

	private <S> BasicPersistentEntity<S, T> createEntity(Class<S> type, Comparator<T> comparator) {
		return new BasicPersistentEntity<S, T>(ClassTypeInformation.from(type), Optional.ofNullable(comparator));
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

	@Retention(RetentionPolicy.RUNTIME)
	@TypeAlias("foo")
	static @interface ComposedTypeAlias {

		@AliasFor(annotation = TypeAlias.class, attribute = "value")
		String name() default "bar";
	}

	@ComposedTypeAlias
	static class AliasEntityUsingComposedAnnotation {}

	static class Subtype extends Entity {}
}
