/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.query.parser;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

/**
 * Unit tests for {@link SpelQueryContext} and
 * {@link org.springframework.data.repository.query.parser.SpelQueryContext.SpelExtractor}
 * 
 * @author Jens Schauder
 */
public class SpelExtractorUnitTests {

	static final String EXPRESSION_PARAMETER_PREFIX = "EPP";
	static final BiFunction<Integer, String, String> PARAMETER_NAME_SOURCE = (index, spel) -> EXPRESSION_PARAMETER_PREFIX + index;
	static final BiFunction<String, String, String> REPLACEMENT_SOURCE = (prefix, name) -> prefix + name;
	final SoftAssertions softly = new SoftAssertions();

	@Test // DATACMNS-1258
	public void nullQueryThrowsException() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> new SpelQueryContext( //
						PARAMETER_NAME_SOURCE, //
						REPLACEMENT_SOURCE) //
								.parse(null) //
		);
	}

	@Test // DATACMNS-1258
	public void nullParameterNameSourceThrowsException() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> new SpelQueryContext( //
						null, //
						REPLACEMENT_SOURCE) //
		);
	}

	@Test // DATACMNS-1258
	public void nullReplacementSourceThrowsException() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy( //
						() -> new SpelQueryContext( //
								PARAMETER_NAME_SOURCE, //
								null) //
		);
	}

	@Test // DATACMNS-1258
	public void emptyStringGetsParsedCorrectly() {

		SpelQueryContext.SpelExtractor extractor = new SpelQueryContext( //
				PARAMETER_NAME_SOURCE, //
				REPLACEMENT_SOURCE //
		).parse("");

		softly.assertThat(extractor.query()).isEqualTo("");
		softly.assertThat(extractor.parameterNameToSpelMap()).isEmpty();

		softly.assertAll();
	}

	@Test // DATACMNS-1258
	public void findsAndReplacesExpressions() {

		SpelQueryContext.SpelExtractor extractor = new SpelQueryContext( //
				PARAMETER_NAME_SOURCE, //
				REPLACEMENT_SOURCE //
		).parse(":#{one} ?#{two}");

		softly.assertThat(extractor.query()).isEqualTo(":EPP0 ?EPP1");
		softly.assertThat(extractor.parameterNameToSpelMap().entrySet()) //
				.extracting(Map.Entry::getKey, Map.Entry::getValue) //
				.containsExactlyInAnyOrder( //
						Tuple.tuple("EPP0", "one"), //
						Tuple.tuple("EPP1", "two") //
		);

		softly.assertAll();
	}

	@Test // DATACMNS-1258
	public void keepsStringWhenNoMatchIsFound() {

		SpelQueryContext.SpelExtractor extractor = new SpelQueryContext( //
				PARAMETER_NAME_SOURCE, //
				REPLACEMENT_SOURCE //
		).parse("abcdef");

		softly.assertThat(extractor.query()).isEqualTo("abcdef");
		softly.assertThat(extractor.parameterNameToSpelMap()).isEmpty();

		softly.assertAll();
	}

	@Test // DATACMNS-1258
	public void spelsInQuotesGetIgnored() {

		List<String> queries = Arrays.asList("a'b:#{one}cd'ef", "a'b:#{o'ne}cdef", "ab':#{one}'cdef", "ab:'#{one}cd'ef",
				"ab:#'{one}cd'ef", "a'b:#{o'ne}cdef");

		for (String query : queries) {
			checkNoSpelIsFound(query);
		}

		softly.assertAll();
	}

	private void checkNoSpelIsFound(String query) {

		SpelQueryContext.SpelExtractor extractor = new SpelQueryContext( //
				PARAMETER_NAME_SOURCE, //
				REPLACEMENT_SOURCE //
		).parse(query);

		softly.assertThat(extractor.query()).describedAs(query).isEqualTo(query);
		softly.assertThat(extractor.parameterNameToSpelMap()).describedAs(query).isEmpty();
	}

}
