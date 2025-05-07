/*
 * Copyright 2011-2025 the original author or authors.
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Vector;

/**
 * Interface to access method parameters. Allows dedicated access to parameters of special types
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface ParameterAccessor extends Iterable<Object> {

	/**
	 * @return the {@link Vector} of the parameters, if available; {@literal null} otherwise.
	 * @since 4.0
	 */
	default @Nullable Vector getVector() {
		return null;
	}

	/**
	 * @return the {@link Score} of the parameters, if available; {@literal null} otherwise.
	 * @since 4.0
	 */
	default @Nullable Score getScore() {
		return null;
	}

	/**
	 * @return the {@link Range} of {@link Score} of the parameters, if available; {@literal null} otherwise.
	 * @since 4.0
	 */
	default @Nullable Range<Score> getScoreRange() {
		return null;
	}

	/**
	 * @return the {@link ScrollPosition} of the parameters, if available; {@literal null} otherwise.
	 */
	@Nullable
	ScrollPosition getScrollPosition();

	/**
	 * @return the {@link Pageable} of the parameters, if available; {@link Pageable#unpaged()} otherwise.
	 */
	Pageable getPageable();

	/**
	 * @return the sort instance to be used for query creation. Will use a {@link Sort} parameter if available or the
	 *         {@link Sort} contained in a {@link Pageable} if available. {@link Sort#unsorted()} if no {@link Sort} can
	 *         be found.
	 */
	Sort getSort();

	/**
	 * @return the {@link Limit} instance to be used for query creation. If no {@link java.lang.reflect.Parameter}
	 *         assignable to {@link Limit} can be found {@link Limit} will be created out of
	 *         {@link Pageable#getPageSize()} if present.
	 * @since 3.2
	 */
	default Limit getLimit() {
		return getPageable().toLimit();
	}

	/**
	 * @return the dynamic projection type to be used when executing the query or {@literal null} if none is defined.
	 * @since 2.2
	 */
	@Nullable
	Class<?> findDynamicProjection();

	/**
	 * Returns the bindable value with the given index. Bindable means, that {@link Pageable} and {@link Sort} values are
	 * skipped without noticed in the index. For a method signature taking {@link String}, {@link Pageable} ,
	 * {@link String}, {@code #getBindableParameter(1)} would return the second {@link String} value.
	 *
	 * @param index
	 * @return the bindable value with the given index
	 */
	@Nullable
	Object getBindableValue(int index);

	/**
	 * Returns whether one of the bindable parameter values is {@literal null}.
	 *
	 * @return {@literal true} if one of the bindable parameter values is {@literal null}.
	 */
	boolean hasBindableNullValue();

	/**
	 * Returns an iterator over all <em>bindable</em> parameters. This means parameters implementing {@link Pageable} or
	 * {@link Sort} will not be included in this {@link Iterator}.
	 *
	 * @return iterator over all <em>bindable</em> parameters.
	 */
	@Override
	Iterator<Object> iterator();

}
