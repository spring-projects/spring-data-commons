/*
 * Copyright 2025 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link SearchResults}.
 *
 * @author Mark Paluch
 */
class SearchResultsUnitTests {

	@SuppressWarnings("unchecked")
	@Test // GH-3285
	void testSerialization() {

		var result = new SearchResult<>("test", Score.of(2));
		var searchResults = new SearchResults<>(Collections.singletonList(result));

		var serialized = (SearchResults<String>) SerializationUtils
				.deserialize(SerializationUtils.serialize(searchResults));
		assertThat(serialized).isEqualTo(searchResults);
	}

	@SuppressWarnings("unchecked")
	@Test // GH-3285
	void testStream() {

		var result = new SearchResult<>("test", Score.of(2));
		var searchResults = new SearchResults<>(Collections.singletonList(result));

		List<SearchResult<String>> list = searchResults.stream().toList();
		assertThat(list).isEqualTo(searchResults.getContent());
	}

	@SuppressWarnings("unchecked")
	@Test // GH-3285
	void testContentStream() {

		var result = new SearchResult<>("test", Score.of(2));
		var searchResults = new SearchResults<>(Collections.singletonList(result));

		List<String> list = searchResults.contentStream().toList();
		assertThat(list).isEqualTo(Arrays.asList(result.getContent()));
	}

}
