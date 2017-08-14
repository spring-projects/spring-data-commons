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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.Person;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractPersistentProperty}.
 * 
 * @author Oliver Gierke
 */
public class AbstractPersistentPropertyUnitTests {

	SimpleTypeHolder typeHolder;

	@Before
	public void setUp() {
		typeHolder = new SimpleTypeHolder();
	}

	/**
	 * @see DATACMNS-68
	 */
	@Test
	public void discoversComponentTypeCorrectly() throws Exception {

		SamplePersistentProperty property = getProperty(TestClassComplex.class, "testClassSet");

		property.getComponentType();
	}

	/**
	 * @see DATACMNS-101
	 */
	@Test
	public void returnsNestedEntityTypeCorrectly() {

		SamplePersistentProperty property = getProperty(TestClassComplex.class, "testClassSet", null);

		assertThat(property.getPersistentEntityType().iterator().hasNext(), is(false));
	}

	/**
	 * @see DATACMNS-132
	 */
	@Test
	public void isEntityWorksForUntypedMaps() throws Exception {

		SamplePersistentProperty property = getProperty(TestClassComplex.class, "map", null);

		assertThat(property.isEntity(), is(false));
	}

	/**
	 * @see DATACMNS-132
	 */
	@Test
	public void isEntityWorksForUntypedCollection() throws Exception {

		SamplePersistentProperty property = getProperty(TestClassComplex.class, "collection", null);

		assertThat(property.isEntity(), is(false));
	}

	/**
	 * @see DATACMNS-121
	 */
	@Test
	public void considersPropertiesEqualIfFieldEquals() {

		SamplePersistentProperty firstProperty = getProperty(FirstConcrete.class, "genericField", null);
		SamplePersistentProperty secondProperty = getProperty(SecondConcrete.class, "genericField", null);

		assertThat(firstProperty, is(secondProperty));
		assertThat(firstProperty.hashCode(), is(secondProperty.hashCode()));
	}

	/**
	 * @see DATACMNS-180
	 */
	@Test
	public void doesNotConsiderJavaTransientFieldsTransient() {

		PersistentProperty<?> property = getProperty(TestClassComplex.class, "transientField", null);

		assertThat(property.isTransient(), is(false));
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void findsSimpleGettersAndASetters() {

		PersistentProperty<SamplePersistentProperty> property = getProperty(AccessorTestClass.class, "id");

		assertThat(property.getGetter(), is(notNullValue()));
		assertThat(property.getSetter(), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void doesNotUseInvalidGettersAndASetters() {

		PersistentProperty<SamplePersistentProperty> property = getProperty(AccessorTestClass.class, "anotherId");

		assertThat(property.getGetter(), is(nullValue()));
		assertThat(property.getSetter(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void usesCustomGetter() {

		PersistentProperty<SamplePersistentProperty> property = getProperty(AccessorTestClass.class, "yetAnotherId");

		assertThat(property.getGetter(), is(notNullValue()));
		assertThat(property.getSetter(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void usesCustomSetter() {

		PersistentProperty<SamplePersistentProperty> property = getProperty(AccessorTestClass.class, "yetAnotherId",
				getPropertyDescriptor(AccessorTestClass.class, "yetYetAnotherId"));

		assertThat(property.getGetter(), is(nullValue()));
		assertThat(property.getSetter(), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-206
	 */
	@Test
	public void returnsNullGetterAndSetterIfNoPropertyDescriptorGiven() {

		PersistentProperty<SamplePersistentProperty> property = getProperty(AccessorTestClass.class, "id", null);

		assertThat(property.getGetter(), is(nullValue()));
		assertThat(property.getSetter(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-337
	 */
	@Test
	public void resolvesActualType() {

		SamplePersistentProperty property = getProperty(Sample.class, "person");
		assertThat(property.getActualType(), is((Object) Person.class));

		property = getProperty(Sample.class, "persons");
		assertThat(property.getActualType(), is((Object) Person.class));

		property = getProperty(Sample.class, "personArray");
		assertThat(property.getActualType(), is((Object) Person.class));

		property = getProperty(Sample.class, "personMap");
		assertThat(property.getActualType(), is((Object) Person.class));
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersCollectionPropertyEntitiesIfComponentTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "persons");
		assertThat(property.isEntity(), is(true));
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersMapPropertyEntitiesIfValueTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "personMap");
		assertThat(property.isEntity(), is(true));
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersArrayPropertyEntitiesIfComponentTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "personArray");
		assertThat(property.isEntity(), is(true));
	}

	/**
	 * @see DATACMNS-462
	 */
	@Test
	public void considersCollectionPropertySimpleIfComponentTypeIsSimple() {

		SamplePersistentProperty property = getProperty(Sample.class, "strings");
		assertThat(property.isEntity(), is(false));
	}

	/**
	 * @see DATACMNS-562
	 */
	@Test
	public void doesNotConsiderPropertyWithTreeMapMapValueAnEntity() {

		SamplePersistentProperty property = getProperty(TreeMapWrapper.class, "map");
		assertThat(property.getPersistentEntityType(), is(emptyIterable()));
		assertThat(property.isEntity(), is(false));
	}

	@Test // DATACMNS-1139
	public void resolvesGenericsForRawType() {

		SamplePersistentProperty property = getProperty(FirstConcrete.class, "genericField");

		assertThat(property.getRawType(), is(typeCompatibleWith(String.class)));
	}

	private <T> SamplePersistentProperty getProperty(Class<T> type, String name) {
		return getProperty(type, name, getPropertyDescriptor(type, name));
	}

	private <T> SamplePersistentProperty getProperty(Class<T> type, String name, PropertyDescriptor descriptor) {

		BasicPersistentEntity<T, SamplePersistentProperty> entity = new BasicPersistentEntity<T, SamplePersistentProperty>(
				ClassTypeInformation.from(type));

		Field field = ReflectionUtils.findField(type, name);
		return new SamplePersistentProperty(field, descriptor, entity, typeHolder);
	}

	private static PropertyDescriptor getPropertyDescriptor(Class<?> type, String propertyName) {

		try {

			BeanInfo info = Introspector.getBeanInfo(type);

			for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
				if (descriptor.getName().equals(propertyName)) {
					return descriptor;
				}
			}

			return null;

		} catch (IntrospectionException e) {
			return null;
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

		public SamplePersistentProperty(Field field, PropertyDescriptor propertyDescriptor,
				PersistentEntity<?, SamplePersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
			super(field, propertyDescriptor, owner, simpleTypeHolder);
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
		public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
			return null;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
			return false;
		}

		@Override
		public <A extends Annotation> A findPropertyOrOwnerAnnotation(Class<A> annotationType) {
			return null;
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
