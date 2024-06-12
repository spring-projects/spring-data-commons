/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ResolvableType;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryInvocationMulticaster.DefaultRepositoryInvocationMulticaster;
import org.springframework.data.repository.core.support.RepositoryInvocationMulticaster.NoOpRepositoryInvocationMulticaster;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * This {@link MethodInterceptor} intercepts calls to methods of the custom implementation and delegates the to it if
 * configured. Furthermore it resolves method calls to finders and triggers execution of them. You can rely on having a
 * custom repository implementation instance set if this returns true.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author John Blum
 * @author Johannes Englmeier
 */
class QueryExecutorMethodInterceptor implements MethodInterceptor {

	private final RepositoryInformation repositoryInformation;
	private final Map<Method, RepositoryQuery> queries;
	private final Map<Method, RepositoryMethodInvoker> invocationMetadataCache = new ConcurrentReferenceHashMap<>();
	private final QueryExecutionResultHandler resultHandler;
	private final NamedQueries namedQueries;
	private final List<QueryCreationListener<?>> queryPostProcessors;
	private final RepositoryInvocationMulticaster invocationMulticaster;

	/**
	 * Creates a new {@link QueryExecutorMethodInterceptor}. Builds a model of {@link QueryMethod}s to be invoked on
	 * execution of repository interface methods.
	 */
	public QueryExecutorMethodInterceptor(RepositoryInformation repositoryInformation,
			ProjectionFactory projectionFactory, Optional<QueryLookupStrategy> queryLookupStrategy, NamedQueries namedQueries,
			List<QueryCreationListener<?>> queryPostProcessors,
			List<RepositoryMethodInvocationListener> methodInvocationListeners) {

		this.repositoryInformation = repositoryInformation;
		this.namedQueries = namedQueries;
		this.queryPostProcessors = queryPostProcessors;
		this.invocationMulticaster = methodInvocationListeners.isEmpty() ? NoOpRepositoryInvocationMulticaster.INSTANCE
				: new DefaultRepositoryInvocationMulticaster(methodInvocationListeners);

		this.resultHandler = new QueryExecutionResultHandler(RepositoryFactorySupport.CONVERSION_SERVICE);

		if (!queryLookupStrategy.isPresent() && repositoryInformation.hasQueryMethods()) {

			throw new IllegalStateException(
					"You have defined query methods in the repository" + " but do not have any query lookup strategy defined."
							+ " The infrastructure apparently does not support query methods");
		}

		this.queries = queryLookupStrategy //
				.map(it -> mapMethodsToQuery(repositoryInformation, it, projectionFactory)) //
				.orElse(Collections.emptyMap());
	}

	private Map<Method, RepositoryQuery> mapMethodsToQuery(RepositoryInformation repositoryInformation,
			QueryLookupStrategy lookupStrategy, ProjectionFactory projectionFactory) {

		List<Method> queryMethods = repositoryInformation.getQueryMethods().toList();
		Map<Method, RepositoryQuery> result = new HashMap<>(queryMethods.size());

		for (Method method : queryMethods) {

			Pair<Method, RepositoryQuery> pair = lookupQuery(method, repositoryInformation, lookupStrategy,
					projectionFactory);
			invokeListeners(pair.getSecond());
			result.put(pair.getFirst(), pair.getSecond());
		}

		return result;
	}

	private Pair<Method, RepositoryQuery> lookupQuery(Method method, RepositoryInformation information,
			QueryLookupStrategy strategy, ProjectionFactory projectionFactory) {
		try {
			return Pair.of(method, strategy.resolveQuery(method, information, projectionFactory, namedQueries));
		} catch (QueryCreationException e) {
			throw e;
		} catch (RuntimeException e) {
			throw QueryCreationException.create(e.getMessage(), e, information.getRepositoryInterface(), method);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void invokeListeners(RepositoryQuery query) {

		for (QueryCreationListener listener : queryPostProcessors) {

			ResolvableType typeArgument = ResolvableType.forClass(QueryCreationListener.class, listener.getClass())
					.getGeneric(0);

			if (typeArgument != null && typeArgument.isAssignableFrom(ResolvableType.forClass(query.getClass()))) {
				listener.onCreation(query);
			}
		}
	}

	@Override
	@Nullable
	public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		QueryExecutionConverters.ExecutionAdapter executionAdapter = QueryExecutionConverters //
				.getExecutionAdapter(method.getReturnType());

		if (executionAdapter == null) {
			return resultHandler.postProcessInvocationResult(doInvoke(invocation), method);
		}

		return executionAdapter //
				.apply(() -> resultHandler.postProcessInvocationResult(doInvoke(invocation), method));
	}

	@Nullable
	private Object doInvoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (hasQueryFor(method)) {

			RepositoryMethodInvoker invocationMetadata = invocationMetadataCache.get(method);

			if (invocationMetadata == null) {
				invocationMetadata = RepositoryMethodInvoker.forRepositoryQuery(method, queries.get(method));
				invocationMetadataCache.put(method, invocationMetadata);
			}

			return invocationMetadata.invoke(repositoryInformation.getRepositoryInterface(), invocationMulticaster,
					invocation.getArguments());
		}

		return invocation.proceed();
	}

	/**
	 * Returns whether we know of a query to execute for the given {@link Method};
	 *
	 * @param method
	 * @return
	 */
	private boolean hasQueryFor(Method method) {
		return queries.containsKey(method);
	}

}
