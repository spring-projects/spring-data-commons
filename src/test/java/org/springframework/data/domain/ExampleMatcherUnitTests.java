/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.domain;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.springframework.data.domain.ExampleMatcher.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.ExampleMatcher.*;

/**
 * Unit test for {@link ExampleMatcher}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @soundtrack K2 - Der Berg Ruft (Club Mix)
 */
public class ExampleMatcherUnitTests {

	ExampleMatcher matcher;

	@Before
	public void setUp() throws Exception {
		matcher = matching();
	}

	@Test // DATACMNS-810
	public void defaultStringMatcherShouldReturnDefault() throws Exception {
		assertThat(matcher.getDefaultStringMatcher(), is(StringMatcher.DEFAULT));
	}

	@Test // DATACMNS-810
	public void ignoreCaseShouldReturnFalseByDefault() throws Exception {
		assertThat(matcher.isIgnoreCaseEnabled(), is(false));
	}

	@Test // DATACMNS-810
	public void ignoredPathsIsEmptyByDefault() throws Exception {
		assertThat(matcher.getIgnoredPaths(), is(empty()));
	}

	@Test // DATACMNS-810
	public void nullHandlerShouldReturnIgnoreByDefault() throws Exception {
		assertThat(matcher.getNullHandler(), is(NullHandler.IGNORE));
	}

	@Test(expected = UnsupportedOperationException.class) // DATACMNS-810
	public void ignoredPathsIsNotModifiable() throws Exception {
		matcher.getIgnoredPaths().add("¯\\_(ツ)_/¯");
	}

	@Test // DATACMNS-810
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseEnabled() throws Exception {

		matcher = matching().withIgnoreCase();

		assertThat(matcher.isIgnoreCaseEnabled(), is(true));
	}

	@Test // DATACMNS-810
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseSet() throws Exception {

		matcher = matching().withIgnoreCase(true);

		assertThat(matcher.isIgnoreCaseEnabled(), is(true));
	}

	@Test // DATACMNS-810
	public void nullHandlerShouldReturnInclude() throws Exception {

		matcher = matching().withIncludeNullValues();

		assertThat(matcher.getNullHandler(), is(NullHandler.INCLUDE));
	}

	@Test // DATACMNS-810
	public void nullHandlerShouldReturnIgnore() throws Exception {

		matcher = matching().withIgnoreNullValues();

		assertThat(matcher.getNullHandler(), is(NullHandler.IGNORE));
	}

	@Test // DATACMNS-810
	public void nullHandlerShouldReturnConfiguredValue() throws Exception {

		matcher = matching().withNullHandler(NullHandler.INCLUDE);

		assertThat(matcher.getNullHandler(), is(NullHandler.INCLUDE));
	}

	@Test // DATACMNS-810
	public void ignoredPathsShouldReturnCorrectProperties() throws Exception {

		matcher = matching().withIgnorePaths("foo", "bar", "baz");

		assertThat(matcher.getIgnoredPaths(), contains("foo", "bar", "baz"));
		assertThat(matcher.getIgnoredPaths(), hasSize(3));
	}

	@Test // DATACMNS-810
	public void ignoredPathsShouldReturnUniqueProperties() throws Exception {

		matcher = matching().withIgnorePaths("foo", "bar", "foo");

		assertThat(matcher.getIgnoredPaths(), contains("foo", "bar"));
		assertThat(matcher.getIgnoredPaths(), hasSize(2));
	}

	@Test // DATACMNS-810
	public void withCreatesNewInstance() throws Exception {

		matcher = matching().withIgnorePaths("foo", "bar", "foo");
		ExampleMatcher configuredExampleSpec = matcher.withIgnoreCase();

		assertThat(matcher, is(not(sameInstance(configuredExampleSpec))));
		assertThat(matcher.getIgnoredPaths(), hasSize(2));
		assertThat(matcher.isIgnoreCaseEnabled(), is(false));

		assertThat(configuredExampleSpec.getIgnoredPaths(), hasSize(2));
		assertThat(configuredExampleSpec.isIgnoreCaseEnabled(), is(true));
	}

	@Test // DATACMNS-879
	public void defaultMatcherRequiresAllMatching() {

		assertThat(matching().isAllMatching(), is(true));
		assertThat(matching().isAnyMatching(), is(false));
	}

	@Test // DATACMNS-879
	public void allMatcherRequiresAllMatching() {

		assertThat(matchingAll().isAllMatching(), is(true));
		assertThat(matchingAll().isAnyMatching(), is(false));
	}

	@Test // DATACMNS-879
	public void anyMatcherYieldsAnyMatching() {

		assertThat(matchingAny().isAnyMatching(), is(true));
		assertThat(matchingAny().isAllMatching(), is(false));
	}

	@Test // DATACMNS-900
	public void shouldCompareUsingHashCodeAndEquals() throws Exception {

		matcher = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withIgnoreCase("ignored-case") //
				.withMatcher("hello", GenericPropertyMatchers.contains().caseSensitive()) //
				.withMatcher("world", new MatcherConfigurer<GenericPropertyMatcher>() {
					@Override
					public void configureMatcher(GenericPropertyMatcher matcher) {
						matcher.endsWith();
					}
				});

		ExampleMatcher sameAsMatcher = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withIgnoreCase("ignored-case") //
				.withMatcher("hello", GenericPropertyMatchers.contains().caseSensitive()) //
				.withMatcher("world", new MatcherConfigurer<GenericPropertyMatcher>() {
					@Override
					public void configureMatcher(GenericPropertyMatcher matcher) {
						matcher.endsWith();
					}
				});

		ExampleMatcher different = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withMatcher("hello", GenericPropertyMatchers.contains().ignoreCase());

		assertThat(matcher.hashCode(), is(sameAsMatcher.hashCode()));
		assertThat(matcher.hashCode(), is(not(different.hashCode())));
		assertThat(matcher, is(equalTo(sameAsMatcher)));
		assertThat(matcher, is(not(equalTo(different))));
	}

	static class Person {

		String firstname;
	}
}
