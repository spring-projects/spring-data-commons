/*
 * Copyright 2024 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.env.Environment;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.util.ReactiveWrappers;
import org.springframework.util.Assert;

/**
 * Factory to create {@link QueryMethodValueEvaluationContextProvider} instances. Supports
 * {@link ReactiveQueryMethodValueEvaluationContextProvider} if the underlying
 * {@link QueryMethodEvaluationContextProvider} is a reactive one.
 *
 * @author Mark Paluch
 */
public class QueryMethodValueEvaluationContextProviderFactory {

	private final Environment environment;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	public QueryMethodValueEvaluationContextProviderFactory(Environment environment,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		this.environment = environment;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/**
	 * Creates a new {@link QueryMethodValueEvaluationContextProvider} for the given {@link Parameters}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @return a new {@link QueryMethodValueEvaluationContextProvider} for the given {@link Parameters}.
	 */
	public QueryMethodValueEvaluationContextProvider create(Parameters<?, ?> parameters) {

		Assert.notNull(parameters, "Parameters must not be null");

		if (ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)) {
			if (evaluationContextProvider instanceof ReactiveQueryMethodEvaluationContextProvider reactive) {
				return new DefaultReactiveQueryMethodValueEvaluationContextProvider(environment, parameters, reactive);
			}
		}

		return new DefaultQueryMethodValueEvaluationContextProvider(environment, parameters, evaluationContextProvider);
	}

	/**
	 * @author Mark Paluch
	 * @since 3.3
	 */
	static class DefaultQueryMethodValueEvaluationContextProvider implements QueryMethodValueEvaluationContextProvider {

		final Environment environment;
		final Parameters<?, ?> parameters;
		private final QueryMethodEvaluationContextProvider delegate;

		DefaultQueryMethodValueEvaluationContextProvider(Environment environment, Parameters<?, ?> parameters,
				QueryMethodEvaluationContextProvider delegate) {
			this.environment = environment;
			this.parameters = parameters;
			this.delegate = delegate;
		}

		@Override
		public ValueEvaluationContext getEvaluationContext(Object[] parameterValues, ExpressionDependencies dependencies) {
			return ValueEvaluationContext.of(environment,
					delegate.getEvaluationContext(parameters, parameterValues, dependencies));
		}
	}

	/**
	 * @author Mark Paluch
	 * @since 3.3
	 */
	static class DefaultReactiveQueryMethodValueEvaluationContextProvider extends
			DefaultQueryMethodValueEvaluationContextProvider implements ReactiveQueryMethodValueEvaluationContextProvider {

		private final ReactiveQueryMethodEvaluationContextProvider delegate;

		DefaultReactiveQueryMethodValueEvaluationContextProvider(Environment environment, Parameters<?, ?> parameters,
				ReactiveQueryMethodEvaluationContextProvider delegate) {
			super(environment, parameters, delegate);
			this.delegate = delegate;
		}

		@Override
		public Mono<ValueEvaluationContext> getEvaluationContextLater(Object[] parameterValues,
				ExpressionDependencies dependencies) {
			return delegate.getEvaluationContextLater(parameters, parameterValues, dependencies)
					.map(it -> ValueEvaluationContext.of(environment, it));
		}
	}
}
