package org.springframework.data.util;

import org.junit.Test;

/**
 * Unit tests for {@link TypeDiscoverer}.
 *
 * @author Oliver Gierke
 */
public class TypeDiscovererUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullType() {
		new TypeDiscoverer<Object>(null, null, null);
	}
}
