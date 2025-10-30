/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.core.ReactiveWrappers;
import org.springframework.data.expression.ReactiveValueEvaluationContextProvider;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.ReactiveEvaluationContextProvider;
import org.springframework.data.spel.ReactiveExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ExtensionIdAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory to create {@link ValueEvaluationContextProvider} instances. Supports its reactive variant
 * {@link ReactiveValueEvaluationContextProvider} if the underlying {@link EvaluationContextProvider} is a reactive one.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class QueryMethodValueEvaluationContextAccessor {

	public static final EvaluationContextProvider DEFAULT_CONTEXT_PROVIDER = createEvaluationContextProvider(
			Collections.emptyList());

	static final StandardEnvironment ENVIRONMENT = new StandardEnvironment();

	private final Environment environment;
	private final EvaluationContextProvider evaluationContextProvider;

	/**
	 * Creates a new {@link QueryMethodValueEvaluationContextAccessor} from {@link ApplicationContext}.
	 *
	 * @param context the application context to use, must not be {@literal null}.
	 */
	public QueryMethodValueEvaluationContextAccessor(ApplicationContext context) {

		Assert.notNull(context, "ApplicationContext must not be null");

		this.environment = context.getEnvironment();
		this.evaluationContextProvider = createEvaluationContextProvider(context);
	}

	/**
	 * Creates a new {@link QueryMethodValueEvaluationContextAccessor} from {@link Environment} and
	 * {@link ListableBeanFactory}.
	 *
	 * @param environment
	 * @param beanFactory the bean factory to use, must not be {@literal null}.
	 */
	public QueryMethodValueEvaluationContextAccessor(Environment environment, ListableBeanFactory beanFactory) {
		this(environment, createEvaluationContextProvider(beanFactory));
	}

	/**
	 * Creates a new {@link QueryMethodValueEvaluationContextAccessor} from {@link Environment} and
	 * {@link EvaluationContextProvider}.
	 *
	 * @param environment
	 * @param evaluationContextProvider the underlying {@link EvaluationContextProvider} to use, must not be
	 *          {@literal null}.
	 */
	public QueryMethodValueEvaluationContextAccessor(Environment environment,
			EvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null");

		this.environment = environment;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	/**
	 * Creates a new {@link QueryMethodValueEvaluationContextAccessor} for the given {@link EvaluationContextExtension}s.
	 *
	 * @param environment
	 * @param extensions must not be {@literal null}.
	 */
	public QueryMethodValueEvaluationContextAccessor(Environment environment,
			Collection<? extends ExtensionIdAware> extensions) {

		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(extensions, "EvaluationContextExtensions must not be null");

		this.environment = environment;
		this.evaluationContextProvider = createEvaluationContextProvider(extensions);
	}

	public static EvaluationContextProvider createEvaluationContextProvider(ListableBeanFactory factory) {

		return ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)
				? new ReactiveExtensionAwareEvaluationContextProvider(factory)
				: new ExtensionAwareEvaluationContextProvider(factory);

	}

	private static EvaluationContextProvider createEvaluationContextProvider(
			Collection<? extends ExtensionIdAware> extensions) {

		return ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)
				? new ReactiveExtensionAwareEvaluationContextProvider(extensions)
				: new ExtensionAwareEvaluationContextProvider(extensions);
	}

	/**
	 * Creates a default {@link QueryMethodValueEvaluationContextAccessor} using the
	 * {@link org.springframework.core.env.StandardEnvironment} and extension-less
	 * {@link org.springframework.data.spel.EvaluationContextProvider}.
	 *
	 * @return a default {@link ValueExpressionDelegate}.
	 */
	public static QueryMethodValueEvaluationContextAccessor create() {
		return new QueryMethodValueEvaluationContextAccessor(ENVIRONMENT, DEFAULT_CONTEXT_PROVIDER);
	}

	EvaluationContextProvider getEvaluationContextProvider() {
		return evaluationContextProvider;
	}

	/**
	 * Creates a new {@link ValueEvaluationContextProvider} for the given {@link Parameters}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @return a new {@link ValueEvaluationContextProvider} for the given {@link Parameters}.
	 */
	public ValueEvaluationContextProvider create(Parameters<?, ?> parameters) {

		Assert.notNull(parameters, "Parameters must not be null");

		if (ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)) {
			if (evaluationContextProvider instanceof ReactiveEvaluationContextProvider reactive) {
				return new DefaultReactiveQueryMethodValueEvaluationContextProvider(environment, parameters, reactive);
			}
		}

		return new DefaultQueryMethodValueEvaluationContextProvider(environment, parameters, evaluationContextProvider);
	}

	/**
	 * Exposes variables for all named parameters for the given arguments. Also exposes non-bindable parameters under the
	 * names of their types.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param arguments must not be {@literal null}.
	 * @return
	 */
	static Map<String, Object> collectVariables(Parameters<?, ?> parameters, Object[] arguments) {

		if (parameters.getNumberOfParameters() != arguments.length) {

			throw new IllegalArgumentException(
					"Number of method parameters (%d) must match the number of method invocation arguments (%d)"
							.formatted(parameters.getNumberOfParameters(), arguments.length));
		}

		Map<String, Object> variables = new HashMap<>(parameters.getNumberOfParameters(), 1.0f);

		for (Parameter parameter : parameters) {

			if (parameter.isSpecialParameter()) {
				variables.put(//
						StringUtils.uncapitalize(parameter.getType().getSimpleName()), //
						arguments[parameter.getIndex()]);
			}

			if (parameter.isNamedParameter()) {
				variables.put(parameter.getRequiredName(), //
						arguments[parameter.getIndex()]);
			}
		}

		return variables;
	}

	/**
	 * Imperative {@link ValueEvaluationContextProvider} variant.
	 */
	static class DefaultQueryMethodValueEvaluationContextProvider implements ValueEvaluationContextProvider {

		final Environment environment;
		final Parameters<?, ?> parameters;
		final EvaluationContextProvider delegate;

		DefaultQueryMethodValueEvaluationContextProvider(Environment environment, Parameters<?, ?> parameters,
				EvaluationContextProvider delegate) {
			this.environment = environment;
			this.parameters = parameters;
			this.delegate = delegate;
		}

		@Override
		public ValueEvaluationContext getEvaluationContext(@Nullable Object rootObject) {
			return doGetEvaluationContext(delegate.getEvaluationContext(rootObject), rootObject);
		}

		@Override
		public ValueEvaluationContext getEvaluationContext(@Nullable Object rootObject,
				ExpressionDependencies dependencies) {
			return doGetEvaluationContext(delegate.getEvaluationContext(rootObject, dependencies), rootObject);
		}

		ValueEvaluationContext doGetEvaluationContext(EvaluationContext evaluationContext, @Nullable Object rootObject) {

			if (rootObject instanceof Object[] parameterValues) {
				collectVariables(parameters, parameterValues).forEach(evaluationContext::setVariable);
			}

			return ValueEvaluationContext.of(environment, evaluationContext);
		}

	}

	/**
	 * Reactive {@link ValueEvaluationContextProvider} extension to
	 * {@link DefaultQueryMethodValueEvaluationContextProvider}.
	 */
	static class DefaultReactiveQueryMethodValueEvaluationContextProvider
			extends DefaultQueryMethodValueEvaluationContextProvider implements ReactiveValueEvaluationContextProvider {

		private final ReactiveEvaluationContextProvider delegate;

		DefaultReactiveQueryMethodValueEvaluationContextProvider(Environment environment,
				Parameters<?, ?> parameters, ReactiveEvaluationContextProvider delegate) {
			super(environment, parameters, delegate);
			this.delegate = delegate;
		}

		@Override
		public Mono<ValueEvaluationContext> getEvaluationContextLater(@Nullable Object rootObject) {
			return delegate.getEvaluationContextLater(rootObject).map(it -> doGetEvaluationContext(it, rootObject));
		}

		@Override
		public Mono<ValueEvaluationContext> getEvaluationContextLater(@Nullable Object rootObject,
				ExpressionDependencies dependencies) {
			return delegate.getEvaluationContextLater(rootObject, dependencies)
					.map(it -> doGetEvaluationContext(it, rootObject));
		}
	}
}
