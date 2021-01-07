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

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SpelQueryContext}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
class SpelQueryContextUnitTests {

	static final QueryMethodEvaluationContextProvider EVALUATION_CONTEXT_PROVIDER = QueryMethodEvaluationContextProvider.DEFAULT;
	static final BiFunction<Integer, String, String> PARAMETER_NAME_SOURCE = (index, spel) -> "__$synthetic$__" + index;
	static final BiFunction<String, String, String> REPLACEMENT_SOURCE = (prefix, name) -> prefix + name;

	@Test // DATACMNS-1258
	void nullParameterNameSourceThrowsException() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> SpelQueryContext.of(null, REPLACEMENT_SOURCE));
	}

	@Test // DATACMNS-1258
	void nullReplacementSourceThrowsException() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> SpelQueryContext.of(PARAMETER_NAME_SOURCE, null));
	}

	@Test // DATACMNS-1258
	void rejectsNullEvaluationContextProvider() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> context.withEvaluationContextProvider(null));
	}

	@Test // DATACMNS-1258
	void createsEvaluatingContextUsingProvider() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);

		assertThat(context.withEvaluationContextProvider(EVALUATION_CONTEXT_PROVIDER)).isNotNull();
	}

	@Test // DATACMNS-1683
	void reportsQuotationCorrectly() {

		SpelQueryContext context = SpelQueryContext.of(PARAMETER_NAME_SOURCE, REPLACEMENT_SOURCE);

		SpelQueryContext.SpelExtractor extractor = context.parse(
				"select n from NetworkServer n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',:#{#networkRequest.name},'%')), '')) OR :#{#networkRequest.name} IS NULL )");

		assertThat(extractor.getQueryString()).isEqualTo(
				"select n from NetworkServer n where (LOWER(n.name) LIKE LOWER(NULLIF(text(concat('%',:__$synthetic$__0,'%')), '')) OR :__$synthetic$__1 IS NULL )");
		assertThat(extractor.isQuoted(extractor.getQueryString().indexOf(":__$synthetic$__0"))).isFalse();
		assertThat(extractor.isQuoted(extractor.getQueryString().indexOf(":__$synthetic$__1"))).isFalse();
	}
}
