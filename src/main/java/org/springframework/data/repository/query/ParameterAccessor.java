/*
 * Copyright 2011-2023 the original author or authors.
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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * Interface to access method parameters. Allows dedicated access to parameters of special types
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface ParameterAccessor extends Iterable<Object> {

	/**
	 * Returns the {@link ScrollPosition} of the parameters, if available. Returns {@code null} otherwise.
	 *
	 * @return
	 */
	@Nullable
	ScrollPosition getScrollPosition();

	/**
	 * Returns the {@link Pageable} of the parameters, if available. Returns {@link Pageable#unpaged()} otherwise.
	 *
	 * @return
	 */
	Pageable getPageable();

	/**
	 * Returns the sort instance to be used for query creation. Will use a {@link Sort} parameter if available or the
	 * {@link Sort} contained in a {@link Pageable} if available. Returns {@link Sort#unsorted()} if no {@link Sort} can
	 * be found.
	 *
	 * @return
	 */
	Sort getSort();

	/**
	 * Returns the dynamic projection type to be used when executing the query or {@literal null} if none is defined.
	 *
	 * @return
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
	 * @return
	 */
	@Nullable
	Object getBindableValue(int index);

	/**
	 * Returns whether one of the bindable parameter values is {@literal null}.
	 *
	 * @return
	 */
	boolean hasBindableNullValue();

	/**
	 * Returns an iterator over all <em>bindable</em> parameters. This means parameters implementing {@link Pageable} or
	 * {@link Sort} will not be included in this {@link Iterator}.
	 *
	 * @return
	 */
	Iterator<Object> iterator();
}
