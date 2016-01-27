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

import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsCollectionContaining.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Example.*;
import static org.springframework.data.domain.PropertySpecifier.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Example.NullHandler;
import org.springframework.data.domain.Example.StringMatcher;
import org.springframework.data.domain.PropertySpecifier.NoOpPropertyValueTransformer;
import org.springframework.data.domain.PropertySpecifier.PropertyValueTransformer;

/**
 * @author Christoph Strobl
 */
public class ExampleUnitTests {

	private Person person;
	private Example<Person> example;

	@Before
	public void setUp() {

		person = new Person();
		person.firstname = "rand";

		example = exampleOf(person);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = IllegalArgumentException.class)
	public void exampleOfNullThrowsException() {
		new Example<Object>(null);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcher() {
		assertThat(example.getDefaultStringMatcher(), equalTo(StringMatcher.DEFAULT));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatDoesNotHavePropertySpecifier() {
		assertThat(example.getStringMatcherForPath("firstname"), equalTo(example.getDefaultStringMatcher()));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseConfiguredStringMatcherAsDefaultForPathThatDoesNotHavePropertySpecifier() {

		example = newExampleOf(person).withStringMatcher(StringMatcher.CONTAINING).get();

		assertThat(example.getDefaultStringMatcher(), equalTo(StringMatcher.CONTAINING));
		assertThat(example.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcher() {

		example = newExampleOf(person).withPropertySpecifier(
				PropertySpecifier.newPropertySpecifier("firstname").matchString(StringMatcher.CONTAINING).get()).get();

		assertThat(example.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldFavorStringMatcherDefinedForPathOverConfiguredDefaultStringMatcher() {

		example = newExampleOf(person)
				.withStringMatcher(StringMatcher.STARTING)
				.withPropertySpecifier(
						PropertySpecifier.newPropertySpecifier("firstname").matchString(StringMatcher.CONTAINING).get()).get();

		assertThat(example.getDefaultStringMatcher(), equalTo(StringMatcher.STARTING));
		assertThat(example.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatHasPropertySpecifierWithoutStringMatcher() {

		example = newExampleOf(person).withStringMatcher(StringMatcher.STARTING)
				.withPropertySpecifier(ignoreCase("firstname")).get();

		assertThat(example.getStringMatcherForPath("firstname"), equalTo(StringMatcher.STARTING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void isIgnoredPathShouldReturnFalseWhenNoPathsIgnored() {

		assertThat(example.getIgnoredPaths(), is(empty()));
		assertThat(example.isIgnoredPath("firstname"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void isIgnoredPathShouldReturnTrueWhenNoPathsIgnored() {

		example = newExampleOf(person).ignore("firstname").get();

		assertThat(example.getIgnoredPaths(), hasItem("firstname"));
		assertThat(example.isIgnoredPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void ignoredPathsShouldNotAllowModification() {
		example.getIgnoredPaths().add("o_O");
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnFalseByDefault() {

		assertThat(example.isIngnoreCaseEnabled(), is(false));
		assertThat(example.isIgnoreCaseForPath("firstname"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseIsEnabled() {

		example = newExampleOf(person).matchStringsWithIgnoreCase().get();

		assertThat(example.isIngnoreCaseEnabled(), is(true));
		assertThat(example.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldFavorPathSpecificSettings() {

		example = newExampleOf(person).withPropertySpecifier(ignoreCase("firstname")).get();

		assertThat(example.isIngnoreCaseEnabled(), is(false));
		assertThat(example.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getValueTransformerForPathReturnsNoOpValueTransformerByDefault() {
		assertThat(example.getValueTransformerForPath("firstname"), instanceOf(NoOpPropertyValueTransformer.class));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getValueTransformerForPathReturnsConfigurtedTransformerForPath() {

		PropertyValueTransformer transformer = new PropertyValueTransformer() {

			@Override
			public Object convert(Object source) {
				return source.toString();
			}
		};

		example = newExampleOf(person).withPropertySpecifier(
				newPropertySpecifier("firstname").withValueTransformer(transformer).get()).get();

		assertThat(example.getValueTransformerForPath("firstname"), equalTo(transformer));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsFalseIfNoneDefined() {
		assertThat(example.hasPropertySpecifiers(), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void getPropertiesSpecifiersShouldNotAllowAddingSpecifiers() {
		example.getPropertySpecifiers().add(ignoreCase("firstname"));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsTrueWhenAtLeastOneIsSet() {

		example = newExampleOf(person)
				.withStringMatcher(StringMatcher.STARTING)
				.withPropertySpecifier(
						PropertySpecifier.newPropertySpecifier("firstname").matchString(StringMatcher.CONTAINING).get()).get();

		assertThat(example.hasPropertySpecifiers(), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getSampleTypeRetunsSampleObjectsClass() {
		assertThat(example.getSampleType(), equalTo(Person.class));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getNullHandlerShouldReturnIgnoreByDefault() {
		assertThat(example.getNullHandler(), is(NullHandler.IGNORE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getNullHandlerShouldReturnConfiguredHandler() {

		example = newExampleOf(person).handleNullValues(NullHandler.INCLUDE).get();
		assertThat(example.getNullHandler(), is(NullHandler.INCLUDE));
	}

	static class Person {

		String firstname;
	}

}
