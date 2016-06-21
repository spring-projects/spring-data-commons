/*
 * Copyright 2011-2014 the original author or authors.
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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.Person;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractPersistentProperty}.
 * 
 * @author Oliver Gierke
 */
public class AbstractPersistentPropertyUnitTests {

	TypeInformation<TestClassComplex> typeInfo;
	PersistentEntity<TestClassComplex, SamplePersistentProperty> entity;
	SimpleTypeHolder typeHolder;

	@Before
	public void setUp() {

		typeInfo = ClassTypeInformation.from(TestClassComplex.class);
		entity = new BasicPersistentEntity<>(typeInfo);
		typeHolder = new SimpleTypeHolder();
	}

	/**
	 * @see DATACMNS-68
	 */
	@Test
	public void discoversComponentTypeCorrectly() throws Exception {
		assertThat(getProperty(TestClassComplex.class, "testClassSet").getComponentType()).isEqualTo(Object.class);
	}

	/**
	 * @see DATACMNS-101
	 */
	@Test
	public void returnsNestedEntityTypeCorrectly() {
		assertThat(getProperty(TestClassComplex.class, "testClassSet").getPersistentEntityType()).isEmpty();
	}

	/**
	 * @see DATACMNS-132
	 */
	@Test
	public void isEntityWorksForUntypedMaps() throws Exception {
		assertThat(getProperty(TestClassComplex.class, "map").isEntity()).isFalse();
	}

	/**
	 * @see DATACMNS-132
	 */
	@Test
	public void isEntityWorksForUntypedCollection() throws Exception {
		assertThat(getProperty(TestClassComplex.class, "collection").isEntity()).isFalse();
	}

	/**
	 * @see DATACMNS-121
	 */
	@Test
	public void considersPropertiesEqualIfFieldEquals() {

		SamplePersistentProperty firstProperty = getProperty(FirstConcrete.class, "genericField");
		SamplePersistentProperty secondProperty = getProperty(SecondConcrete.class, "genericField");

		assertThat(firstProperty).isEqualTo(secondProperty);
		assertThat(firstProperty.hashCode()).isEqualTo(secondProperty.hashCode());
	}

	/**
	 * @see DATACMNS-180
	 */
	@Test
	public void doesNotConsiderJavaTransientFieldsTransient() {
		assertThat(getProperty(TestClassComplex.class, "transientField").isTransient()).isFalse();
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void findsSimpleGettersAndASetters() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "id");

