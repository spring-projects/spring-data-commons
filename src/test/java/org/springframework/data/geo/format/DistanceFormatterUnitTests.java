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
import static org.springframework.data.geo.format.DistanceFormatter.*;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;

/**
 * Unit tests for {@link DistanceFormatter}.
 *
 * @author Oliver Gierke
 */
class DistanceFormatterUnitTests {

	static Distance REFERENCE = new Distance(10.8, Metrics.KILOMETERS);

	@Test // DATAREST-279, DATACMNS-626
	void rejectsArbitraryNonsense() {
		assertThatIllegalArgumentException().isThrownBy(() -> INSTANCE.convert("foo"));
	}

	@Test // DATAREST-279, DATACMNS-626
	void rejectsUnsupportedMetric() {
		assertThatIllegalArgumentException().isThrownBy(() -> INSTANCE.convert("10.8cm"));
	}

	@Test // DATAREST-279, DATACMNS-626
	void printsDistance() {
		assertThat(INSTANCE.print(REFERENCE, Locale.US)).isEqualTo("10.8km");
	}

	static Collection<String[]> parameters() {
		return Arrays.asList(new String[] { "10.8km" }, new String[] { " 10.8km" }, new String[] { " 10.8 km" },
				new String[] { " 10.8 km " }, new String[] { " 10.8 KM" }, new String[] { " 10.8 kilometers" },
				new String[] { " 10.8 KILOMETERS" }, new String[] { " 10.8 KILOMETERS " });
	}

	@ParameterizedTest // DATAREST-279, DATACMNS-626
	@MethodSource("parameters")
	void parsesDistanceFromString(String source) {
		assertThat(INSTANCE.convert(source)).isEqualTo(REFERENCE);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	void parsesDistances(String source) throws ParseException {
		assertThat(INSTANCE.parse(source, Locale.US)).isEqualTo(REFERENCE);
	}
}
