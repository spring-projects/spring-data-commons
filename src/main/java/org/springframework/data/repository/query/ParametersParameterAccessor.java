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

import java.util.Iterator;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Vector;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.util.Assert;

/**
 * {@link ParameterAccessor} implementation using a {@link Parameters} instance to find special parameters.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ParametersParameterAccessor implements ParameterAccessor {

	private final Parameters<?, ?> parameters;
	private final @Nullable Object[] values;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public ParametersParameterAccessor(Parameters<?, ?> parameters, @Nullable Object[] values) {

		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(values, "Values must not be null");

		Assert.isTrue(parameters.getNumberOfParameters() == values.length, "Invalid number of parameters given");

		this.parameters = parameters;

		if (requiresUnwrapping(values)) {
			this.values = new Object[values.length];

			for (int i = 0; i < values.length; i++) {
				this.values[i] = QueryExecutionConverters.unwrap(values[i]);
			}
		} else {
			this.values = values;
		}
	}

	private static boolean requiresUnwrapping(@Nullable Object[] values) {

		for (Object value : values) {
			if (value != null && (QueryExecutionConverters.supports(value.getClass())
					|| ReactiveWrapperConverters.supports(value.getClass()))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the {@link Parameters} instance backing the accessor.
	 *
	 * @return the parameters will never be {@literal null}.
	 */
	public Parameters<?, ?> getParameters() {
		return parameters;
	}

	/**
	 * Returns the potentially unwrapped values.
	 *
	 * @return
	 */
	protected @Nullable Object[] getValues() {
		return this.values;
	}

	@Override
	public @Nullable Vector getVector() {

		if (parameters.getVectorIndex() == -1) {
			return null;
		}

		return (Vector) values[parameters.getVectorIndex()];
	}

	@Override
	public @Nullable Score getScore() {

		if (!parameters.hasScoreParameter()) {
			return null;
		}

		return (Score) values[parameters.getScoreIndex()];
	}

	@Override
	public @Nullable Range<Score> getScoreRange() {

		if (!parameters.hasScoreRangeParameter()) {
			return null;
		}

		return (Range<Score>) values[parameters.getScoreRangeIndex()];
	}

	@Override
	public @Nullable ScrollPosition getScrollPosition() {

		if (!parameters.hasScrollPositionParameter()) {

			Pageable pageable = getPageable();
			if (pageable.isPaged()) {
				return pageable.toScrollPosition();
			}

			return null;
		}

		return (ScrollPosition) values[parameters.getScrollPositionIndex()];
	}

	@Override
	public Pageable getPageable() {

		if (parameters.hasPageableParameter()) {

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			return pageable == null ? Pageable.unpaged() : pageable;
		}

		Limit limit = parameters.hasLimitParameter() ? getLimit() : Limit.unlimited();
		Sort sort = parameters.hasSortParameter() ? getSort() : Sort.unsorted();

		if (limit.isUnlimited()) {
			return Pageable.unpaged(sort);
		}

		return PageRequest.of(0, limit.max(), sort);
	}

	@Override
	public Sort getSort() {

		if (parameters.hasSortParameter()) {

			Sort sort = (Sort) values[parameters.getSortIndex()];
			return sort == null ? Sort.unsorted() : sort;
		}

		if (parameters.hasPageableParameter()) {
			return getPageable().getSort();
		}

		return Sort.unsorted();
	}

	@Override
	@SuppressWarnings("NullAway")
	public Limit getLimit() {

		if (parameters.hasLimitParameter()) {

			Limit limit = (Limit) values[parameters.getLimitIndex()];
			return limit == null ? Limit.unlimited() : limit;
		}

		if (parameters.hasPageableParameter()) {

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			return pageable.toLimit();
		}

		return Limit.unlimited();
	}

	/**
	 * Returns the dynamic projection type if available, {@literal null} otherwise.
	 *
	 * @return
	 */
	@Override
	@Nullable
	public Class<?> findDynamicProjection() {

		return parameters.hasDynamicProjection() //
				? (Class<?>) values[parameters.getDynamicProjectionIndex()]
				: null;
	}

	/**
	 * Returns the value with the given index.
	 *
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> @Nullable T getValue(int index) {
		return (T) values[index];
	}

	@Override
	public @Nullable Object getBindableValue(int index) {
		return values[parameters.getBindableParameter(index).getIndex()];
	}

	@Override
	public boolean hasBindableNullValue() {

		for (Parameter parameter : parameters.getBindableParameters()) {
			if (values[parameter.getIndex()] == null) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Iterator<Object> iterator() {
		return new BindableParameterIterator(this);
	}

	/**
	 * Iterator class to allow traversing all bindable parameters inside the accessor.
	 *
	 * @author Oliver Gierke
	 */
	private static class BindableParameterIterator implements Iterator<Object> {

		private final int bindableParameterCount;
		private final ParameterAccessor accessor;

		private int currentIndex = 0;

		/**
		 * Creates a new {@link BindableParameterIterator}.
		 *
		 * @param accessor must not be {@literal null}.
		 */
		public BindableParameterIterator(ParametersParameterAccessor accessor) {

			Assert.notNull(accessor, "ParametersParameterAccessor must not be null");

			this.accessor = accessor;
			this.bindableParameterCount = accessor.getParameters().getBindableParameters().getNumberOfParameters();
		}

		/**
		 * Returns the next bindable parameter.
		 *
		 * @return
		 */
		@Override
		public @Nullable Object next() {
			return accessor.getBindableValue(currentIndex++);
		}

		@Override
		public boolean hasNext() {
			return bindableParameterCount > currentIndex;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
