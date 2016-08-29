/*
 * Copyright 2016 the original author or authors.
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
 * @soundtrack K2 - Der Berg Ruft (Club Mix)
 */
public class ExampleMatcherUnitTests {

	ExampleMatcher matcher;

	@Before
	public void setUp() throws Exception {
		matcher = matching();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnDefault() throws Exception {
		assertThat(matcher.getDefaultStringMatcher(), is(StringMatcher.DEFAULT));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnFalseByDefault() throws Exception {
		assertThat(matcher.isIgnoreCaseEnabled(), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoredPathsIsEmptyByDefault() throws Exception {
		assertThat(matcher.getIgnoredPaths(), is(empty()));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnIgnoreByDefault() throws Exception {
		assertThat(matcher.getNullHandler(), is(NullHandler.IGNORE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void ignoredPathsIsNotModifiable() throws Exception {
		matcher.getIgnoredPaths().add("¯\\_(ツ)_/¯");
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseEnabled() throws Exception {

		matcher = matching().withIgnoreCase();

		assertThat(matcher.isIgnoreCaseEnabled(), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseSet() throws Exception {

		matcher = matching().withIgnoreCase(true);

		assertThat(matcher.isIgnoreCaseEnabled(), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnInclude() throws Exception {

		matcher = matching().withIncludeNullValues();

		assertThat(matcher.getNullHandler(), is(NullHandler.INCLUDE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnIgnore() throws Exception {

		matcher = matching().withIgnoreNullValues();

		assertThat(matcher.getNullHandler(), is(NullHandler.IGNORE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnConfiguredValue() throws Exception {

		matcher = matching().withNullHandler(NullHandler.INCLUDE);

		assertThat(matcher.getNullHandler(), is(NullHandler.INCLUDE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoredPathsShouldReturnCorrectProperties() throws Exception {

		matcher = matching().withIgnorePaths("foo", "bar", "baz");

		assertThat(matcher.getIgnoredPaths(), contains("foo", "bar", "baz"));
		assertThat(matcher.getIgnoredPaths(), hasSize(3));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoredPathsShouldReturnUniqueProperties() throws Exception {

		matcher = matching().withIgnorePaths("foo", "bar", "foo");

		assertThat(matcher.getIgnoredPaths(), contains("foo", "bar"));
		assertThat(matcher.getIgnoredPaths(), hasSize(2));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void withCreatesNewInstance() throws Exception {

		matcher = matching().withIgnorePaths("foo", "bar", "foo");
		ExampleMatcher configuredExampleSpec = matcher.withIgnoreCase();

		assertThat(matcher, is(not(sameInstance(configuredExampleSpec))));
		assertThat(matcher.getIgnoredPaths(), hasSize(2));
		assertThat(matcher.isIgnoreCaseEnabled(), is(false));

		assertThat(configuredExampleSpec.getIgnoredPaths(), hasSize(2));
		assertThat(configuredExampleSpec.isIgnoreCaseEnabled(), is(true));
	}

	/**
	 * @see DATACMNS-900
	 */
	@Test
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
