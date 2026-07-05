/*
 * Copyright 2008-present the original author or authors.
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

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Vector;
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

	public static final List<Class<?>> TYPES = List.of(ScrollPosition.class, Pageable.class, Sort.class,
			Limit.class);

	private static final String PARAM_ON_SPECIAL = format("You must not use @%s on a parameter typed %s or %s",
			Param.class.getSimpleName(), Pageable.class.getSimpleName(), Sort.class.getSimpleName());
	private static final String ALL_OR_NOTHING = String.format(
			"Either use @%s on all parameters except %s and %s typed once, or none at all", Param.class.getSimpleName(),
			Pageable.class.getSimpleName(), Sort.class.getSimpleName());

	private final int vectorIndex;
	private final int scoreIndex;
	private final int scoreRangeIndex;
	private final int scrollPositionIndex;
	private final int pageableIndex;
	private final int sortIndex;
	private final int limitIndex;
	private final List<T> parameters;
	private final Lazy<S> bindable;

	private int dynamicProjectionIndex;

	/**
	 * Creates a new {@link Parameters} instance for the given {@link Method} and {@link Function} to create a
	 * {@link Parameter} instance from a {@link MethodParameter}.
	 *
	 * @param parametersSource must not be {@literal null}.
	 * @param parameterFactory must not be {@literal null}.
	 * @since 3.2.1
	 */
	protected Parameters(ParametersSource parametersSource, Function<MethodParameter, T> parameterFactory) {

		Assert.notNull(parametersSource, "ParametersSource must not be null");
		Assert.notNull(parameterFactory, "Parameter factory must not be null");

		Method method = parametersSource.getMethod();
		int parameterCount = method.getParameterCount();

		this.parameters = new ArrayList<>(parameterCount);
		this.dynamicProjectionIndex = -1;

		int vectorIndex = -1;
		int scoreIndex = -1;
		int scoreRangeIndex = -1;
		int scrollPositionIndex = -1;
		int pageableIndex = -1;
		int sortIndex = -1;
		int limitIndex = -1;

		for (int i = 0; i < parameterCount; i++) {

			MethodParameter methodParameter = new MethodParameter(method, i)
					.withContainingClass(parametersSource.getContainingClass());

			T parameter = parameterFactory.apply(methodParameter);

			if (parameter.isSpecialParameter() && parameter.isNamedParameter()) {
				throw new IllegalArgumentException(PARAM_ON_SPECIAL);
			}

			if (parameter.isDynamicProjectionParameter()) {
				this.dynamicProjectionIndex = parameter.getIndex();
			}

			if (Vector.class.isAssignableFrom(parameter.getType())) {
				vectorIndex = i;
			}

			if (Score.class.isAssignableFrom(parameter.getType())) {
				scoreIndex = i;
			}

			if (Range.class.isAssignableFrom(parameter.getType())
					&& Score.class.isAssignableFrom(ResolvableType.forMethodParameter(methodParameter).getGeneric(0).toClass())) {
				scoreRangeIndex = i;
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

			if (Limit.class.isAssignableFrom(parameter.getType())) {
				limitIndex = i;
			}

			parameters.add(parameter);
		}

		this.vectorIndex = vectorIndex;
		this.scoreIndex = scoreIndex;
		this.scoreRangeIndex = scoreRangeIndex;
		this.scrollPositionIndex = scrollPositionIndex;
		this.pageableIndex = pageableIndex;
		this.sortIndex = sortIndex;
		this.limitIndex = limitIndex;
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

		int vectorIndexTemp = -1;
		int scoreIndexTemp = -1;
		int scoreRangeIndexTemp = -1;
		int scrollPositionIndexTemp = -1;
		int pageableIndexTemp = -1;
		int sortIndexTemp = -1;
		int limitIndexTmp = -1;
		int dynamicProjectionTemp = -1;

		for (int i = 0; i < originals.size(); i++) {

			T original = originals.get(i);
			this.parameters.add(original);

			vectorIndexTemp = original.isVector() ? i : -1;
			scoreIndexTemp = original.isScore() ? i : -1;
			scoreRangeIndexTemp = original.isScoreRange() ? i : -1;
			scrollPositionIndexTemp = original.isScrollPosition() ? i : -1;
			pageableIndexTemp = original.isPageable() ? i : -1;
			sortIndexTemp = original.isSort() ? i : -1;
			limitIndexTmp = original.isLimit() ? i : -1;
			dynamicProjectionTemp = original.isDynamicProjectionParameter() ? i : -1;
		}

		this.vectorIndex = vectorIndexTemp;
		this.scoreIndex = scoreIndexTemp;
		this.scoreRangeIndex = scoreRangeIndexTemp;
		this.scrollPositionIndex = scrollPositionIndexTemp;
		this.pageableIndex = pageableIndexTemp;
		this.sortIndex = sortIndexTemp;
		this.limitIndex = limitIndexTmp;
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
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Vector} argument.
	 *
	 * @return
	 * @since 4.0
	 */
	public boolean hasVectorParameter() {
		return vectorIndex != -1;
	}

	/**
	 * Returns the index of the {@link Vector} argument.
	 *
	 * @return the argument index or {@literal -1} if none defined.
	 * @since 4.0
	 */
	public int getVectorIndex() {
		return vectorIndex;
	}

	/**
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Score} argument.
	 *
	 * @return
	 * @since 4.0
	 */
	public boolean hasScoreParameter() {
		return scoreIndex != -1;
	}

	/**
	 * Returns the index of the {@link Score} argument.
	 *
	 * @return the argument index or {@literal -1} if none defined.
	 * @since 4.0
	 */
	public int getScoreIndex() {
		return scoreIndex;
	}

	/**
	 * Returns whether the method, the {@link Parameters} was created for, contains a {@link Range} of {@link Score}
	 * argument.
	 *
	 * @return
	 * @since 4.0
	 */
	public boolean hasScoreRangeParameter() {
		return scoreRangeIndex != -1;
	}

	/**
	 * Returns the index of the argument that contains a {@link Range} of {@link Score}.
	 *
	 * @return the argument index or {@literal -1} if none defined.
	 * @since 4.0
	 */
	public int getScoreRangeIndex() {
		return scoreRangeIndex;
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
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Limit} argument.
	 *
	 * @return
	 * @since 3.2
	 */
	public boolean hasLimitParameter() {
		return getLimitIndex() != -1;
	}

	/**
	 * Returns the index of the {@link Limit} {@link Method} parameter if available. Will return {@literal -1} if there is
	 * no {@link Limit} argument in the {@link Method}'s parameter list.
	 *
	 * @return
	 * @since 3.2
	 */
	public int getLimitIndex() {
		return limitIndex;
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
	 * Asserts that either all the non-special parameters ({@link Pageable}, {@link Sort}) are annotated with
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

	@Override
	public Iterator<T> iterator() {
		return parameters.iterator();
	}

}
