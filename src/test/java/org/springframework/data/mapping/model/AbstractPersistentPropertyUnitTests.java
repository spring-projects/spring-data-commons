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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import lombok.Getter;
import lombok.Setter;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.Person;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractPersistentProperty}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public class AbstractPersistentPropertyUnitTests {

	private TypeInformation<TestClassComplex> typeInfo;
	private PersistentEntity<TestClassComplex, SamplePersistentProperty> entity;
	private SimpleTypeHolder typeHolder;

	@BeforeEach
	void setUp() {

		typeInfo = ClassTypeInformation.from(TestClassComplex.class);
		entity = new BasicPersistentEntity<>(typeInfo);
		typeHolder = new SimpleTypeHolder();
	}

	@Test // DATACMNS-68
	void discoversComponentTypeCorrectly() {
		assertThat(getProperty(TestClassComplex.class, "testClassSet").getComponentType()).isEqualTo(Object.class);
	}

	@Test // DATACMNS-101
	void returnsNestedEntityTypeCorrectly() {
		assertThat(getProperty(TestClassComplex.class, "testClassSet").getPersistentEntityTypes()).isEmpty();
	}

	@Test // DATACMNS-132
	void isEntityWorksForUntypedMaps() {
		assertThat(getProperty(TestClassComplex.class, "map").isEntity()).isFalse();
	}

	@Test // DATACMNS-132
	void isEntityWorksForUntypedCollection() {
		assertThat(getProperty(TestClassComplex.class, "collection").isEntity()).isFalse();
	}

	@Test // DATACMNS-121
	void considersPropertiesEqualIfFieldEquals() {

		SamplePersistentProperty firstProperty = getProperty(FirstConcrete.class, "genericField");
		SamplePersistentProperty secondProperty = getProperty(SecondConcrete.class, "genericField");

		assertThat(firstProperty).isEqualTo(secondProperty);
		assertThat(firstProperty.hashCode()).isEqualTo(secondProperty.hashCode());
	}

	@Test // DATACMNS-180
	void doesNotConsiderJavaTransientFieldsTransient() {
		assertThat(getProperty(TestClassComplex.class, "transientField").isTransient()).isFalse();
	}

	@Test // DATACMNS-206
	void findsSimpleGettersAndASetters() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "id");

		assertThat(property.getGetter()).isNotNull();
		assertThat(property.getSetter()).isNotNull();
	}

	@Test // DATACMNS-206
	void doesNotUseInvalidGettersAndASetters() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "anotherId");

		assertThat(property.getGetter()).isNull();
		assertThat(property.getSetter()).isNull();
	}

	@Test // DATACMNS-206
	void usesCustomGetter() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "yetAnotherId");

		assertThat(property.getGetter()).isNotNull();
		assertThat(property.getSetter()).isNull();
	}

	@Test // DATACMNS-206
	void usesCustomSetter() {

		SamplePersistentProperty property = getProperty(AccessorTestClass.class, "yetYetAnotherId");

		assertThat(property.getGetter()).isNull();
		assertThat(property.getSetter()).isNotNull();
	}

	@Test // DATACMNS-206
	void doesNotDiscoverGetterAndSetterIfNoPropertyDescriptorGiven() {

		Field field = ReflectionUtils.findField(AccessorTestClass.class, "id");
		Property property = Property.of(ClassTypeInformation.from(AccessorTestClass.class), field);

		PersistentProperty<SamplePersistentProperty> persistentProperty = new SamplePersistentProperty(property,
				getEntity(AccessorTestClass.class), typeHolder);

		assertThat(persistentProperty.getGetter()).isNull();
		assertThat(persistentProperty.getSetter()).isNull();
	}

	@Test // DATACMNS-337
	void resolvesActualType() {

		SamplePersistentProperty property = getProperty(Sample.class, "person");
		assertThat(property.getActualType()).isEqualTo(Person.class);

		property = getProperty(Sample.class, "persons");
		assertThat(property.getActualType()).isEqualTo(Person.class);

		property = getProperty(Sample.class, "personArray");
		assertThat(property.getActualType()).isEqualTo(Person.class);

		property = getProperty(Sample.class, "personMap");
		assertThat(property.getActualType()).isEqualTo(Person.class);
	}

	@Test // DATACMNS-462
	void considersCollectionPropertyEntitiesIfComponentTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "persons");
		assertThat(property.isEntity()).isTrue();
	}

	@Test // DATACMNS-462
	void considersMapPropertyEntitiesIfValueTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "personMap");
		assertThat(property.isEntity()).isTrue();
	}

	@Test // DATACMNS-462
	void considersArrayPropertyEntitiesIfComponentTypeIsEntity() {

		SamplePersistentProperty property = getProperty(Sample.class, "personArray");
		assertThat(property.isEntity()).isTrue();
	}

	@Test // DATACMNS-462
	void considersCollectionPropertySimpleIfComponentTypeIsSimple() {

		SamplePersistentProperty property = getProperty(Sample.class, "strings");
		assertThat(property.isEntity()).isFalse();
	}

	@Test // DATACMNS-562
	void doesNotConsiderPropertyWithTreeMapMapValueAnEntity() {

		SamplePersistentProperty property = getProperty(TreeMapWrapper.class, "map");
		assertThat(property.getPersistentEntityTypes()).isEmpty();
		assertThat(property.isEntity()).isFalse();
	}

	@Test // DATACMNS-867
	void resolvesFieldNameWithUnderscoresCorrectly() {

		SamplePersistentProperty property = getProperty(TestClassComplex.class, "var_name_with_underscores");
		assertThat(property.getName()).isEqualTo("var_name_with_underscores");
	}

	@Test // DATACMNS-1139
	void resolvesGenericsForRawType() {

		SamplePersistentProperty property = getProperty(FirstConcrete.class, "genericField");

		assertThat(property.getRawType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-1180
	void returnsAccessorsForGenericReturnType() {

		SamplePersistentProperty property = getProperty(ConcreteGetter.class, "genericField");

		assertThat(property.getSetter()).isNotNull();
		assertThat(property.getGetter()).isNotNull();
	}

	private <T> BasicPersistentEntity<T, SamplePersistentProperty> getEntity(Class<T> type) {
		return new BasicPersistentEntity<>(ClassTypeInformation.from(type));
	}

	private <T> SamplePersistentProperty getProperty(Class<T> type, String name) {

		TypeInformation<?> typeInformation = ClassTypeInformation.from(type);
		Optional<Field> field = Optional.ofNullable(ReflectionUtils.findField(type, name));
		Optional<PropertyDescriptor> descriptor = getPropertyDescriptor(type, name);

		Property property = Optionals.firstNonEmpty( //
				() -> Optionals.mapIfAllPresent(field, descriptor, (left, right) -> Property.of(typeInformation, left, right)), //
				() -> field.map(it -> Property.of(typeInformation, it)), //
				() -> descriptor.map(it -> Property.of(typeInformation, it))) //
				.orElseThrow(() -> new IllegalArgumentException(String.format("Couldn't find property %s on %s!", name, type)));

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

	@Getter
	@Setter
	class GenericGetter<T> {
		T genericField;
	}

	class ConcreteGetter extends GenericGetter<String> {}

	@SuppressWarnings("serial")
	class TestClassSet extends TreeSet<Object> {}

	@SuppressWarnings("rawtypes")
	class TestClassComplex {

		String id;
		TestClassSet testClassSet;
		Map map;
		Collection collection;
		transient Object transientField;
		String var_name_with_underscores;
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

		@Override
		public Class<?> getAssociationTargetType() {
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
