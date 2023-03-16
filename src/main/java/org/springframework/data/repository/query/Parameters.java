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

import static java.lang.String.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Abstracts method parameters that have to be bound to query parameters or applied to the query independently.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Johannes Englmeier
 */
public abstract class Parameters<S extends Parameters<S, T>, T extends Parameter> implements Streamable<T> {

	public static final List<Class<?>> TYPES = Arrays.asList(ScrollPosition.class, Pageable.class, Sort.class);

	private static final String PARAM_ON_SPECIAL = format("You must not use @%s on a parameter typed %s or %s",
			Param.class.getSimpleName(), Pageable.class.getSimpleName(), Sort.class.getSimpleName());
	private static final String ALL_OR_NOTHING = String.format(
			"Either use @%s on all parameters except %s and %s typed once, or none at all", Param.class.getSimpleName(),
			Pageable.class.getSimpleName(), Sort.class.getSimpleName());

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final int scrollPositionIndex;
	private final int pageableIndex;
	private final int sortIndex;
	private final List<T> parameters;
	private final Lazy<S> bindable;

	private int dynamicProjectionIndex;

	/**
	 * Creates a new instance of {@link Parameters}.
	 *
	 * @param method must not be {@literal null}.
	 * @deprecated since 3.1, use {@link #Parameters(Method, Function)} instead.
	 */
	@SuppressWarnings("null")
	@Deprecated(since = "3.1", forRemoval = true)
	public Parameters(Method method) {
		this(method, null);
	}

	/**
	 * Creates a new {@link Parameters} instance for the given {@link Method} and {@link Function} to create a
	 * {@link Parameter} instance from a {@link MethodParameter}.
	 *
	 * @param method must not be {@literal null}.
	 * @param parameterFactory must not be {@literal null}.
	 * @since 3.0.2
	 */
	protected Parameters(Method method, Function<MethodParameter, T> parameterFactory) {

		Assert.notNull(method, "Method must not be null");

		// Factory nullability not enforced yet to support falling back to the deprecated
		// createParameter(MethodParameter). Add assertion when the deprecation is removed.

		int parameterCount = method.getParameterCount();

		this.parameters = new ArrayList<>(parameterCount);
		this.dynamicProjectionIndex = -1;

		int scrollPositionIndex = -1;
		int pageableIndex = -1;
		int sortIndex = -1;

		for (int i = 0; i < parameterCount; i++) {

			MethodParameter methodParameter = new MethodParameter(method, i);
			methodParameter.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);

			T parameter = parameterFactory == null //
					? createParameter(methodParameter) //
					: parameterFactory.apply(methodParameter);

			if (parameter.isSpecialParameter() && parameter.isNamedParameter()) {
				throw new IllegalArgumentException(PARAM_ON_SPECIAL);
			}

			if (parameter.isDynamicProjectionParameter()) {
				this.dynamicProjectionIndex = parameter.getIndex();
			}

			if (ScrollPosition.class.isAssignableFrom(parameter.getType())) {
				scrollPositionIndex = i;
			}

			if (Pageable.class.isAssignableFrom(parameter.getType())) {
				pageableIndex = i;
			}

			if (Sort.class.isAssignableFrom(parameter.getType())) {
				sortIndex = i;
			}

			parameters.add(parameter);
		}

		this.scrollPositionIndex = scrollPositionIndex;
		this.pageableIndex = pageableIndex;
		this.sortIndex = sortIndex;
		this.bindable = Lazy.of(this::getBindable);

