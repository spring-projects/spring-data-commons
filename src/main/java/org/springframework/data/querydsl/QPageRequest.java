/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.querydsl;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import com.querydsl.core.types.OrderSpecifier;

/**
 * Basic Java Bean implementation of {@link Pageable} with support for QueryDSL.
 *
 * @author Thomas Darimont
 * @author Oliver Drotbohm
 * @author Mark Paluch
 * @author Thach Le
 */
public class QPageRequest extends AbstractPageRequest {

	private static final long serialVersionUID = 7529171950267879273L;

	private final QSort sort;

	/**
	 * Creates a new {@link QPageRequest}. Pages are zero indexed, thus providing 0 for {@code pageNumber} will return the first
	 * pageNumber.
	 *
	 * @param pageNumber must not be negative.
	 * @param pageSize must be greater or equal to 0.
	 * @deprecated since 2.1, use {@link #of(int, int)} instead.
	 */
	@Deprecated
	public QPageRequest(int pageNumber, int pageSize) {
		this(pageNumber, pageSize, QSort.unsorted());
	}

	/**
	 * Creates a new {@link QPageRequest} with the given {@link OrderSpecifier}s applied.
	 *
	 * @param pageNumber must not be negative.
	 * @param pageSize must be greater or equal to 0.
	 * @param orderSpecifiers must not be {@literal null} or empty;
	 * @deprecated since 2.1, use {@link #of(int, int, OrderSpecifier...)} instead.
	 */
	@Deprecated
	public QPageRequest(int pageNumber, int pageSize, OrderSpecifier<?>... orderSpecifiers) {
		this(pageNumber, pageSize, new QSort(orderSpecifiers));
	}

	/**
	 * Creates a new {@link QPageRequest} with sort parameters applied.
	 *
	 * @param pageNumber must not be negative.
	 * @param pageSize must be greater or equal to 0.
	 * @param sort must not be {@literal null}.
	 * @deprecated since 2.1, use {@link #of(int, int, QSort)} instead.
	 */
	@Deprecated
	public QPageRequest(int pageNumber, int pageSize, QSort sort) {

		super(pageNumber, pageSize);

		Assert.notNull(sort, "QSort must not be null");

		this.sort = sort;
	}

	/**
	 * Creates a new {@link QPageRequest}. Pages are zero indexed, thus providing 0 for {@code pageNumber} will return the first
	 * pageNumber.
	 *
	 * @param pageNumber must not be negative.
	 * @param pageSize must be greater or equal to 0.
	 * @since 2.1
	 */
	public static QPageRequest of(int pageNumber, int pageSize) {
		return new QPageRequest(pageNumber, pageSize, QSort.unsorted());
	}

	/**
	 * Creates a new {@link QPageRequest} with the given {@link OrderSpecifier}s applied.
	 *
	 * @param pageNumber must not be negative.
	 * @param pageSize must be greater or equal to 0.
	 * @param orderSpecifiers must not be {@literal null} or empty;
	 * @since 2.1
	 */
	public static QPageRequest of(int pageNumber, int pageSize, OrderSpecifier<?>... orderSpecifiers) {
		return new QPageRequest(pageNumber, pageSize, new QSort(orderSpecifiers));
	}

	/**
	 * Creates a new {@link QPageRequest} with sort parameters applied.
	 *
	 * @param pageNumber must not be negative.
	 * @param pageSize must be greater or equal to 0.
	 * @param sort must not be {@literal null}.
	 * @since 2.1
	 */
	public static QPageRequest of(int pageNumber, int pageSize, QSort sort) {
		return new QPageRequest(pageNumber, pageSize, sort);
	}

	/**
	 * Creates a new {@link QPageRequest} for the first page (page number {@code 0}) given {@code pageSize} .
	 *
	 * @param pageSize the size of the page to be returned, must be greater than 0.
	 * @return a new {@link QPageRequest}.
	 * @since 2.5
	 */
	public static QPageRequest ofSize(int pageSize) {
		return QPageRequest.of(0, pageSize);
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public Pageable next() {
		return QPageRequest.of(getPageNumber() + 1, getPageSize(), sort);
	}

	@Override
	public Pageable previous() {
		return QPageRequest.of(getPageNumber() - 1, getPageSize(), sort);
	}

	@Override
	public Pageable first() {
		return QPageRequest.of(0, getPageSize(), sort);
	}

	/**
	 * Creates a new {@link QPageRequest} with {@code pageNumber} applied.
	 *
	 * @param pageNumber
	 * @return a new {@link PageRequest}.
	 * @since 2.5
	 */
	@Override
	public QPageRequest withPage(int pageNumber) {
		return new QPageRequest(pageNumber, getPageSize(), sort);
	}

	/**
	 * Creates a new {@link QPageRequest} with {@link QSort} applied.
	 *
	 * @param sort must not be {@literal null}.
	 * @return a new {@link PageRequest}.
	 * @since 2.5
	 */
	public QPageRequest withSort(QSort sort) {
		return new QPageRequest(getPageNumber(), getPageSize(), sort);
	}
}
