/*
 * Copyright 2024-present the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.spel.EvaluationContextProvider;

/**
 * Unit tests for {@link ValueExpressionQueryRewriter}.
 *
 * @author Mark Paluch
 */
class ValueExpressionQueryRewriterUnitTests {

	static final BiFunction<Integer, String, String> PARAMETER_NAME_SOURCE = (index, spel) -> "EPP" + index;
	static final BiFunction<String, String, String> REPLACEMENT_SOURCE = (prefix, name) -> prefix + name;
	static final ValueExpressionParser PARSER = ValueExpressionParser.create();

	@Test // GH-3049
	void nullQueryThrowsException() {

		var context = ValueExpressionQueryRewriter.of(PARSER, PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);

		assertThatIllegalArgumentException().isThrownBy(() -> context.parse(null));
	}

	@Test // GH-3049
	void emptyStringGetsParsedCorrectly() {

		var context = ValueExpressionQueryRewriter.of(PARSER, PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		var extractor = context.parse("");

		assertThat(extractor.getQueryString()).isEqualTo("");
		assertThat(extractor.getParameterMap()).isEmpty();
	}

	@Test // GH-3049
	void findsAndReplacesExpressions() {

		var context = ValueExpressionQueryRewriter.of(PARSER, PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		var extractor = context.parse(":#{one} ?#{two} :${three} ?${four}");

		assertThat(extractor.getQueryString()).isEqualTo(":EPP0 ?EPP1 :EPP2 ?EPP3");
		assertThat(extractor.getParameterMap().entrySet()) //
				.extracting(Map.Entry::getKey, it -> it.getValue().getExpressionString()) //
				.containsExactlyInAnyOrder( //
						Tuple.tuple("EPP0", "one"), //
						Tuple.tuple("EPP1", "two"), //
						Tuple.tuple("EPP2", "${three}"), //
						Tuple.tuple("EPP3", "${four}") //
				);
	}

	@Test // GH-3049
	void keepsStringWhenNoMatchIsFound() {

		var context = ValueExpressionQueryRewriter.of(PARSER, PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		var extractor = context.parse("abcdef");

		assertThat(extractor.getQueryString()).isEqualTo("abcdef");
		assertThat(extractor.getParameterMap()).isEmpty();
	}

	@Test // GH-3049
	void spelsInQuotesGetIgnored() {

		var queries = Arrays.asList(//
				"a'b:#{one}cd'ef", //
				"a'b:#{o'ne}cdef", //
				"ab':#{one}'cdef", //
				"ab:'#{one}cd'ef", //
				"ab:#'{one}cd'ef", //
				"a'b:#{o'ne}cdef");

		queries.forEach(this::checkNoExpressionIsFound);
	}

	private void checkNoExpressionIsFound(String query) {

		var context = ValueExpressionQueryRewriter.of(PARSER, PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);
		var extractor = context.parse(query);

		assertThat(extractor.getQueryString()).describedAs(query).isEqualTo(query);
		assertThat(extractor.getParameterMap()).describedAs(query).isEmpty();
	}

	@Test // GH-3049
	void shouldEvaluateExpression() throws Exception {

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("synthetic", Map.of("foo", "world")));

		QueryMethodValueEvaluationContextAccessor contextAccessor = new QueryMethodValueEvaluationContextAccessor(
				environment,
				EvaluationContextProvider.DEFAULT);

		ValueExpressionDelegate delegate = new ValueExpressionDelegate(contextAccessor, PARSER);
		ValueExpressionQueryRewriter.EvaluatingValueExpressionQueryRewriter rewriter = ValueExpressionQueryRewriter
				.of(delegate, PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);

		Method method = ValueExpressionQueryRewriterUnitTests.MyRepository.class.getDeclaredMethod("simpleExpression",
				String.class);
		var extractor = rewriter.parse("SELECT :#{#value}, :${foo}",
				new DefaultParameters(ParametersSource.of(method)));

		assertThat(extractor.getQueryString()).isEqualTo("SELECT :EPP0, :EPP1");
		assertThat(extractor.evaluate(new Object[] { "hello" })).containsEntry("EPP0", "hello").containsEntry("EPP1",
				"world");
	}

	@Test // GH-3049
	void shouldAllowNullValues() throws Exception {

		ValueExpressionQueryRewriter rewriter = ValueExpressionQueryRewriter.of(PARSER, PARAMETER_NAME_SOURCE,
				REPLACEMENT_SOURCE);
		StandardEnvironment environment = new StandardEnvironment();

		QueryMethodValueEvaluationContextAccessor factory = new QueryMethodValueEvaluationContextAccessor(environment,
				EvaluationContextProvider.DEFAULT);

		Method method = ValueExpressionQueryRewriterUnitTests.MyRepository.class.getDeclaredMethod("simpleExpression",
				String.class);
		var extractor = rewriter.withEvaluationContextAccessor(factory).parse("SELECT :#{#value}",
				new DefaultParameters(ParametersSource.of(method)));

		assertThat(extractor.getQueryString()).isEqualTo("SELECT :EPP0");
		assertThat(extractor.evaluate(new Object[] { null })).containsEntry("EPP0", null);
	}

	interface MyRepository {

		void simpleExpression(String value);

	}
}
