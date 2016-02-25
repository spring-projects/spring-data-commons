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
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.ExampleSpec.GenericPropertyMatcher;
import org.springframework.data.domain.ExampleSpec.GenericPropertyMatchers;
import org.springframework.data.domain.ExampleSpec.MatcherConfigurer;
import org.springframework.data.domain.ExampleSpec.NoOpPropertyValueTransformer;
import org.springframework.data.domain.ExampleSpec.NullHandler;
import org.springframework.data.domain.ExampleSpec.PropertyValueTransformer;
import org.springframework.data.domain.ExampleSpec.StringMatcher;

/**
 * Test for {@link ExampleSpecAccessor}.
 *
 * @author Mark Paluch
 * @soundtrack Cabballero - Dancing With Tears In My Eyes (Dance Maxi)
 */
public class ExampleSpecAccessorUnitTests {

	private Person person;
	private ExampleSpec<Person> exampleSpec;
	private ExampleSpecAccessor exampleSpecAccessor;

	@Before
	public void setUp() {

		person = new Person();
		person.firstname = "rand";

		exampleSpec = ExampleSpec.of(Person.class);
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnDefault() {
		assertThat(exampleSpecAccessor.getDefaultStringMatcher(), equalTo(StringMatcher.DEFAULT));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnInclude() {

		exampleSpec = ExampleSpec.of(Person.class).withIncludeNullValues();
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getNullHandler(), equalTo(NullHandler.INCLUDE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldIgnorePaths() {

		exampleSpec = ExampleSpec.of(Person.class).withIgnorePaths("firstname");
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.isIgnoredPath("firstname"), equalTo(true));
		assertThat(exampleSpecAccessor.isIgnoredPath("lastname"), equalTo(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatDoesNotHavePropertySpecifier() {
		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"),
				equalTo(exampleSpec.getDefaultStringMatcher()));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseConfiguredStringMatcherAsDefaultForPathThatDoesNotHavePropertySpecifier() {

		exampleSpec = ExampleSpec.of(Person.class).withStringMatcher(StringMatcher.CONTAINING);
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		exampleSpec = ExampleSpec.of(Person.class).withIgnoreCase().withMatcher("firstname",
				new GenericPropertyMatcher().contains());
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("firstname"), equalTo(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseConfiguredIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		exampleSpec = ExampleSpec.of(Person.class).withIgnoreCase().withMatcher("firstname",
				new GenericPropertyMatcher().contains().caseSensitive());
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("firstname"), equalTo(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherStarting() {

		exampleSpec = ExampleSpec.of(Person.class).withMatcher("firstname", GenericPropertyMatchers.startsWith());
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.STARTING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherContaining() {

		exampleSpec = ExampleSpec.of(Person.class).withMatcher("firstname", GenericPropertyMatchers.contains());
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherRegex() {

		exampleSpec = ExampleSpec.of(Person.class).withMatcher("firstname", GenericPropertyMatchers.regex());
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.REGEX));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldFavorStringMatcherDefinedForPathOverConfiguredDefaultStringMatcher() {

		exampleSpec = ExampleSpec.of(Person.class).withStringMatcher(StringMatcher.ENDING)
				.withMatcher("firstname", new GenericPropertyMatcher().contains())
				.withMatcher("address.city", new GenericPropertyMatcher().startsWith())
				.withMatcher("lastname", new MatcherConfigurer<GenericPropertyMatcher>() {
					@Override
					public void configureMatcher(GenericPropertyMatcher matcher) {
						matcher.ignoreCase();
					}
				});

		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getPropertySpecifiers(), hasSize(3));
		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
		assertThat(exampleSpecAccessor.getStringMatcherForPath("lastname"), equalTo(StringMatcher.ENDING));
		assertThat(exampleSpecAccessor.getStringMatcherForPath("unknownProperty"), equalTo(StringMatcher.ENDING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatHasPropertySpecifierWithoutStringMatcher() {

		exampleSpec = ExampleSpec.of(Person.class).withStringMatcher(StringMatcher.STARTING).withMatcher("firstname",
				new MatcherConfigurer<GenericPropertyMatcher>() {
					@Override
					public void configureMatcher(GenericPropertyMatcher matcher) {
						matcher.ignoreCase();
					}
				});

		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.STARTING));
		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("firstname"), is(true));
		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("unknownProperty"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnFalseByDefault() {

		assertThat(exampleSpec.isIgnoreCaseEnabled(), is(false));
		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("firstname"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseIsEnabled() {

		exampleSpec = ExampleSpec.of(Person.class).withIgnoreCase();
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.isIgnoreCaseEnabled(), is(true));
		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldFavorPathSpecificSettings() {

		exampleSpec = ExampleSpec.of(Person.class).withIgnoreCase("firstname");
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpec.isIgnoreCaseEnabled(), is(false));
		assertThat(exampleSpecAccessor.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getValueTransformerForPathReturnsNoOpValueTransformerByDefault() {
		assertThat(exampleSpecAccessor.getValueTransformerForPath("firstname"),
				instanceOf(NoOpPropertyValueTransformer.class));
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

		ExampleSpec<Person> exampleSpec = ExampleSpec.of(Person.class).withTransformer("firstname", transformer);
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.getValueTransformerForPath("firstname"), equalTo(transformer));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsFalseIfNoneDefined() {
		assertThat(exampleSpecAccessor.hasPropertySpecifiers(), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsTrueWhenAtLeastOneIsSet() {

		ExampleSpec<Person> exampleSpec = ExampleSpec.of(Person.class).withStringMatcher(StringMatcher.STARTING)
				.withMatcher("firstname", new GenericPropertyMatcher().contains());
		exampleSpecAccessor = new ExampleSpecAccessor(exampleSpec);

		assertThat(exampleSpecAccessor.hasPropertySpecifiers(), is(true));
	}

	static class Person {

		String firstname;
	}

}
