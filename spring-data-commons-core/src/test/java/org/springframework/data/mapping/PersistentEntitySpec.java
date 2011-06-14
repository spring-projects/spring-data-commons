package org.springframework.data.mapping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Some test methods that define expected behaviour for {@link PersistentEntity} interface. Implementation test classes
 * can simply extend that class to get the specs tested against an instance of their implementation.
 * 
 * @author Oliver Gierke
 */
public abstract class PersistentEntitySpec {

	public static void assertInvariants(PersistentEntity<?, ?> entity) {
		assertThat(entity.getName(), is(notNullValue()));
		assertThat(entity.getPreferredConstructor(), is(notNullValue()));
	}
}
