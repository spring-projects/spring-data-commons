package org.springframework.data.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link Version}.
 * 
 * @author Oliver Gierke
 */
public class VersionUnitTests {

	@Test
	public void sameVersionsEqualOneDigits() {

		Version first = new Version(6);
		Version second = new Version(6);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test
	public void sameVersionsEqualTwoDigits() {

		Version first = new Version(5, 2);
		Version second = new Version(5, 2);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test
	public void sameVersionsEqualThreeDigits() {

		Version first = new Version(1, 2, 3);
		Version second = new Version(1, 2, 3);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test
	public void sameVersionsEqualFourDigits() {

		Version first = new Version(1, 2, 3, 1000);
		Version second = new Version(1, 2, 3, 1000);

		assertThat(first, is(second));
		assertThat(second, is(first));
	}

	@Test
	public void parsesVersionCorrectlyOneDigits() {

		Version version = Version.parse("5");
		assertThat(version, is(new Version(5)));
	}

	@Test
	public void parsesVersionCorrectlyTwoDigits() {

		Version version = Version.parse("5.2");
		assertThat(version, is(new Version(5, 2)));
	}

	@Test
	public void parsesVersionCorrectlyThreeDigits() {

		Version version = Version.parse("12.1.3");
		assertThat(version, is(new Version(12, 1, 3)));
	}

	@Test
	public void parsesVersionCorrectlyFourDigits() {

		Version version = Version.parse("12.1.3.1000");
		assertThat(version, is(new Version(12, 1, 3, 1000)));
	}

	@Test
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

	@Test
	public void removesTrailingZerosAfterSecondValueForToString() {

		assertThat(new Version(2).toString(), is("2.0"));
		assertThat(new Version(2, 0).toString(), is("2.0"));
		assertThat(new Version(2, 0, 0).toString(), is("2.0"));
		assertThat(new Version(2, 0, 0, 0).toString(), is("2.0"));
		assertThat(new Version(2, 0, 1).toString(), is("2.0.1"));
		assertThat(new Version(2, 0, 1, 0).toString(), is("2.0.1"));
		assertThat(new Version(2, 0, 0, 1).toString(), is("2.1.0"));
	}
}
