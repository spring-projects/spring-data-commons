/*
 * Copyright 2011-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link SearchResult}.
 *
 * @author Mark Paluch
 */
class SearchResultUnitTests {

	SearchResult<String> first = new SearchResult<>("Foo", Score.of(2.5));
	SearchResult<String> second = new SearchResult<>("Foo", Score.of(2.5));
	SearchResult<String> third = new SearchResult<>("Bar", Score.of(2.5));
	SearchResult<String> fourth = new SearchResult<>("Foo", Score.of(5.2));

	@Test // GH-3285
	void considersSameInstanceEqual() {
		assertThat(first.equals(first)).isTrue();
	}

	@Test // GH-3285
	void considersSameValuesAsEqual() {

		assertThat(first.equals(second)).isTrue();
		assertThat(second.equals(first)).isTrue();
		assertThat(first.equals(third)).isFalse();
		assertThat(third.equals(first)).isFalse();
		assertThat(first.equals(fourth)).isFalse();
		assertThat(fourth.equals(first)).isFalse();
	}

	@Test // GH-3285
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void rejectsNullContent() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SearchResult(null, Score.of(2.5)));
	}

	@Test // GH-3285
	@SuppressWarnings("unchecked")
	void testSerialization() {

		var result = new SearchResult<>("test", Score.of(2d));

		var serialized = (SearchResult<String>) SerializationUtils.deserialize(SerializationUtils.serialize(result));
		assertThat(serialized).isEqualTo(result);
	}

}
