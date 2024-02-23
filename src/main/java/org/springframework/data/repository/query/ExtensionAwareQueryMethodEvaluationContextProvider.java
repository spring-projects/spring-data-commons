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

import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;

/**
 * An {@link QueryMethodEvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} instances.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Johannes Englmeier
 * @since 1.9
 * @deprecated since 3.4 in favor of {@link QueryMethodValueEvaluationContextAccessor}.
 */
@Deprecated(since = "3.4")
public class ExtensionAwareQueryMethodEvaluationContextProvider implements QueryMethodEvaluationContextProvider {

	private final QueryMethodValueEvaluationContextAccessor delegate;

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param evaluationContextProvider to lookup the {@link EvaluationContextExtension}s from, must not be
	 *          {@literal null}.
	 */
	public ExtensionAwareQueryMethodEvaluationContextProvider(EvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null");

		this.delegate = new QueryMethodValueEvaluationContextAccessor(QueryMethodValueEvaluationContextAccessor.ENVIRONMENT,
				evaluationContextProvider);
	}

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	public ExtensionAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {

		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");

		this.delegate = beanFactory instanceof ApplicationContext ctx ? new QueryMethodValueEvaluationContextAccessor(ctx)
				: new QueryMethodValueEvaluationContextAccessor(QueryMethodValueEvaluationContextAccessor.ENVIRONMENT,
						beanFactory);
	}

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider} using the given
	 * {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ExtensionAwareQueryMethodEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {

		Assert.notNull(extensions, "EvaluationContextExtensions must not be null");

		this.delegate = new QueryMethodValueEvaluationContextAccessor(QueryMethodValueEvaluationContextAccessor.ENVIRONMENT,
				extensions);
	}

	ExtensionAwareQueryMethodEvaluationContextProvider(QueryMethodValueEvaluationContextAccessor delegate) {
		this.delegate = delegate;
	}

	@Override
	public EvaluationContextProvider getEvaluationContextProvider() {
		return getDelegate().getEvaluationContextProvider();
	}

	public QueryMethodValueEvaluationContextAccessor getDelegate() {
		return delegate;
	}

	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues) {

		ValueEvaluationContext evaluationContext = delegate.create(parameters).getEvaluationContext(parameterValues);

		return evaluationContext.getRequiredEvaluationContext();
	}

	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues,
			ExpressionDependencies dependencies) {

		ValueEvaluationContext evaluationContext = delegate.create(parameters).getEvaluationContext(parameterValues,
				dependencies);

		return evaluationContext.getRequiredEvaluationContext();
	}

}