		assertEitherAllParamAnnotatedOrNone();
	}

	/**
	 * Creates a new {@link Parameters} instance with the given {@link Parameter}s put into new context.
	 *
	 * @param originals
	 */
	protected Parameters(List<T> originals) {

		this.parameters = new ArrayList<>(originals.size());

		int scrollPositionIndexTemp = -1;
		int pageableIndexTemp = -1;
		int sortIndexTemp = -1;
		int dynamicProjectionTemp = -1;

		for (int i = 0; i < originals.size(); i++) {

			T original = originals.get(i);
			this.parameters.add(original);

			scrollPositionIndexTemp = original.isScrollPosition() ? i : -1;
			pageableIndexTemp = original.isPageable() ? i : -1;
			sortIndexTemp = original.isSort() ? i : -1;
			dynamicProjectionTemp = original.isDynamicProjectionParameter() ? i : -1;
		}

		this.scrollPositionIndex = scrollPositionIndexTemp;
		this.pageableIndex = pageableIndexTemp;
		this.sortIndex = sortIndexTemp;
		this.dynamicProjectionIndex = dynamicProjectionTemp;
		this.bindable = Lazy.of(() -> (S) this);
	}

	private S getBindable() {

		List<T> bindables = new ArrayList<>();

		for (T candidate : this) {

			if (candidate.isBindable()) {
				bindables.add(candidate);
			}
		}

		return createFrom(bindables);
	}

	/**
	 * Creates a {@link Parameter} instance for the given {@link MethodParameter}.
	 *
	 * @param parameter will never be {@literal null}.
	 * @return
	 * @deprecated since 3.1, in your extension, call {@link #Parameters(Method, Function)} instead.
	 */
	@SuppressWarnings("unchecked")
	@Deprecated(since = "3.1", forRemoval = true)
	protected T createParameter(MethodParameter parameter) {
		return (T) new Parameter(parameter);
	}

	/**
	 * Returns whether the method the {@link Parameters} was created for contains a {@link ScrollPosition} argument.
	 *
	 * @return
	 * @since 3.1
	 */
	public boolean hasScrollPositionParameter() {
		return scrollPositionIndex != -1;
	}

	/**
	 * Returns the index of the {@link ScrollPosition} {@link Method} parameter if available. Will return {@literal -1} if
	 * there is no {@link ScrollPosition} argument in the {@link Method}'s parameter list.
	 *
	 * @return the scrollPositionIndex
	 * @since 3.1
	 */
	public int getScrollPositionIndex() {
		return scrollPositionIndex;
	}

	/**
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Pageable} argument.
	 *
	 * @return
	 */
	public boolean hasPageableParameter() {
		return pageableIndex != -1;
	}

	/**
	 * Returns the index of the {@link Pageable} {@link Method} parameter if available. Will return {@literal -1} if there
	 * is no {@link Pageable} argument in the {@link Method}'s parameter list.
	 *
	 * @return the pageableIndex
	 */
	public int getPageableIndex() {
		return pageableIndex;
	}

	/**
	 * Returns the index of the {@link Sort} {@link Method} parameter if available. Will return {@literal -1} if there is
	 * no {@link Sort} argument in the {@link Method}'s parameter list.
	 *
	 * @return
	 */
	public int getSortIndex() {
		return sortIndex;
	}

	/**
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Sort} argument.
	 *
	 * @return
	 */
	public boolean hasSortParameter() {
		return sortIndex != -1;
	}

	/**
	 * Returns the index of the parameter that represents the dynamic projection type. Will return {@literal -1} if no
	 * such parameter exists.
	 *
	 * @return
	 */
	public int getDynamicProjectionIndex() {
		return dynamicProjectionIndex;
	}

	/**
	 * Returns whether a parameter expressing a dynamic projection exists.
	 *
	 * @return
	 */
	public boolean hasDynamicProjection() {
		return dynamicProjectionIndex != -1;
	}

	/**
	 * Returns whether we potentially find a {@link Sort} parameter in the parameters.
	 *
	 * @return
	 */
	public boolean potentiallySortsDynamically() {
		return hasSortParameter() || hasPageableParameter();
	}

	/**
	 * Returns the parameter with the given index.
	 *
	 * @param index
	 * @return
	 */
	public T getParameter(int index) {

		try {
			return parameters.get(index);
		} catch (IndexOutOfBoundsException e) {
			throw new ParameterOutOfBoundsException(
					"Invalid parameter index; You seem to have declared too little query method parameters", e);
		}
	}

	/**
	 * Returns whether we have a parameter at the given position.
	 *
	 * @param position
	 * @return
	 */
	public boolean hasParameterAt(int position) {

		try {
			return null != getParameter(position);
		} catch (ParameterOutOfBoundsException e) {
			return false;
		}
	}

	/**
	 * Returns whether the method signature contains one of the special parameters ({@link Pageable}, {@link Sort}).
	 *
	 * @return
	 */
	public boolean hasSpecialParameter() {
		return hasScrollPositionParameter() || hasSortParameter() || hasPageableParameter();
	}

	/**
	 * Returns the number of parameters.
	 *
	 * @return
	 */
	public int getNumberOfParameters() {
		return parameters.size();
	}

	/**
	 * Returns a {@link Parameters} instance with effectively all special parameters removed.
	 *
	 * @return
	 * @see Parameter#TYPES
	 * @see Parameter#isSpecialParameter()
	 */
	public S getBindableParameters() {
		return this.bindable.get();
	}

	protected abstract S createFrom(List<T> parameters);

	/**
	 * Returns a bindable parameter with the given index. So for a method with a signature of
	 * {@code (Pageable pageable, String name)} a call to {@code #getBindableParameter(0)} will return the {@link String}
	 * parameter.
	 *
	 * @param bindableIndex
	 * @return
	 */
	public T getBindableParameter(int bindableIndex) {
		return getBindableParameters().getParameter(bindableIndex);
	}

	/**
	 * Asserts that either all of the non special parameters ({@link Pageable}, {@link Sort}) are annotated with
	 * {@link Param} or none of them is.
	 */
	private void assertEitherAllParamAnnotatedOrNone() {

		boolean nameFound = false;
		int index = 0;

		for (T parameter : this.getBindableParameters()) {

			if (parameter.isNamedParameter()) {
				Assert.isTrue(nameFound || index == 0, ALL_OR_NOTHING);
				nameFound = true;
			} else {
				Assert.isTrue(!nameFound, ALL_OR_NOTHING);
			}

			index++;
		}
	}

	/**
	 * Returns whether the given type is a bindable parameter.
	 *
	 * @param type
	 * @return
	 */
	public static boolean isBindable(Class<?> type) {
		return !TYPES.contains(type);
	}

	public Iterator<T> iterator() {
		return parameters.iterator();
	}
}
