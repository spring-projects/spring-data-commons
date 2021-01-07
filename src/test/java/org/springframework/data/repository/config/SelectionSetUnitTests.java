/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.repository.config;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SelectionSet}
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 */
class SelectionSetUnitTests {

	@Test // DATACMNS-764
	void returnsUniqueResult() {
		assertThat(SelectionSet.of(singleton("single value")).uniqueResult()).hasValue("single value");
	}

	@Test // DATACMNS-764
	void emptyCollectionReturnsNull() {
		assertThat(SelectionSet.of(emptySet()).uniqueResult()).isEmpty();
	}

	@Test // DATACMNS-764
	void multipleElementsThrowException() {
		assertThatIllegalStateException().isThrownBy(() -> SelectionSet.of(asList("one", "two")).uniqueResult());
	}

	@Test // DATACMNS-764
	void throwsCustomExceptionWhenConfigured() {

		assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
			SelectionSet.of(asList("one", "two"), c -> {
				throw new NullPointerException();
			}).uniqueResult();
		});
	}

	@Test // DATACMNS-764
	void usesFallbackWhenConfigured() {
		assertThat(SelectionSet.of(asList("one", "two"), c -> Optional.of(String.valueOf(c.size()))).uniqueResult())
				.hasValue("2");
	}

	@Test // DATACMNS-764
	void returnsUniqueResultAfterFilter() {

		SelectionSet<String> selection = SelectionSet.of(asList("one", "two", "three"))
				.filterIfNecessary(s -> s.contains("w"));

		assertThat(selection.uniqueResult()).hasValue("two");
	}

	@Test // DATACMNS-764
	void ignoresFilterWhenResultIsAlreadyUnique() {

		SelectionSet<String> selection = SelectionSet.of(asList("one")).filterIfNecessary(s -> s.contains("w"));

		assertThat(selection.uniqueResult()).hasValue("one");
	}
}
