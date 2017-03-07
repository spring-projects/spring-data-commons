package org.springframework.data.repository.config;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link SelectionSet}
 *
 * @author Jens Schauder
 */
public class SelectionSetUnitTests {

	@Test // DATACMNS-764
	public void returnsUniqueResult() {
		assertEquals("single value", new SelectionSet<>(singleton("single value")).uniqueResult());
	}

	@Test // DATACMNS-764
	public void emptyCollectionReturnsNull() {
		assertNull(new SelectionSet<Object>(emptySet()).uniqueResult());
	}

	@Test(expected = IllegalStateException.class) // DATACMNS-764
	public void multipleElementsThrowException() {
		new SelectionSet<>(asList("one", "two")).uniqueResult();
	}

	@Test(expected = NullPointerException.class) // DATACMNS-764
	public void throwsCustomExceptionWhenConfigured() {

		new SelectionSet<>(asList("one", "two"), c -> {
			throw new NullPointerException();
		}).uniqueResult();
	}

	@Test // DATACMNS-764
	public void useseFallbackWhenConfigured() {

		String value = new SelectionSet<>(asList("one", "two"), c -> String.valueOf(c.size())).uniqueResult();

		assertEquals("2", value);
	}

	@Test // DATACMNS-764
	public void returnsUniqueResultAfterFilter() {

		SelectionSet<String> selection = new SelectionSet<>(asList("one", "two", "three")).filterIfNecessary(s -> s.contains("w"));

		assertEquals("two", selection.uniqueResult());
	}

	@Test // DATACMNS-764
	public void ignoresFilterWhenResultIsAlreadyUnique() {

		SelectionSet<String> selection = new SelectionSet<>(asList("one")).filterIfNecessary(s -> s.contains("w"));

		assertEquals("one", selection.uniqueResult());
	}
}
