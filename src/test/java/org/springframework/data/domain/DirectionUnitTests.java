package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;

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

		assertThat(Direction.fromString("asc")).isEqualTo(Direction.ASC);
		assertThat(Direction.fromString("desc")).isEqualTo(Direction.DESC);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidString() throws Exception {
		Direction.fromString("foo");
	}
}
