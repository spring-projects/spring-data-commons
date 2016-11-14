package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;

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

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}

	/**
	 * Asserts that both objects are not equal to each other and differ in hash code, too.
	 * 
	 * @param first
	 * @param second
	 */
	public static void assertNotEqualsAndHashcode(Object first, Object second) {

		assertThat(first).isNotEqualTo(second);
		assertThat(second).isNotEqualTo(first);
		assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
	}
}
