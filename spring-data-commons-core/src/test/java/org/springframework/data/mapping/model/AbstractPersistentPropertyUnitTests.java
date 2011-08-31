package org.springframework.data.mapping.model;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.TreeSet;

import org.junit.Test;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AbstractPersistentProperty}.
 * 
 * @author Oliver Gierke
 */
public class AbstractPersistentPropertyUnitTests {

	/**
	 * @see DATACMNS-68
	 * @throws Exception
	 */
	@Test
	public void discoversComponentTypeCorrectly() throws Exception {

		BasicPersistentEntity<TestClassComplex, SamplePersistentProperty> entity = new BasicPersistentEntity<TestClassComplex, SamplePersistentProperty>(
				ClassTypeInformation.from(TestClassComplex.class));
		
		Field field = ReflectionUtils.findField(TestClassComplex.class, "testClassSet");
		
		SamplePersistentProperty property = new SamplePersistentProperty(field, null, entity, new SimpleTypeHolder());
		property.getComponentType();
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
