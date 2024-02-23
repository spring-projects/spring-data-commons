/*
 * Copyright 2014-2024 the original author or authors.
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

import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.expression.ReactiveValueEvaluationContextProvider;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ExtensionIdAware;
import org.springframework.expression.EvaluationContext;

/**
 * An reactive {@link QueryMethodEvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} and {@link org.springframework.data.spel.spi.ReactiveEvaluationContextExtension}.
 * instances.
 *
 * @author Mark Paluch
 * @since 2.4
 * @deprecated since 3.4 in favor of {@link QueryMethodValueEvaluationContextAccessor}.
 */
@Deprecated(since = "3.4")
public class ReactiveExtensionAwareQueryMethodEvaluationContextProvider
		extends ExtensionAwareQueryMethodEvaluationContextProvider
		implements ReactiveQueryMethodEvaluationContextProvider {

	/**
	 * Create a new {@link ReactiveExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	public ReactiveExtensionAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * Create a new {@link ReactiveExtensionAwareQueryMethodEvaluationContextProvider} using the given
	 * {@link EvaluationContextExtension}s and
	 * {@link org.springframework.data.spel.spi.ReactiveEvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ReactiveExtensionAwareQueryMethodEvaluationContextProvider(List<? extends ExtensionIdAware> extensions) {
		super(new QueryMethodValueEvaluationContextAccessor(QueryMethodValueEvaluationContextAccessor.ENVIRONMENT,
				extensions));
	}

	/**
	 * Creates a new {@link ReactiveExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param evaluationContextProvider to lookup the {@link EvaluationContextExtension}s from, must not be
	 *          {@literal null}.
	 */
	public ReactiveExtensionAwareQueryMethodEvaluationContextProvider(
			EvaluationContextProvider evaluationContextProvider) {
		super(new QueryMethodValueEvaluationContextAccessor(QueryMethodValueEvaluationContextAccessor.ENVIRONMENT,
				evaluationContextProvider));
	}

	@Override
	public <T extends Parameters<?, ?>> Mono<EvaluationContext> getEvaluationContextLater(T parameters,
			Object[] parameterValues) {

		return createProvider(parameters).getEvaluationContextLater(parameterValues)
				.map(ValueEvaluationContext::getRequiredEvaluationContext);
	}

	@Override
	public <T extends Parameters<?, ?>> Mono<EvaluationContext> getEvaluationContextLater(T parameters,
			Object[] parameterValues, ExpressionDependencies dependencies) {

		return createProvider(parameters).getEvaluationContextLater(parameterValues, dependencies)
				.map(ValueEvaluationContext::getRequiredEvaluationContext);
	}

	private ReactiveValueEvaluationContextProvider createProvider(Parameters<?, ?> parameters) {
		return (ReactiveValueEvaluationContextProvider) getDelegate().create(parameters);
	}
}
