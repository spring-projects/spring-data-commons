package org.springframework.data.mapping.context;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
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
