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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Unit tests for {@link CachingValueExpressionEvaluatorFactory}.
 *
 * @author Mark Paluch
 */
class CachingValueExpressionEvaluatorFactoryUnitTests {

	@Test // GH-2369
	void shouldEvaluateWithDependencies() {

		EvaluationContextProvider contextProviderMock = mock(EvaluationContextProvider.class);
		CachingValueExpressionEvaluatorFactory factory = new CachingValueExpressionEvaluatorFactory(
				new SpelExpressionParser(), StandardEnvironment::new, contextProviderMock);
		ValueExpressionEvaluator evaluator = factory.create(new MyRoot("foo"));

		when(contextProviderMock.getEvaluationContext(any(), any(ExpressionDependencies.class)))
				.then(invocation -> new StandardEvaluationContext(invocation.getArgument(0)));

		Object result = evaluator.evaluate("#{root}");

		assertThat(result).isEqualTo("foo");

		ArgumentCaptor<ExpressionDependencies> captor = ArgumentCaptor.forClass(ExpressionDependencies.class);

		verify(contextProviderMock).getEvaluationContext(any(), captor.capture());

		ExpressionDependencies value = captor.getValue();
		assertThat(value).hasSize(1).isNotEqualTo(ExpressionDependencies.none());
	}

	record MyRoot(String root) {

	}

}
