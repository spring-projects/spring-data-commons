package org.springframework.data.mapping.model;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.mapping.PersistentEntitySpec;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.Person;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit test for {@link BasicPersistentEntity}.
 *
 * @author Oliver Gierke
 */
public class BasicPersistentEntityUnitTests<T extends PersistentProperty<T>> {

	@Test
	public void assertInvariants() {
		PersistentEntitySpec.assertInvariants(createEntity());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeInformation() {
		new BasicPersistentEntity<Object, T>(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProperty() {
		createEntity().addPersistentProperty(null);
	}
	
	private BasicPersistentEntity<Person, T> createEntity() {
		return new BasicPersistentEntity<Person, T>(ClassTypeInformation.from(Person.class));
	}
}
