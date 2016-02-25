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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.ExampleSpec.NullHandler;
import org.springframework.data.domain.ExampleSpec.StringMatcher;

/**
 * Unit test for {@link ExampleSpec}.
 *
 * @author Mark Paluch
 * @soundtrack Harmonic Rush - The Dark Side of Persia (ahmed romel remix)
 */
public class TypedExampleSpecUnitTests {

	TypedExampleSpec<Person> exampleSpec;

	@Before
	public void setUp() throws Exception {
		exampleSpec = ExampleSpec.typed(Person.class);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void typeShouldReturnConfiguredType() throws Exception {
		assertThat(exampleSpec.getType(), equalTo(Person.class));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnDefault() throws Exception {
		assertThat(exampleSpec.getDefaultStringMatcher(), is(StringMatcher.DEFAULT));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnContainingWhenConfigured() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withStringMatcherContaining();
		assertThat(exampleSpec.getDefaultStringMatcher(), is(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnStartingWhenConfigured() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withStringMatcherStarting();
		assertThat(exampleSpec.getDefaultStringMatcher(), is(StringMatcher.STARTING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnEndingWhenConfigured() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withStringMatcherEnding();
		assertThat(exampleSpec.getDefaultStringMatcher(), is(StringMatcher.ENDING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnFalseByDefault() throws Exception {
		assertThat(exampleSpec.isIgnoreCaseEnabled(), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoredPathsIsEmptyByDefault() throws Exception {
		assertThat(exampleSpec.getIgnoredPaths(), is(empty()));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnIgnoreByDefault() throws Exception {
		assertThat(exampleSpec.getNullHandler(), is(NullHandler.IGNORE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void ignoredPathsIsNotModifiable() throws Exception {
		exampleSpec.getIgnoredPaths().add("¯\\_(ツ)_/¯");
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = IllegalArgumentException.class)
	public void defaultExampleSpecWithoutTypeFails() throws Exception {
		ExampleSpec.typed(null);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseEnabled() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIgnoreCase();

		assertThat(exampleSpec.isIgnoreCaseEnabled(), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseSet() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIgnoreCase(true);

		assertThat(exampleSpec.isIgnoreCaseEnabled(), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnInclude() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIncludeNullValues();

		assertThat(exampleSpec.getNullHandler(), is(NullHandler.INCLUDE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnIgnore() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIgnoreNullValues();

		assertThat(exampleSpec.getNullHandler(), is(NullHandler.IGNORE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnConfiguredValue() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withNullHandler(NullHandler.INCLUDE);

		assertThat(exampleSpec.getNullHandler(), is(NullHandler.INCLUDE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoredPathsShouldReturnCorrectProperties() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIgnorePaths("foo", "bar", "baz");

		assertThat(exampleSpec.getIgnoredPaths(), contains("foo", "bar", "baz"));
		assertThat(exampleSpec.getIgnoredPaths(), hasSize(3));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoredPathsShouldReturnUniqueProperties() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIgnorePaths("foo", "bar", "foo");

		assertThat(exampleSpec.getIgnoredPaths(), contains("foo", "bar"));
		assertThat(exampleSpec.getIgnoredPaths(), hasSize(2));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void withCreatesNewInstance() throws Exception {

		exampleSpec = ExampleSpec.typed(Person.class).withIgnorePaths("foo", "bar", "foo");
		TypedExampleSpec<Person> configuredExampleSpec = exampleSpec.withIgnoreCase();

		assertThat(exampleSpec, is(not(sameInstance(configuredExampleSpec))));
		assertThat(exampleSpec.getIgnoredPaths(), hasSize(2));
		assertThat(exampleSpec.isIgnoreCaseEnabled(), is(false));

		assertThat(configuredExampleSpec.getIgnoredPaths(), hasSize(2));
		assertThat(configuredExampleSpec.isIgnoreCaseEnabled(), is(true));
	}

	static class Person {

		String firstname;
	}
}
