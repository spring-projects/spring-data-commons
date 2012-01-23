package org.springframework.data.mapping.model;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
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

	@Test
	public void foo() {

		Field field1 = ReflectionUtils.findField(Bar1.class, "field");
		Field field2 = ReflectionUtils.findField(Bar2.class, "field");
		assertThat(field1, is(field2));
		System.out.println(field1.getType());

		System.out.println(ClassTypeInformation.from(Bar1.class).getProperty("field").getType());
		System.out.println(ClassTypeInformation.from(Bar2.class).getProperty("field").getType());
	}

	class Foo<T> {
		T field;
	}

	class Bar1 extends Foo<String> {

	}

	class Bar2 extends Foo<Object> {

	}

	@SuppressWarnings("serial")
	class TestClassSet extends TreeSet<Object> {
	}

	class TestClassComplex {

		String id;
		TestClassSet testClassSet;
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
