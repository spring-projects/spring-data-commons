/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.data.domain.ExampleMatcher.NoOpPropertyValueTransformer;
import org.springframework.data.domain.ExampleMatcher.NullHandler;
import org.springframework.data.domain.ExampleMatcher.PropertyValueTransformer;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.support.ExampleMatcherAccessor;

/**
 * Unit tests for {@link ExampleMatcherAccessor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Ron Spielman Trio - Fretboard Highway (Electric Tales)
 */
class ExampleSpecificationAccessorUnitTests {

	Person person;
	ExampleMatcher specification;
	ExampleMatcherAccessor exampleSpecificationAccessor;

	@BeforeEach
	void setUp() {

		person = new Person();
		person.firstname = "rand";

		specification = ExampleMatcher.matching();
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);
	}

	@Test // DATACMNS-810
	void defaultStringMatcherShouldReturnDefault() {
		assertThat(exampleSpecificationAccessor.getDefaultStringMatcher()).isEqualTo(StringMatcher.DEFAULT);
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnInclude() {

		specification = ExampleMatcher.matching().//
				withIncludeNullValues();
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	@Test // DATACMNS-810
	void exampleShouldIgnorePaths() {

		specification = ExampleMatcher.matching().withIgnorePaths("firstname");
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoredPath("firstname")).isTrue();
		assertThat(exampleSpecificationAccessor.isIgnoredPath("lastname")).isFalse();
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefaultStringMatcherForPathThatDoesNotHavePropertySpecifier() {
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(specification.getDefaultStringMatcher());
	}

	@Test // DATACMNS-810
	void exampleShouldUseConfiguredStringMatcherAsDefaultForPathThatDoesNotHavePropertySpecifier() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.CONTAINING);

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.CONTAINING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefaultIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase().//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	@Test // DATACMNS-810
	void exampleShouldUseConfiguredIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase().//
				withMatcher("firstname", contains().caseSensitive());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isFalse();
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherStarting() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", startsWith());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.STARTING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherContaining() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.CONTAINING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherRegex() {

		specification = ExampleMatcher.matching().//
				withMatcher("firstname", regex());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.REGEX);
	}

	@Test // DATACMNS-810
	void exampleShouldFavorStringMatcherDefinedForPathOverConfiguredDefaultStringMatcher() {

		specification = ExampleMatcher.matching().withStringMatcher(StringMatcher.ENDING)
				.withMatcher("firstname", contains()).withMatcher("address.city", startsWith())
				.withMatcher("lastname", GenericPropertyMatcher::ignoreCase);

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getPropertySpecifiers()).hasSize(3);
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.CONTAINING);
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("lastname")).isEqualTo(StringMatcher.ENDING);
		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("unknownProperty")).isEqualTo(StringMatcher.ENDING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefaultStringMatcherForPathThatHasPropertySpecifierWithoutStringMatcher() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", GenericPropertyMatcher::ignoreCase);

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getStringMatcherForPath("firstname")).isEqualTo(StringMatcher.STARTING);
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("unknownProperty")).isFalse();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnFalseByDefault() {

		assertThat(specification.isIgnoreCaseEnabled()).isFalse();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isFalse();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnTrueWhenIgnoreCaseIsEnabled() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase();

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.isIgnoreCaseEnabled()).isTrue();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldFavorPathSpecificSettings() {

		specification = ExampleMatcher.matching().//
				withIgnoreCase("firstname");

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(specification.isIgnoreCaseEnabled()).isFalse();
		assertThat(exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	@Test // DATACMNS-810
	void getValueTransformerForPathReturnsNoOpValueTransformerByDefault() {
		assertThat(exampleSpecificationAccessor.getValueTransformerForPath("firstname"))
				.isInstanceOf(NoOpPropertyValueTransformer.class);
	}

	@Test // DATACMNS-810
	void getValueTransformerForPathReturnsConfigurtedTransformerForPath() {

		PropertyValueTransformer transformer = source -> source.map(Object::toString);

		specification = ExampleMatcher.matching().//
				withTransformer("firstname", transformer);
		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.getValueTransformerForPath("firstname")).isEqualTo(transformer);
	}

	@Test // DATACMNS-810
	void hasPropertySpecifiersReturnsFalseIfNoneDefined() {
		assertThat(exampleSpecificationAccessor.hasPropertySpecifiers()).isFalse();
	}

	@Test // DATACMNS-810
	void hasPropertySpecifiersReturnsTrueWhenAtLeastOneIsSet() {

		specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", contains());

		exampleSpecificationAccessor = new ExampleMatcherAccessor(specification);

		assertThat(exampleSpecificationAccessor.hasPropertySpecifiers()).isTrue();
	}

	@Test // DATACMNS-953
	void exactMatcherUsesExactMatching() {

		ExampleMatcher matcher = ExampleMatcher.matching()//
				.withMatcher("firstname", exact());

		assertThat(new ExampleMatcherAccessor(matcher).getPropertySpecifier("firstname").getStringMatcher())
				.isEqualTo(StringMatcher.EXACT);
	}

	static class Person {
		String firstname;
	}
}
