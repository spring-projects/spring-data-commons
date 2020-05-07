package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;

/**
 * Unit test for {@link Direction}.
 *
 * @author Oliver Gierke
 */
class DirectionUnitTests {

	@Test
	void jpaValueMapping() throws Exception {

		assertThat(Direction.fromString("asc")).isEqualTo(Direction.ASC);
		assertThat(Direction.fromString("desc")).isEqualTo(Direction.DESC);
	}

	@Test
	void rejectsInvalidString() {
		assertThatIllegalArgumentException().isThrownBy(() -> Direction.fromString("foo"));
	}
}
