package org.springframework.data.mapping.context;

import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.junit.Test;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 *
 * @author Oliver Gierke
 */
public class AbstractMappingContextUnitTest<T extends PersistentProperty<T>> {
	
	final SimpleTypeHolder holder = new SimpleTypeHolder();
	
	@Test
	public void doesNotTryToLookupPersistentEntityForLeafProperty() {
		
		DummyMappingContext context = new DummyMappingContext();
		context.setSimpleTypeHolder(holder);
		PersistentPropertyPath<T> path = context.getPersistentPropertyPath(PropertyPath.from("name", Person.class));
		org.junit.Assert.assertThat(path, is(notNull()));
	}

	class Person {
		String name;
	}
	
	
	class DummyMappingContext extends AbstractMappingContext<BasicPersistentEntity<Object, T>, T> {

		@Override
		@SuppressWarnings("unchecked")
		protected <S> BasicPersistentEntity<Object, T> createPersistentEntity(TypeInformation<S> typeInformation) {
			return new BasicPersistentEntity<Object, T>((TypeInformation<Object>) typeInformation) {
				
				@Override
				public void verify() {
					Assert.isTrue(!holder.isSimpleType(getType()));
				}
			};
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected T createPersistentProperty(final Field field, final PropertyDescriptor descriptor,
				final BasicPersistentEntity<Object, T> owner, final SimpleTypeHolder simpleTypeHolder) {
			
			PersistentProperty prop = mock(PersistentProperty.class);
			
			when(prop.getTypeInformation()).thenReturn(ClassTypeInformation.from(field.getType()));
			when(prop.getName()).thenReturn(field.getName());

			return (T) prop;
		}
	}
}
