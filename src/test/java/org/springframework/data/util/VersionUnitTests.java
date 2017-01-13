/*
 * Copyright 2015-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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

	@Test // DATCMNS-384
	public void sameVersionsEqualOneDigits() {

		Version first = new Version(6);
		Version second = new Version(6);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test // DATCMNS-384
	public void sameVersionsEqualTwoDigits() {

		Version first = new Version(5, 2);
		Version second = new Version(5, 2);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test // DATCMNS-384
	public void sameVersionsEqualThreeDigits() {

		Version first = new Version(1, 2, 3);
		Version second = new Version(1, 2, 3);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test // DATCMNS-384
	public void sameVersionsEqualFourDigits() {

		Version first = new Version(1, 2, 3, 1000);
		Version second = new Version(1, 2, 3, 1000);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test // DATCMNS-384
	public void parsesVersionCorrectlyOneDigits() {

		Version version = Version.parse("5");
		assertThat(version, is(new Version(5)));
	}

	@Test // DATCMNS-384
	public void parsesVersionCorrectlyTwoDigits() {

		Version version = Version.parse("5.2");
		assertThat(version, is(new Version(5, 2)));
	}

	@Test // DATCMNS-384
	public void parsesVersionCorrectlyThreeDigits() {

		Version version = Version.parse("12.1.3");
		assertThat(version, is(new Version(12, 1, 3)));
	}

	@Test // DATCMNS-384
	public void parsesVersionCorrectlyFourDigits() {

		Version version = Version.parse("12.1.3.1000");
		assertThat(version, is(new Version(12, 1, 3, 1000)));
	}

	@Test // DATCMNS-384
	public void comparesToCorrectly() {

		Version version = new Version(1, 2, 3, 1000);
		Version nextBuild = new Version(1, 2, 3, 1001);
		Version nextBugfix = new Version(1, 2, 4);
		Version nextMinor = new Version(1, 3);
		Version nextMajor = new Version(2);

		assertThat(nextMajor.isGreaterThan(nextMinor), is(true));
		assertThat(nextMajor.isGreaterThan(nextMajor), is(false));
		assertThat(nextMajor.is(nextMajor), is(true));
		assertThat(nextMinor.isLessThan(nextMajor), is(true));
		assertThat(nextMinor.isLessThan(nextMinor), is(false));

		assertThat(nextMajor.compareTo(nextMajor), is(0));
		assertThat(nextMinor.compareTo(nextMinor), is(0));
		assertThat(nextBugfix.compareTo(nextBugfix), is(0));
		assertThat(nextBuild.compareTo(nextBuild), is(0));

		assertThat(version.compareTo(nextMajor), is(lessThan(0)));
		assertThat(version.compareTo(nextMinor), is(lessThan(0)));
		assertThat(version.compareTo(nextBugfix), is(lessThan(0)));
		assertThat(version.compareTo(nextBuild), is(lessThan(0)));

		assertThat(version.compareTo(null), is(greaterThan(0)));
		assertThat(nextMajor.compareTo(version), is(greaterThan(0)));
		assertThat(nextMinor.compareTo(version), is(greaterThan(0)));
		assertThat(nextBugfix.compareTo(version), is(greaterThan(0)));
		assertThat(nextBuild.compareTo(version), is(greaterThan(0)));
	}

	@Test // DATCMNS-384
	public void removesTrailingZerosAfterSecondValueForToString() {

		assertThat(new Version(2).toString(), is("2.0"));
		assertThat(new Version(2, 0).toString(), is("2.0"));
		assertThat(new Version(2, 0, 0).toString(), is("2.0"));
		assertThat(new Version(2, 0, 0, 0).toString(), is("2.0"));
		assertThat(new Version(2, 0, 1).toString(), is("2.0.1"));
		assertThat(new Version(2, 0, 1, 0).toString(), is("2.0.1"));
		assertThat(new Version(2, 0, 0, 1).toString(), is("2.0.0.1"));
	}

	@Test // DATACMNS-496
	public void parseShouldRemoveNonNumericVersionParts() {
		assertThat(Version.parse("2.0.0-rc1"), is(new Version(2, 0, 0)));
	}

	@Test // DATACMNS-719, DATACMNS-496
	public void removesNonNumericSuffix() {
		assertThat(Version.parse("4.2.0.RELEASE"), is(new Version(4, 2, 0)));
	}

	@Test // DATACMNS-719, DATACMNS-496
	public void rejectsNonNumericPartOnNonLastPosition() {

		exception.expect(IllegalArgumentException.class);
		exception.expectCause(Matchers.<Throwable> instanceOf(IllegalArgumentException.class));
		exception.expectMessage("1.RELEASE.2");

		Version.parse("1.RELEASE.2");
	}
}
