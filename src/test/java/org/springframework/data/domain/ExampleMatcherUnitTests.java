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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.ExampleMatcher.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ExampleMatcher}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @soundtrack K2 - Der Berg Ruft (Club Mix)
 */
class ExampleMatcherUnitTests {

	ExampleMatcher matcher;

	@BeforeEach
	void setUp() {
		matcher = matching();
	}

	@Test // DATACMNS-810
	void defaultStringMatcherShouldReturnDefault() {
		assertThat(matcher.getDefaultStringMatcher()).isEqualTo(StringMatcher.DEFAULT);
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnFalseByDefault() {
		assertThat(matcher.isIgnoreCaseEnabled()).isFalse();
	}

	@Test // DATACMNS-810
	void ignoredPathsIsEmptyByDefault() {
		assertThat(matcher.getIgnoredPaths()).isEmpty();
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnIgnoreByDefault() {
		assertThat(matcher.getNullHandler()).isEqualTo(NullHandler.IGNORE);
	}

	@Test // DATACMNS-810
	void ignoredPathsIsNotModifiable() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> matcher.getIgnoredPaths().add("¯\\_(ツ)_/¯"));
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnTrueWhenIgnoreCaseEnabled() {

		matcher = matching().withIgnoreCase();

		assertThat(matcher.isIgnoreCaseEnabled()).isTrue();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnTrueWhenIgnoreCaseSet() {

		matcher = matching().withIgnoreCase(true);

		assertThat(matcher.isIgnoreCaseEnabled()).isTrue();
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnInclude() {

		matcher = matching().withIncludeNullValues();

		assertThat(matcher.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnIgnore() {

		matcher = matching().withIgnoreNullValues();

		assertThat(matcher.getNullHandler()).isEqualTo(NullHandler.IGNORE);
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnConfiguredValue() {

		matcher = matching().withNullHandler(NullHandler.INCLUDE);

		assertThat(matcher.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	@Test // DATACMNS-810
	void ignoredPathsShouldReturnCorrectProperties() {

		matcher = matching().withIgnorePaths("foo", "bar", "baz");

		assertThat(matcher.getIgnoredPaths()).contains("foo", "bar", "baz");
		assertThat(matcher.getIgnoredPaths()).hasSize(3);
	}

	@Test // DATACMNS-810
	void ignoredPathsShouldReturnUniqueProperties() {

		matcher = matching().withIgnorePaths("foo", "bar", "foo");

		assertThat(matcher.getIgnoredPaths()).contains("foo", "bar");
		assertThat(matcher.getIgnoredPaths()).hasSize(2);
	}

	@Test // DATACMNS-810
	void withCreatesNewInstance() {

		matcher = matching().withIgnorePaths("foo", "bar", "foo");
		ExampleMatcher configuredExampleSpec = matcher.withIgnoreCase();

		assertThat(matcher).isNotSameAs(configuredExampleSpec);
		assertThat(matcher.getIgnoredPaths()).hasSize(2);
		assertThat(matcher.isIgnoreCaseEnabled()).isFalse();

		assertThat(configuredExampleSpec.getIgnoredPaths()).hasSize(2);
		assertThat(configuredExampleSpec.isIgnoreCaseEnabled()).isTrue();
	}

	@Test // DATACMNS-879
	void defaultMatcherRequiresAllMatching() {

		assertThat(matching().isAllMatching()).isTrue();
		assertThat(matching().isAnyMatching()).isFalse();
	}

	@Test // DATACMNS-879
	void allMatcherRequiresAllMatching() {

		assertThat(matchingAll().isAllMatching()).isTrue();
		assertThat(matchingAll().isAnyMatching()).isFalse();
	}

	@Test // DATACMNS-879
	void anyMatcherYieldsAnyMatching() {

		assertThat(matchingAny().isAnyMatching()).isTrue();
		assertThat(matchingAny().isAllMatching()).isFalse();
	}

	@Test // DATACMNS-900
	void shouldCompareUsingHashCodeAndEquals() {

		matcher = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withIgnoreCase("ignored-case") //
				.withMatcher("hello", GenericPropertyMatchers.contains().caseSensitive()) //
				.withMatcher("world", GenericPropertyMatcher::endsWith);

		ExampleMatcher sameAsMatcher = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withIgnoreCase("ignored-case") //
				.withMatcher("hello", GenericPropertyMatchers.contains().caseSensitive()) //
				.withMatcher("world", GenericPropertyMatcher::endsWith);

		ExampleMatcher different = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withMatcher("hello", GenericPropertyMatchers.contains().ignoreCase());

		assertThat(matcher.hashCode()).isEqualTo(sameAsMatcher.hashCode()).isNotEqualTo(different.hashCode());
		assertThat(matcher).isEqualTo(sameAsMatcher).isNotEqualTo(different);
	}

	static class Person {

		String firstname;
	}
}
