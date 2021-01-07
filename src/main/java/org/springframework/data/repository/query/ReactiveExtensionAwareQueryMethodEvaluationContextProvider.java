/*
 * Copyright 2014-2021 the original author or authors.
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
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.spel.ReactiveExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ExtensionIdAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * An reactive {@link QueryMethodEvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} and {@link org.springframework.data.spel.spi.ReactiveEvaluationContextExtension}.
 * instances.
 *
 * @author Mark Paluch
 * @since 2.4
 */
public class ReactiveExtensionAwareQueryMethodEvaluationContextProvider
		implements ReactiveQueryMethodEvaluationContextProvider {

	private final ReactiveExtensionAwareEvaluationContextProvider delegate;

	/**
	 * Create a new {@link ReactiveExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	public ReactiveExtensionAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {

		Assert.notNull(beanFactory, "ListableBeanFactory must not be null!");

		this.delegate = new ReactiveExtensionAwareEvaluationContextProvider(beanFactory);
	}

	/**
	 * Create a new {@link ReactiveExtensionAwareQueryMethodEvaluationContextProvider} using the given
	 * {@link EvaluationContextExtension}s and
	 * {@link org.springframework.data.spel.spi.ReactiveEvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ReactiveExtensionAwareQueryMethodEvaluationContextProvider(List<? extends ExtensionIdAware> extensions) {

		Assert.notNull(extensions, "EvaluationContextExtensions must not be null!");

		this.delegate = new ReactiveExtensionAwareEvaluationContextProvider(extensions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[])
	 */
	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues) {

		EvaluationContext evaluationContext = delegate.getEvaluationContext(parameterValues);

		if (evaluationContext instanceof StandardEvaluationContext) {
			((StandardEvaluationContext) evaluationContext).setVariables(
					ExtensionAwareQueryMethodEvaluationContextProvider.collectVariables(parameters, parameterValues));
		}

		return evaluationContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[], org.springframework.data.spel.ExpressionDependencies)
	 */
	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues,
			ExpressionDependencies dependencies) {

		EvaluationContext evaluationContext = delegate.getEvaluationContext(parameterValues, dependencies);

		if (evaluationContext instanceof StandardEvaluationContext) {
			((StandardEvaluationContext) evaluationContext).setVariables(
					ExtensionAwareQueryMethodEvaluationContextProvider.collectVariables(parameters, parameterValues));
		}

		return evaluationContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[])
	 */
	@Override
	public <T extends Parameters<?, ?>> Mono<EvaluationContext> getEvaluationContextLater(T parameters,
			Object[] parameterValues) {

		Mono<StandardEvaluationContext> evaluationContext = delegate.getEvaluationContextLater(parameterValues);

		return evaluationContext
				.doOnNext(it -> it.setVariables(
						ExtensionAwareQueryMethodEvaluationContextProvider.collectVariables(parameters, parameterValues)))
				.cast(EvaluationContext.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider#getEvaluationContextLater(org.springframework.data.repository.query.Parameters, java.lang.Object[], org.springframework.data.spel.ExpressionDependencies)
	 */
	@Override
	public <T extends Parameters<?, ?>> Mono<EvaluationContext> getEvaluationContextLater(T parameters,
			Object[] parameterValues, ExpressionDependencies dependencies) {

		Mono<StandardEvaluationContext> evaluationContext = delegate.getEvaluationContextLater(parameterValues,
				dependencies);

		return evaluationContext
				.doOnNext(it -> it.setVariables(
						ExtensionAwareQueryMethodEvaluationContextProvider.collectVariables(parameters, parameterValues)))
				.cast(EvaluationContext.class);
	}
}
