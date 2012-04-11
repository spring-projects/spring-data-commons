package org.springframework.data.mapping.model;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
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
		entity = new BasicPersistentEntity<TestClassComplex, SamplePersistentProperty>(typeInfo);
		typeHolder = new SimpleTypeHolder();
	}

	/**
	 * @see DATACMNS-68
	 */
	@Test
	public void discoversComponentTypeCorrectly() throws Exception {

		Field field = ReflectionUtils.findField(TestClassComplex.class, "testClassSet");

		SamplePersistentProperty property = new SamplePersistentProperty(field, null, entity, typeHolder);
		property.getComponentType();
	}

	@Test
	public void returnsNestedEntityTypeCorrectly() {

		Field field = ReflectionUtils.findField(TestClassComplex.class, "testClassSet");

		SamplePersistentProperty property = new SamplePersistentProperty(field, null, entity, typeHolder);
		assertThat(property.getPersistentEntityType().iterator().hasNext(), is(false));
	}

	/**
	 * @see DATACMNS-132
	 */
	@Test
	public void isEntityWorksForUntypedMaps() throws Exception {

		Field field = ReflectionUtils.findField(TestClassComplex.class, "map");
		SamplePersistentProperty property = new SamplePersistentProperty(field, null, entity, typeHolder);
		assertThat(property.isEntity(), is(false));
	}

	/**
	 * @see DATACMNS-132
	 */
	@Test
	public void isEntityWorksForUntypedCollection() throws Exception {

		Field field = ReflectionUtils.findField(TestClassComplex.class, "collection");
		SamplePersistentProperty property = new SamplePersistentProperty(field, null, entity, typeHolder);
		assertThat(property.isEntity(), is(false));
	}

	/**
	 * @see DATACMNS-121
	 */
	@Test
	public void considersPropertiesEqualIfFieldEquals() {

		Field first = ReflectionUtils.findField(FirstConcrete.class, "genericField");
		Field second = ReflectionUtils.findField(SecondConcrete.class, "genericField");

		SamplePersistentProperty firstProperty = new SamplePersistentProperty(first, null, entity, typeHolder);
		SamplePersistentProperty secondProperty = new SamplePersistentProperty(second, null, entity, typeHolder);

		assertThat(firstProperty, is(secondProperty));
		assertThat(firstProperty.hashCode(), is(secondProperty.hashCode()));
	}

	class Generic<T> {
		T genericField;
	}

	class FirstConcrete extends Generic<String> {

	}

	class SecondConcrete extends Generic<Integer> {

	}

	@SuppressWarnings("serial")
	class TestClassSet extends TreeSet<Object> {
	}

	@SuppressWarnings("rawtypes")
	class TestClassComplex {

		String id;
		TestClassSet testClassSet;
		Map map;
		Collection collection;
	}

	class SamplePersistentProperty extends AbstractPersistentProperty<SamplePersistentProperty> {

		public SamplePersistentProperty(Field field, PropertyDescriptor propertyDescriptor,
				PersistentEntity<?, SamplePersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
			super(field, propertyDescriptor, owner, simpleTypeHolder);
		}

		public boolean isIdProperty() {
			return false;
		}

		@Override
		protected Association<SamplePersistentProperty> createAssociation() {
			return null;
		}
	}
}
