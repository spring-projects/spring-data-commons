/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.geo.format;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.geo.format.PointFormatter.*;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.data.geo.Point;

/**
 * Unit tests for {@link PointFormatter}.
 *
 * @author Oliver Gierke
 */
class PointFormatterUnitTests {

	static Point REFERENCE = new Point(20.9, 10.8);

	@Test
	void testname() {
		// To make sure Maven picks up the tests
	}

	@Test
	// DATAREST-279, DATACMNS-626
	void rejectsArbitraryNonsense() {
		assertThatIllegalArgumentException().isThrownBy(() -> INSTANCE.convert("foo")).withMessageContaining("comma");
	}

	@Test
	// DATAREST-279, DATACMNS-626
	void rejectsMoreThanTwoCoordinates() {
		assertThatIllegalArgumentException().isThrownBy(() -> INSTANCE.convert("10.8,20.9,30.10"));
	}

	@Test
	// DATAREST-279, DATACMNS-626
	void rejectsInvalidCoordinate() {
		assertThatIllegalArgumentException().isThrownBy(() -> INSTANCE.convert("10.8,foo"));
	}

	static Collection<String[]> parameters() {
		return Arrays.asList(new String[] { "10.8,20.9" }, new String[] { " 10.8,20.9 " }, new String[] { " 10.8 ,20.9" },
				new String[] { " 10.8, 20.9 " });
	}

	@ParameterizedTest // DATAREST-279, DATACMNS-626
	@MethodSource("parameters")
	void convertsPointFromString(String source) {
		assertThat(INSTANCE.convert(source)).isEqualTo(REFERENCE);
	}

	@ParameterizedTest // DATAREST-279, DATACMNS-626
	@MethodSource("parameters")
	void parsesPoint(String source) throws ParseException {
		assertThat(INSTANCE.parse(source, Locale.US)).isEqualTo(REFERENCE);
	}
}
