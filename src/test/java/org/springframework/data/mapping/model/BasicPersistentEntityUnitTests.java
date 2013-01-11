package org.springframework.data.mapping.model;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentEntitySpec;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.Person;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for {@link BasicPersistentEntity}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicPersistentEntityUnitTests<T extends PersistentProperty<T>> {

	@Mock
	T property;

	@Test
	public void assertInvariants() {
		PersistentEntitySpec.assertInvariants(createEntity(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeInformation() {
		new BasicPersistentEntity<Object, T>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProperty() {
		createEntity(null).addPersistentProperty(null);
	}

	@Test
	public void returnsNullForTypeAliasIfNoneConfigured() {

		PersistentEntity<Entity, T> entity = new BasicPersistentEntity<Entity, T>(ClassTypeInformation.from(Entity.class));
		assertThat(entity.getTypeAlias(), is(nullValue()));
	}

	@Test
	public void returnsTypeAliasIfAnnotated() {

		PersistentEntity<AliasedEntity, T> entity = new BasicPersistentEntity<AliasedEntity, T>(
				ClassTypeInformation.from(AliasedEntity.class));
		assertThat(entity.getTypeAlias(), is((Object) "foo"));
	}

	/**
	 * @see DATACMNS-50
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void considersComparatorForPropertyOrder() {

		BasicPersistentEntity<Person, T> entity = createEntity(new Comparator<T>() {
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

		SortedSet<T> properties = (SortedSet<T>) ReflectionTestUtils.getField(entity, "properties");

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

		MutablePersistentEntity<Person, T> entity = createEntity(null);
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

		MutablePersistentEntity<Person, T> entity = createEntity(null);

		when(property.isIdProperty()).thenReturn(true);

		entity.addPersistentProperty(property);

		try {
			entity.addPersistentProperty(property);
			fail("Expected MappingException!");
		} catch (MappingException e) {
			// expected
		}
	}

	private BasicPersistentEntity<Person, T> createEntity(Comparator<T> comparator) {
		return new BasicPersistentEntity<Person, T>(ClassTypeInformation.from(Person.class), comparator);
	}

	@TypeAlias("foo")
	static class AliasedEntity {

	}

	static class Entity {

	}
}