		assertThat(property.getGetter()).isPresent();
		assertThat(property.getSetter()).isPresent();
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void doesNotUseInvalidGettersAndASetters() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "anotherId");

		assertThat(property.getGetter()).isNotPresent();
		assertThat(property.getSetter()).isNotPresent();
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void usesCustomGetter() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "yetAnotherId");

		assertThat(property.getGetter()).isPresent();
		assertThat(property.getSetter()).isNotPresent();
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void usesCustomSetter() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "yetYetAnotherId");

		assertThat(property.getGetter()).isNotPresent();
		assertThat(property.getSetter()).isPresent();
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void doesNotDiscoverGetterAndSetterIfNoPropertyDescriptorGiven() {

		Field field = ReflectionUtils.findField(AccessorTestClass.class, "id");
		PersistentProperty<SamplePersistentProperty> property = new SamplePersistentProperty(Property.of(field),
				getEntity(AccessorTestClass.class), typeHolder);

		assertThat(property.getGetter()).isNotPresent();
		assertThat(property.getSetter()).isNotPresent();
	}

	/**
	 * @see DATACMNS-337
	 */
	@Test
	public void resolvesActualType() {

		SamplePersistentProperty property = getProperty(Sample.class, "person");
		assertThat(property.getActualType()).isEqualTo(Person.class);

		property = getProperty(Sample.class, "persons");
		assertThat(property.getActualType()).isEqualTo(Person.class);

		property = getProperty(Sample.class, "personArray");
		assertThat(property.getActualType()).isEqualTo(Person.class);

		property = getProperty(Sample.class, "personMap");
		assertThat(property.getActualType()).isEqualTo(Person.class);
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersCollectionPropertyEntitiesIfComponentTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "persons");
		assertThat(property.isEntity()).isTrue();
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersMapPropertyEntitiesIfValueTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "personMap");
		assertThat(property.isEntity()).isTrue();
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersArrayPropertyEntitiesIfComponentTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "personArray");
		assertThat(property.isEntity()).isTrue();
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersCollectionPropertySimpleIfComponentTypeIsSimple() {

		SamplePersistentProperty property = getProperty(Sample.class, "strings");
		assertThat(property.isEntity()).isFalse();
	}

	/**
	 * @see DATACMNS-562
	 */
	@Test
	public void doesNotConsiderPropertyWithTreeMapMapValueAnEntity() {

		SamplePersistentProperty property = getProperty(TreeMapWrapper.class, "map");
		assertThat(property.getPersistentEntityType()).isEmpty();
		assertThat(property.isEntity()).isFalse();
	}

	private <T> BasicPersistentEntity<T, SamplePersistentProperty> getEntity(Class<T> type) {
		return new BasicPersistentEntity<>(ClassTypeInformation.from(type));
	}

	private <T> SamplePersistentProperty getProperty(Class<T> type, String name) {

		Optional<Field> field = Optional.ofNullable(ReflectionUtils.findField(type, name));

		Property property = field.map(it -> Property.of(it, getPropertyDescriptor(type, name)))
				.orElseGet(() -> Property.of(getPropertyDescriptor(type, name).orElseThrow(
						() -> new IllegalArgumentException(String.format("Couldn't find property %s on %s!", name, type)))));

		return new SamplePersistentProperty(property, getEntity(type), typeHolder);
	}

	private static Optional<PropertyDescriptor> getPropertyDescriptor(Class<?> type, String propertyName) {

		try {

			return Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())//
					.filter(it -> it.getName().equals(propertyName))//
					.findFirst();

		} catch (IntrospectionException o_O) {
			throw new RuntimeException(o_O);
		}
	}

	class Generic<T> {
		T genericField;

	}

	class FirstConcrete extends Generic<String> {

	}

	class SecondConcrete extends Generic<Integer> {

	}

	@SuppressWarnings("serial")
	class TestClassSet extends TreeSet<Object> {}

	@SuppressWarnings("rawtypes")
	class TestClassComplex {

		String id;
		TestClassSet testClassSet;
		Map map;
		Collection collection;
		transient Object transientField;
	}

	class AccessorTestClass {

		// Valid getters and setters
		Long id;
		// Invalid getters and setters
		Long anotherId;

		// Customized getter
		Number yetAnotherId;

		// Customized setter
		Number yetYetAnotherId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAnotherId() {
			return anotherId.toString();
		}

		public void setAnotherId(String anotherId) {
			this.anotherId = Long.parseLong(anotherId);
		}

		public Long getYetAnotherId() {
			return null;
		}

		public void setYetYetAnotherId(Object yetYetAnotherId) {
			this.yetYetAnotherId = null;
		}
	}

	class SamplePersistentProperty extends AbstractPersistentProperty<SamplePersistentProperty> {

		public SamplePersistentProperty(Property property, PersistentEntity<?, SamplePersistentProperty> owner,
				SimpleTypeHolder simpleTypeHolder) {
			super(property, owner, simpleTypeHolder);
		}

		public boolean isIdProperty() {
			return false;
		}

		public boolean isVersionProperty() {
			return false;
		}

		@Override
		public boolean isAssociation() {
			return false;
		}

		@Override
		protected Association<SamplePersistentProperty> createAssociation() {
			return null;
		}

		@Override
		public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
			return Optional.empty();
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
			return false;
		}

		@Override
		public <A extends Annotation> Optional<A> findPropertyOrOwnerAnnotation(Class<A> annotationType) {
			return Optional.empty();
		}
	}

	static class Sample {

		Person person;
		Collection<Person> persons;
		Person[] personArray;
		Map<String, Person> personMap;
		Collection<String> strings;
	}

	class TreeMapWrapper {
		TreeMap<String, TreeMap<String, String>> map;
	}
}
