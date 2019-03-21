/*
 * Copyright 2018 the original author or authors.
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

import lombok.Getter;
import reactor.util.context.Context;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.ReactiveEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.SubscriberContextAwareExtension;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * {@link ExtensionAwareEvaluationContextProvider} implementation that is notified with a Reactor subscriber
 * {@link Context} upon execution. It propagates the context to context-aware {@link SubscriberContextAwareExtension
 * extensions}.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public class ReactiveQueryMethodEvaluationContextProvider extends ExtensionAwareQueryMethodEvaluationContextProvider {

	/**
	 * Creates a new {@link ReactiveQueryMethodEvaluationContextProvider}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	public ReactiveQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * Creates a new {@link ReactiveQueryMethodEvaluationContextProvider} for the given
	 * {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ReactiveQueryMethodEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {
		super(extensions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider#createEvaluationContextProvider(java.util.function.Supplier)
	 */
	@Override
	ExtensionAwareEvaluationContextProvider createEvaluationContextProvider(
			Supplier<List<? extends EvaluationContextExtension>> extensions) {
		return new ReactiveEvaluationContextProvider(extensions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.spel.ExtensionAwareEvaluationContextProvider, org.springframework.data.repository.query.QueryMethodEvaluationContextProvider.ParameterContext)
	 */
	@Override
	protected <T extends Parameters<?, ?>> StandardEvaluationContext getEvaluationContext(
			ExtensionAwareEvaluationContextProvider provider, ParameterContext<T> parameterContext) {

		ExtensionAwareEvaluationContextProvider providerToUse = provider;
		if (provider instanceof ReactiveEvaluationContextProvider && parameterContext instanceof ReactiveParameterContext) {

			Context context = ((ReactiveParameterContext<T>) parameterContext).getContext();
			providerToUse = ((ReactiveEvaluationContextProvider) provider).withSubscriberContext(context);
		}

		return super.getEvaluationContext(providerToUse, parameterContext);
	}

	/**
	 * Value object to abstract {@link Parameters}, their actual values and a Reactor {@link Context subscriber context}
	 * for a query method invocation.
	 */
	public static class ReactiveParameterContext<T extends Parameters<?, ?>> extends DefaultParameterContext<T> {

		private final @Getter Context context;

		private ReactiveParameterContext(T parameters, Supplier<Object[]> parameterValuesSupplier, Context context) {

			super(parameters, parameterValuesSupplier);

			this.context = context;
		}

		private ReactiveParameterContext(T parameters, Object[] parameterValues, Context context) {

			super(parameters, parameterValues);

			this.context = context;
		}

		/**
		 * Creates a new {@link ParameterContext} given {@link Parameters}, their {@code parameterValues} and a
		 * {@link Context}.
		 *
		 * @param parameters the {@link Parameters} instance obtained from the query method the context is built for.
		 * @param parameterValues must not be {@literal null}.
		 * @param context must not be {@literal null}.
		 * @return
		 */
		public static <T extends Parameters<?, ?>> ReactiveParameterContext<T> of(T parameters, Object[] parameterValues,
				Context context) {

			Assert.notNull(parameters, "Parameters must not be null!");
			Assert.notNull(parameterValues, "Parameter values must not be null!");
			Assert.notNull(context, "Context must not be null!");

			return new ReactiveParameterContext<>(parameters, parameterValues, context);
		}

		/**
		 * Creates a new {@link ParameterContext} given {@link Parameters}, their {@code parameterValues} and a
		 * {@link Context}.
		 *
		 * @param parameters the {@link Parameters} instance obtained from the query method the context is built for.
		 * @param parameterValues must not be {@literal null}.
		 * @param context must not be {@literal null}.
		 * @return
		 */
		public static <T extends Parameters<?, ?>> ReactiveParameterContext<T> of(T parameters,
				Supplier<Object[]> parameterValues, Context context) {

			Assert.notNull(parameters, "Parameters must not be null!");
			Assert.notNull(parameterValues, "Parameter value supplier must not be null!");
			Assert.notNull(context, "Context must not be null!");

			return new ReactiveParameterContext<>(parameters, parameterValues, context);
		}
	}
}
