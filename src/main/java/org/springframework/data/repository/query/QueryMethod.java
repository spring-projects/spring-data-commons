/*
 * Copyright 2008-2023 the original author or authors.
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

import static org.springframework.data.repository.util.ClassUtils.*;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Abstraction of a method that is designated to execute a finder query. Enriches the standard {@link Method} interface
 * with specific information that is necessary to construct {@link RepositoryQuery}s for the method.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Maciek Opała
 * @author Mark Paluch
 * @author Johannes Englmeier
 */
public class QueryMethod {

	private final RepositoryMetadata metadata;
	private final Method method;
	private final Class<?> unwrappedReturnType;
	private final Parameters<?, ?> parameters;
	private final ResultProcessor resultProcessor;
	private final Lazy<Class<?>> domainClass;
	private final Lazy<Boolean> isCollectionQuery;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct query to use for following
	 * invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public QueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(metadata, "Repository metadata must not be null");
		Assert.notNull(factory, "ProjectionFactory must not be null");

		Parameters.TYPES.stream()
				.filter(type -> getNumberOfOccurrences(method, type) > 1)
				.findFirst().ifPresent(type -> {
					throw new IllegalStateException(
							String.format("Method must have only one argument of type %s; Offending method: %s",
									type.getSimpleName(), method));
				});

		this.method = method;
		this.unwrappedReturnType = potentiallyUnwrapReturnTypeFor(metadata, method);
		this.metadata = metadata;
		this.parameters = createParameters(method);

		if (hasParameterOfType(method, Pageable.class)) {

			if (!isStreamQuery()) {
				assertReturnTypeAssignable(method, QueryExecutionConverters.getAllowedPageableTypes());
			}

			if (hasParameterOfType(method, Sort.class)) {
				throw new IllegalStateException(String.format("Method must not have Pageable *and* Sort parameters. "
						+ "Use sorting capabilities on Pageable instead; Offending method: %s", method));
			}
		}

		Assert.notNull(this.parameters,
				() -> String.format("Parameters extracted from method '%s' must not be null", method.getName()));

		if (isPageQuery()) {
			Assert.isTrue(this.parameters.hasPageableParameter(),
					String.format("Paging query needs to have a Pageable parameter; Offending method: %s", method));
		}

		this.domainClass = Lazy.of(() -> {

			Class<?> repositoryDomainClass = metadata.getDomainType();
			Class<?> methodDomainClass = metadata.getReturnedDomainClass(method);

			return repositoryDomainClass == null || repositoryDomainClass.isAssignableFrom(methodDomainClass)
					? methodDomainClass
					: repositoryDomainClass;
		});

