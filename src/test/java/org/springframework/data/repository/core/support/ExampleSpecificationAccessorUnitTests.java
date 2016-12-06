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
package org.springframework.data.repository.core.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.*;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.startsWith;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.data.domain.ExampleMatcher.MatcherConfigurer;
import org.springframework.data.domain.ExampleMatcher.NoOpPropertyValueTransformer;
import org.springframework.data.domain.ExampleMatcher.NullHandler;
import org.springframework.data.domain.ExampleMatcher.PropertyValueTransformer;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;

/**
 * Unit tests for {@link ExampleMatcherAccessor}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Ron Spielman Trio - Fretboard Highway (Electric Tales)
 */
public class ExampleSpecificationAccessorUnitTests {

	Person person;
	ExampleMatcher specification;
	ExampleMatcherAccessor exampleSpecificationAccessor;

	@Before
	public void setUp() {

		person = new Person();
		person.firstname = "rand";

		specification = ExampleMatcher.matching();
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void defaultStringMatcherShouldReturnDefault() {
		assertThat(exampleSpecificationAccessor.getDefaultStringMatcher(), is(StringMatcher.DEFAULT));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnInclude() {

		specification = ExampleMatcher.matching().//
				withIncludeNullValues();
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getNullHandler(), is(NullHandler.INCLUDE));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldIgnorePaths() {

		specification = ExampleMatcher.matching().withIgnorePaths("firstname");
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoredPath("firstname"), is(true));
		assertThat(exampleSpecificationAccessor.isIgnoredPath("lastname"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatDoesNotHavePropertySpecifier() {
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"),
				is(specification.getDefaultStringMatcher()));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseConfiguredStringMatcherAsDefaultForPathThatDoesNotHavePropertySpecifier() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.CONTAINING);

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"), is(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase().//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseConfiguredIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase().//
				withMatcher("firstname", contains().caseSensitive());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname"), equalTo(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherStarting() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", startsWith());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.STARTING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherContaining() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherRegex() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", regex());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.REGEX));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldFavorStringMatcherDefinedForPathOverConfiguredDefaultStringMatcher() {

		specification = ExampleMatcher.matching().withStringMatcher(StringMatcher.ENDING)
				.withMatcher("firstname", contains()).withMatcher("address.city", startsWith())
				.withMatcher("lastname", new MatcherConfigurer<GenericPropertyMatcher>() {
					@Override
					public void configureMatcher(GenericPropertyMatcher matcher) {
						matcher.ignoreCase();
					}
				});

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getPropertySpecifiers(), hasSize(3));
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.CONTAINING));
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("lastname"), equalTo(StringMatcher.ENDING));
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("unknownProperty"), equalTo(StringMatcher.ENDING));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatHasPropertySpecifierWithoutStringMatcher() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", new MatcherConfigurer<GenericPropertyMatcher>() {
					@Override
					public void configureMatcher(GenericPropertyMatcher matcher) {
						matcher.ignoreCase();
					}
				});

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"), equalTo(StringMatcher.STARTING));
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname"), is(true));
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("unknownProperty"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnFalseByDefault() {

		assertThat(specification.isIgnoreCaseEnabled(), is(false));
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname"), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseIsEnabled() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase();

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseEnabled(), is(true));
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldFavorPathSpecificSettings() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase("firstname");

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(specification.isIgnoreCaseEnabled(), is(false));
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname"), is(true));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getValueTransformerForPathReturnsNoOpValueTransformerByDefault() {
		assertThat(exampleSpecificationAccessor.getValueTransformerForPath("firstname"),
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

		specification = ExampleMatcher.matching().//
				withTransformer("firstname", transformer);
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getValueTransformerForPath("firstname"), equalTo(transformer));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsFalseIfNoneDefined() {
		assertThat(exampleSpecificationAccessor.hasPropertySpecifiers(), is(false));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsTrueWhenAtLeastOneIsSet() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.hasPropertySpecifiers(), is(true));
	}

	/**
	 * @see DATACMNS-953
	 */
	@Test
	public void exactMatcherUsesExactMatching() {

		ExampleMatcher matcher = ExampleMatcher.matching()//
				.withMatcher("firstname", exact());

		assertThat(new ExampleMatcherAccessor(matcher).getPropertySpecifier("firstname").getStringMatcher(),
				is(StringMatcher.EXACT));
	}

	static class Person {
		String firstname;
	}
}
