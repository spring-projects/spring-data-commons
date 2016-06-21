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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.ExampleMatcher;
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
		assertThat(exampleSpecificationAccessor.getDefaultStringMatcher()).isEqualTo(StringMatcher.DEFAULT);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void nullHandlerShouldReturnInclude() {

		specification = ExampleMatcher.matching().//
				withIncludeNullValues();
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldIgnorePaths() {

		specification = ExampleMatcher.matching().withIgnorePaths("firstname");
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoredPath("firstname")).isTrue();
		assertThat(exampleSpecificationAccessor.isIgnoredPath("lastname")).isFalse();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatDoesNotHavePropertySpecifier() {
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(specification.getDefaultStringMatcher());
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseConfiguredStringMatcherAsDefaultForPathThatDoesNotHavePropertySpecifier() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.CONTAINING);

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.CONTAINING);
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

		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
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

		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isFalse();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherStarting() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", startsWith());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.STARTING);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherContaining() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.CONTAINING);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherRegex() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", regex());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.REGEX);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldFavorStringMatcherDefinedForPathOverConfiguredDefaultStringMatcher() {

		specification = ExampleMatcher.matching().withStringMatcher(StringMatcher.ENDING)
				.withMatcher("firstname", contains()).withMatcher("address.city", startsWith())
				.withMatcher("lastname", matcher -> matcher.ignoreCase());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getPropertySpecifiers()).hasSize(3);
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.CONTAINING);
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("lastname")).isEqualTo(StringMatcher.ENDING);
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("unknownProperty")).isEqualTo(StringMatcher.ENDING);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void exampleShouldUseDefaultStringMatcherForPathThatHasPropertySpecifierWithoutStringMatcher() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", matcher -> matcher.ignoreCase());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.STARTING);
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("unknownProperty")).isFalse();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnFalseByDefault() {

		assertThat(specification.isIgnoreCaseEnabled()).isFalse();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isFalse();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldReturnTrueWhenIgnoreCaseIsEnabled() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase();

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseEnabled()).isTrue();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void ignoreCaseShouldFavorPathSpecificSettings() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase("firstname");

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(specification.isIgnoreCaseEnabled()).isFalse();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getValueTransformerForPathReturnsNoOpValueTransformerByDefault() {
		assertThat(exampleSpecificationAccessor.getValueTransformerForPath("firstname"))
				.isInstanceOf(NoOpPropertyValueTransformer.class);
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

		assertThat(exampleSpecificationAccessor.getValueTransformerForPath("firstname")).isEqualTo(transformer);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void hasPropertySpecifiersReturnsFalseIfNoneDefined() {
		assertThat(exampleSpecificationAccessor.hasPropertySpecifiers()).isFalse();
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

		assertThat(exampleSpecificationAccessor.hasPropertySpecifiers()).isTrue();
	}

	static class Person {
		String firstname;
	}
}
