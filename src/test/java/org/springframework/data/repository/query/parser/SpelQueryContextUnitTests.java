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

import java.util.function.BiFunction;

import org.junit.Test;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

/**
 * Unit tests for {@link SpelQueryContext}.
 * 
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class SpelQueryContextUnitTests {

	static final QueryMethodEvaluationContextProvider EVALUATION_CONTEXT_PROVIDER = QueryMethodEvaluationContextProvider.DEFAULT;
	static final String EXPRESSION_PARAMETER_PREFIX = "EPP";
	static final BiFunction<Integer, String, String> PARAMETER_NAME_SOURCE = (index, spel) -> EXPRESSION_PARAMETER_PREFIX
			+ index;
	static final BiFunction<String, String, String> REPLACEMENT_SOURCE = (prefix, name) -> prefix + name;

	@Test // DATACMNS-1258
	public void nullParameterNameSourceThrowsException() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> SpelQueryContext.of(EVALUATION_CONTEXT_PROVIDER, null, REPLACEMENT_SOURCE));
	}

	@Test // DATACMNS-1258
	public void nullReplacementSourceThrowsException() {

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> SpelQueryContext.of(EVALUATION_CONTEXT_PROVIDER, PARAMETER_NAME_SOURCE, null));
	}
}
