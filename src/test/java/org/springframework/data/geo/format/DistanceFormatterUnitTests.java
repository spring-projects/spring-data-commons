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
import static org.springframework.data.geo.format.DistanceFormatter.*;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;

/**
 * Unit tests for {@link DistanceFormatter}.
 * 
 * @author Oliver Gierke
 */
@RunWith(Enclosed.class)
public class DistanceFormatterUnitTests {

	static Distance REFERENCE = new Distance(10.8, Metrics.KILOMETERS);

	@Test
	public void testname() {
		// To make sure Maven picks up the tests
	}

	public static class UnparameterizedTests {

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test(expected = IllegalArgumentException.class)
		public void rejectsArbitraryNonsense() {
			INSTANCE.convert("foo");
		}

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test(expected = IllegalArgumentException.class)
		public void rejectsUnsupportedMetric() {
			INSTANCE.convert("10.8cm");
		}

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test
		public void printsDistance() {
			assertThat(INSTANCE.print(REFERENCE, Locale.US)).isEqualTo("10.8km");
		}
	}

	@RunWith(Parameterized.class)
	public static class ParameterizedTests {

		@Parameters
		public static Collection<String[]> parameters() {
			return Arrays.asList(new String[] { "10.8km" }, new String[] { " 10.8km" }, new String[] { " 10.8 km" },
					new String[] { " 10.8 km " }, new String[] { " 10.8 KM" }, new String[] { " 10.8 kilometers" },
					new String[] { " 10.8 KILOMETERS" }, new String[] { " 10.8 KILOMETERS " });
		}

		public @Parameter String source;

		/**
		 * @see DATAREST-279, DATACMNS-626
		 */
		@Test
		public void parsesDistanceFromString() {
			assertThat(INSTANCE.convert(source)).isEqualTo(REFERENCE);
		}

		@Test
		public void parsesDistances() throws ParseException {
			assertThat(INSTANCE.parse(source, Locale.US)).isEqualTo(REFERENCE);
		}
	}
}
