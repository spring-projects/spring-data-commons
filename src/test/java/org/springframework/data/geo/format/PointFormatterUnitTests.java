/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.geo.Point;

/**
 * Unit tests for {@link PointFormatter}.
 * 
 * @author Oliver Gierke
 */
@RunWith(Enclosed.class)
public class PointFormatterUnitTests {

	static Point REFERENCE = new Point(20.9, 10.8);

	@Test
	public void testname() {
		// To make sure Maven picks up the tests
	}

	public static class UnparameterizedTests {

		public @Rule ExpectedException exception = ExpectedException.none();

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test
		public void rejectsArbitraryNonsense() {

			exception.expect(IllegalArgumentException.class);
			exception.expectMessage("comma");

			INSTANCE.convert("foo");
		}

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test(expected = IllegalArgumentException.class)
		public void rejectsMoreThanTwoCoordinates() {
			INSTANCE.convert("10.8,20.9,30.10");
		}

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test(expected = IllegalArgumentException.class)
		public void rejectsInvalidCoordinate() {
			INSTANCE.convert("10.8,foo");
		}
	}

	@RunWith(Parameterized.class)
	public static class ParameterizedTests {

		@Parameters
		public static Collection<String[]> parameters() {
			return Arrays.asList(new String[] { "10.8,20.9" }, new String[] { " 10.8,20.9 " }, new String[] { " 10.8 ,20.9" },
					new String[] { " 10.8, 20.9 " });
		}

		public @Parameter String source;

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test
		public void convertsPointFromString() {
			assertThat(INSTANCE.convert(source)).isEqualTo(REFERENCE);
		}

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test
		public void parsesPoint() throws ParseException {
			assertThat(INSTANCE.parse(source, Locale.US)).isEqualTo(REFERENCE);
		}
	}
}
