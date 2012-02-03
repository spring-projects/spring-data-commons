package org.springframework.data.domain;

import static org.junit.Assert.*;

/**
 * @author Oliver Gierke
 */
public abstract class UnitTestUtils {

	private UnitTestUtils() {

	}

	/**
	 * Asserts that delivered objects both equal each other as well as return the same hash code.
	 * 
	 * @param first
	 * @param second
	 */
	public static void assertEqualsAndHashcode(Object first, Object second) {

		assertEquals(first, second);
		assertEquals(second, first);
		assertEquals(first.hashCode(), second.hashCode());
	}

	/**
	 * Asserts that both objects are not equal to each other and differ in hash code, too.
	 * 
	 * @param first
	 * @param second
	 */
	public static void assertNotEqualsAndHashcode(Object first, Object second) {

		assertFalse(first.equals(second));
		assertFalse(second.equals(first));
		assertFalse(first.hashCode() == second.hashCode());
	}
}
