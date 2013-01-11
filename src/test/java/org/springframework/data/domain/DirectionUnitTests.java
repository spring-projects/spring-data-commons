package org.springframework.data.domain;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.domain.Sort.Direction;

/**
 * Unit test for {@link Direction}.
 * 
 * @author Oliver Gierke
 */
public class DirectionUnitTests {

	@Test
	public void jpaValueMapping() throws Exception {

		assertEquals(Direction.ASC, Direction.fromString("asc"));
		assertEquals(Direction.DESC, Direction.fromString("desc"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidString() throws Exception {

		Direction.fromString("foo");
	}
}