		this.resultProcessor = new ResultProcessor(this, factory);
		this.isCollectionQuery = Lazy.of(this::calculateIsCollectionQuery);
	}

	/**
	 * Creates a {@link Parameters} instance.
	 *
	 * @param method must not be {@literal null}.
	 * @return must not return {@literal null}.
	 * @deprecated since 3.1, call or override {@link #createParameters(Method, TypeInformation)} instead.
	 */
	@Deprecated(since = "3.1", forRemoval = true)
	protected Parameters<?, ?> createParameters(Method method) {
		return createParameters(method, metadata.getDomainTypeInformation());
	}

	/**
	 * Creates a {@link Parameters} instance.
	 *
	 * @param method must not be {@literal null}.
	 * @param aggregateType must not be {@literal null}.
	 * @return must not return {@literal null}.
	 */
	protected Parameters<?, ?> createParameters(Method method, TypeInformation<?> aggregateType) {
		return new DefaultParameters(method, aggregateType);
	}

	/**
	 * Returns the method's name.
	 *
	 * @return
	 */
	public String getName() {
		return method.getName();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public EntityMetadata<?> getEntityInformation() {
		return () -> (Class) getDomainClass();
	}

	/**
	 * Returns the name of the named query this method belongs to.
	 *
	 * @return
	 */
	public String getNamedQueryName() {
		return String.format("%s.%s", getDomainClass().getSimpleName(), method.getName());
	}

	/**
	 * Returns the domain class the query method is targeted at.
	 *
	 * @return will never be {@literal null}.
	 */
	protected Class<?> getDomainClass() {
		return domainClass.get();
	}

	/**
	 * Returns the type of the object that will be returned.
	 *
	 * @return
	 */
	public Class<?> getReturnedObjectType() {
		return metadata.getReturnedDomainClass(method);
	}

	/**
	 * Returns whether the finder will actually return a collection of entities or a single one.
	 *
	 * @return
	 */
	public boolean isCollectionQuery() {
		return isCollectionQuery.get();
	}

	/**
	 * Returns whether the query method will return a {@link Slice}.
	 *
	 * @return
	 * @since 1.8
	 */
	public boolean isSliceQuery() {
		return !isPageQuery() && org.springframework.util.ClassUtils.isAssignable(Slice.class, unwrappedReturnType);
	}

	/**
	 * Returns whether the finder will return a {@link Page} of results.
	 *
	 * @return
	 */
	public final boolean isPageQuery() {
		return org.springframework.util.ClassUtils.isAssignable(Page.class, unwrappedReturnType);
	}

	/**
	 * Returns whether the query method is a modifying one.
	 *
	 * @return
	 */
	public boolean isModifyingQuery() {
		return false;
	}

	/**
	 * Returns whether the query for this method actually returns entities.
	 *
	 * @return
	 */
	public boolean isQueryForEntity() {
		return getDomainClass().isAssignableFrom(getReturnedObjectType());
	}

	/**
	 * Returns whether the method returns a Stream.
	 *
	 * @return
	 * @since 1.10
	 */
	public boolean isStreamQuery() {
		return Stream.class.isAssignableFrom(unwrappedReturnType);
	}

	/**
	 * Returns the {@link Parameters} wrapper to gain additional information about {@link Method} parameters.
	 *
	 * @return
	 */
	public Parameters<?, ?> getParameters() {
		return parameters;
	}

	/**
	 * Returns the {@link ResultProcessor} to be used with the query method.
	 *
	 * @return the resultFactory
	 */
	public ResultProcessor getResultProcessor() {
		return resultProcessor;
	}

	RepositoryMetadata getMetadata() {
		return metadata;
	}

	Method getMethod() {
		return method;
	}

	@Override
	public String toString() {
		return method.toString();
	}

	private boolean calculateIsCollectionQuery() {

		if (isPageQuery() || isSliceQuery()) {
			return false;
		}

		Class<?> returnType = metadata.getReturnType(method).getType();

		if (QueryExecutionConverters.supports(returnType) && !QueryExecutionConverters.isSingleValue(returnType)) {
			return true;
		}

		if (QueryExecutionConverters.supports(unwrappedReturnType)) {
			return !QueryExecutionConverters.isSingleValue(unwrappedReturnType);
		}

		return TypeInformation.of(unwrappedReturnType).isCollectionLike();
	}

	private static Class<? extends Object> potentiallyUnwrapReturnTypeFor(RepositoryMetadata metadata, Method method) {

		TypeInformation<?> returnType = metadata.getReturnType(method);
		if (QueryExecutionConverters.supports(returnType.getType())
				|| ReactiveWrapperConverters.supports(returnType.getType())) {

			// unwrap only one level to handle cases like Future<List<Entity>> correctly.

			TypeInformation<?> componentType = returnType.getComponentType();

			if (componentType == null) {
				throw new IllegalStateException(
						String.format("Couldn't find component type for return value of method %s", method));
			}

			return componentType.getType();
		}

		return returnType.getType();
	}

	private static void assertReturnTypeAssignable(Method method, Set<Class<?>> types) {

		Assert.notNull(method, "Method must not be null");
		Assert.notEmpty(types, "Types must not be null or empty");

		// TODO: to resolve generics fully we'd need the actual repository interface here
		TypeInformation<?> returnType = TypeInformation.fromReturnTypeOf(method);

		returnType = QueryExecutionConverters.isSingleValue(returnType.getType()) //
				? returnType.getRequiredComponentType() //
				: returnType;

		for (Class<?> type : types) {
			if (type.isAssignableFrom(returnType.getType())) {
				return;
			}
		}

		throw new IllegalStateException("Method has to have one of the following return types" + types);
	}
}
