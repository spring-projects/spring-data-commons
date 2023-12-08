/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.SpelQueryContext.SpelExtractor;

/**
 * Unit tests for {@link SpelEvaluator}.
 *
 * @author Mark Paluch
 */
class SpelEvaluatorUnitTests {

	final SpelQueryContext context = SpelQueryContext.of((counter, s) -> String.format("__$synthetic$__%d", counter + 1),
			String::concat);

	@Test // GH-2904
	void shouldEvaluateExpression() throws Exception {

		SpelExtractor extractor = context.parse("SELECT :#{#value}");
		Method method = MyRepository.class.getDeclaredMethod("simpleExpression", String.class);
		SpelEvaluator evaluator = new SpelEvaluator(QueryMethodEvaluationContextProvider.DEFAULT,
				new DefaultParameters(ParametersSource.of(method)), extractor);

		assertThat(evaluator.getQueryString()).isEqualTo("SELECT :__$synthetic$__1");
		assertThat(evaluator.evaluate(new Object[] { "hello" })).containsEntry("__$synthetic$__1", "hello");
	}

	@Test // GH-2904
	void shouldAllowNullValues() throws Exception {

		SpelExtractor extractor = context.parse("SELECT :#{#value}");
		Method method = MyRepository.class.getDeclaredMethod("simpleExpression", String.class);
		SpelEvaluator evaluator = new SpelEvaluator(QueryMethodEvaluationContextProvider.DEFAULT,
				new DefaultParameters(ParametersSource.of(method)), extractor);

		assertThat(evaluator.getQueryString()).isEqualTo("SELECT :__$synthetic$__1");
		assertThat(evaluator.evaluate(new Object[] { null })).containsEntry("__$synthetic$__1", null);
	}

	interface MyRepository {

		void simpleExpression(String value);

	}
}
