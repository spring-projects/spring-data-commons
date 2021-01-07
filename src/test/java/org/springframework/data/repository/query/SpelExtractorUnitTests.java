/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.SpelQueryContext.SpelExtractor;

/**
 * Unit tests for {@link SpelExtractor}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 */
class SpelExtractorUnitTests {

	static final BiFunction<Integer, String, String> PARAMETER_NAME_SOURCE = (index, spel) -> "EPP" + index;
	static final BiFunction<String, String, String> REPLACEMENT_SOURCE = (prefix, name) -> prefix + name;

	final SoftAssertions softly = new SoftAssertions();

	@Test // DATACMNS-1258
	void nullQueryThrowsException() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);

		assertThatIllegalArgumentException().isThrownBy(() -> context.parse(null));
	}

	@Test // DATACMNS-1258
	void emptyStringGetsParsedCorrectly() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		SpelExtractor extractor = context.parse("");

		softly.assertThat(extractor.getQueryString()).isEqualTo("");
		softly.assertThat(extractor.getParameterMap()).isEmpty();

		softly.assertAll();
	}

	@Test // DATACMNS-1258
	void findsAndReplacesExpressions() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		SpelExtractor extractor = context.parse(":#{one} ?#{two}");

		softly.assertThat(extractor.getQueryString()).isEqualTo(":EPP0 ?EPP1");
		softly.assertThat(extractor.getParameterMap().entrySet()) //
				.extracting(Map.Entry::getKey, Map.Entry::getValue) //
				.containsExactlyInAnyOrder( //
						Tuple.tuple("EPP0", "one"), //
						Tuple.tuple("EPP1", "two") //
				);

		softly.assertAll();
	}

	@Test // DATACMNS-1258
	void keepsStringWhenNoMatchIsFound() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		SpelExtractor extractor = context.parse("abcdef");

		softly.assertThat(extractor.getQueryString()).isEqualTo("abcdef");
		softly.assertThat(extractor.getParameterMap()).isEmpty();

		softly.assertAll();
	}

	@Test // DATACMNS-1258
	void spelsInQuotesGetIgnored() {

		List<String> queries = Arrays.asList(//
				"a'b:#{one}cd'ef", //
				"a'b:#{o'ne}cdef", //
				"ab':#{one}'cdef", //
				"ab:'#{one}cd'ef", //
				"ab:#'{one}cd'ef", //
				"a'b:#{o'ne}cdef");

		queries.forEach(this::checkNoSpelIsFound);

		softly.assertAll();
	}

	private void checkNoSpelIsFound(String query) {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		SpelExtractor extractor = context.parse(query);

		softly.assertThat(extractor.getQueryString()).describedAs(query).isEqualTo(query);
		softly.assertThat(extractor.getParameterMap()).describedAs(query).isEmpty();
	}
}
