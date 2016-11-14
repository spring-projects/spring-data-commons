/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link Version}.
 * 
 * @author Oliver Gierke
 */
public class VersionUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void sameVersionsEqualOneDigits() {

		Version first = new Version(6);
		Version second = new Version(6);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void sameVersionsEqualTwoDigits() {

		Version first = new Version(5, 2);
		Version second = new Version(5, 2);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void sameVersionsEqualThreeDigits() {

		Version first = new Version(1, 2, 3);
		Version second = new Version(1, 2, 3);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void sameVersionsEqualFourDigits() {

		Version first = new Version(1, 2, 3, 1000);
		Version second = new Version(1, 2, 3, 1000);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void parsesVersionCorrectlyOneDigits() {

		Version version = Version.parse("5");
		assertThat(version).isEqualTo(new Version(5));
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void parsesVersionCorrectlyTwoDigits() {

		Version version = Version.parse("5.2");
		assertThat(version).isEqualTo(new Version(5, 2));
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void parsesVersionCorrectlyThreeDigits() {

		Version version = Version.parse("12.1.3");
		assertThat(version).isEqualTo(new Version(12, 1, 3));
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void parsesVersionCorrectlyFourDigits() {

		Version version = Version.parse("12.1.3.1000");
		assertThat(version).isEqualTo(new Version(12, 1, 3, 1000));
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void comparesToCorrectly() {

		Version version = new Version(1, 2, 3, 1000);
		Version nextBuild = new Version(1, 2, 3, 1001);
		Version nextBugfix = new Version(1, 2, 4);
		Version nextMinor = new Version(1, 3);
		Version nextMajor = new Version(2);

		assertThat(nextMajor.isGreaterThan(nextMinor)).isTrue();
		assertThat(nextMajor.isGreaterThan(nextMajor)).isFalse();
		assertThat(nextMajor.is(nextMajor)).isTrue();
		assertThat(nextMinor.isLessThan(nextMajor)).isTrue();
		assertThat(nextMinor.isLessThan(nextMinor)).isFalse();

		assertThat(nextMajor.compareTo(nextMajor)).isEqualTo(0);
		assertThat(nextMinor.compareTo(nextMinor)).isEqualTo(0);
		assertThat(nextBugfix.compareTo(nextBugfix)).isEqualTo(0);
		assertThat(nextBuild.compareTo(nextBuild)).isEqualTo(0);

		assertThat(version.compareTo(nextMajor)).isLessThan(0);
		assertThat(version.compareTo(nextMinor)).isLessThan(0);
		assertThat(version.compareTo(nextBugfix)).isLessThan(0);
		assertThat(version.compareTo(nextBuild)).isLessThan(0);

		assertThat(version.compareTo(null)).isGreaterThan(0);
		assertThat(nextMajor.compareTo(version)).isGreaterThan(0);
		assertThat(nextMinor.compareTo(version)).isGreaterThan(0);
		assertThat(nextBugfix.compareTo(version)).isGreaterThan(0);
		assertThat(nextBuild.compareTo(version)).isGreaterThan(0);
	}

	/**
	 * @see DATCMNS-384
	 */
	@Test
	public void removesTrailingZerosAfterSecondValueForToString() {

		assertThat(new Version(2).toString()).isEqualTo("2.0");
		assertThat(new Version(2, 0).toString()).isEqualTo("2.0");
		assertThat(new Version(2, 0, 0).toString()).isEqualTo("2.0");
		assertThat(new Version(2, 0, 0, 0).toString()).isEqualTo("2.0");
		assertThat(new Version(2, 0, 1).toString()).isEqualTo("2.0.1");
		assertThat(new Version(2, 0, 1, 0).toString()).isEqualTo("2.0.1");
		assertThat(new Version(2, 0, 0, 1).toString()).isEqualTo("2.0.0.1");
	}

	/**
	 * @see DATACMNS-496
	 */
	@Test
	public void parseShouldRemoveNonNumericVersionParts() {
		assertThat(Version.parse("2.0.0-rc1")).isEqualTo(new Version(2, 0, 0));
	}

	/**
	 * @see DATACMNS-719, DATACMNS-496
	 */
	@Test
	public void removesNonNumericSuffix() {
		assertThat(Version.parse("4.2.0.RELEASE")).isEqualTo(new Version(4, 2, 0));
	}

	/**
	 * @see DATACMNS-719, DATACMNS-496
	 */
	@Test
	public void rejectsNonNumericPartOnNonLastPosition() {

		exception.expect(IllegalArgumentException.class);
		exception.expectCause(Matchers.<Throwable> instanceOf(IllegalArgumentException.class));
		exception.expectMessage("1.RELEASE.2");

		Version.parse("1.RELEASE.2");
	}
}
