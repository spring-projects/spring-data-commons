/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Version}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class VersionUnitTests {

	@Test // DATCMNS-384
	void sameVersionsEqualOneDigits() {

		Version first = new Version(6);
		Version second = new Version(6);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	@Test // DATCMNS-384
	void sameVersionsEqualTwoDigits() {

		Version first = new Version(5, 2);
		Version second = new Version(5, 2);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	@Test // DATCMNS-384
	void sameVersionsEqualThreeDigits() {

		Version first = new Version(1, 2, 3);
		Version second = new Version(1, 2, 3);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	@Test // DATCMNS-384
	void sameVersionsEqualFourDigits() {

		Version first = new Version(1, 2, 3, 1000);
		Version second = new Version(1, 2, 3, 1000);

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	@Test // DATCMNS-384
	void parsesVersionCorrectlyOneDigits() {

		Version version = Version.parse("5");
		assertThat(version).isEqualTo(new Version(5));
	}

	@Test // DATCMNS-384
	void parsesVersionCorrectlyTwoDigits() {

		Version version = Version.parse("5.2");
		assertThat(version).isEqualTo(new Version(5, 2));
	}

	@Test // DATCMNS-384
	void parsesVersionCorrectlyThreeDigits() {

		Version version = Version.parse("12.1.3");
		assertThat(version).isEqualTo(new Version(12, 1, 3));
	}

	@Test // DATCMNS-384
	void parsesVersionCorrectlyFourDigits() {

		Version version = Version.parse("12.1.3.1000");
		assertThat(version).isEqualTo(new Version(12, 1, 3, 1000));
	}

	@Test // DATCMNS-384
	void comparesToCorrectly() {

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

		assertThat(nextMajor.compareTo(version)).isGreaterThan(0);
		assertThat(nextMinor.compareTo(version)).isGreaterThan(0);
		assertThat(nextBugfix.compareTo(version)).isGreaterThan(0);
		assertThat(nextBuild.compareTo(version)).isGreaterThan(0);
	}

	@Test // DATCMNS-384
	void removesTrailingZerosAfterSecondValueForToString() {

		assertThat(new Version(2)).hasToString("2.0");
		assertThat(new Version(2, 0)).hasToString("2.0");
		assertThat(new Version(2, 0, 0)).hasToString("2.0");
		assertThat(new Version(2, 0, 0, 0)).hasToString("2.0");
		assertThat(new Version(2, 0, 1)).hasToString("2.0.1");
		assertThat(new Version(2, 0, 1, 0)).hasToString("2.0.1");
		assertThat(new Version(2, 0, 0, 1)).hasToString("2.0.0.1");
	}

	@Test // DATACMNS-496
	void parseShouldRemoveNonNumericVersionParts() {
		assertThat(Version.parse("2.0.0-rc1")).isEqualTo(new Version(2, 0, 0));
	}

	@Test // DATACMNS-719, DATACMNS-496
	void removesNonNumericSuffix() {
		assertThat(Version.parse("4.2.0.RELEASE")).isEqualTo(new Version(4, 2, 0));
	}

	@Test // DATACMNS-719, DATACMNS-496
	void rejectsNonNumericPartOnNonLastPosition() {

		assertThatIllegalArgumentException().isThrownBy(() -> Version.parse("1.RELEASE.2"))
				.withMessageContaining("1.RELEASE.2");
	}
}
