/*
 * Copyright 2013-2021 the original author or authors.
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import com.querydsl.core.types.OrderSpecifier;

/**
 * Basic Java Bean implementation of {@link Pageable} with support for QueryDSL.
 *
 * @author Thomas Darimont
 * @author Oliver Drotbohm
 */
public class QPageRequest extends AbstractPageRequest {

	private static final long serialVersionUID = 7529171950267879273L;

	private final QSort sort;

	/**
	 * Creates a new {@link QPageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return the first
	 * page.
	 *
	 * @param page must not be negative.
	 * @param size must be greater or equal to 0.
	 * @deprecated since 2.1, use {@link #of(int, int)} instead.
	 */
	@Deprecated
	public QPageRequest(int page, int size) {
		this(page, size, QSort.unsorted());
	}

	/**
	 * Creates a new {@link QPageRequest} with the given {@link OrderSpecifier}s applied.
	 *
	 * @param page must not be negative.
	 * @param size must be greater or equal to 0.
	 * @param orderSpecifiers must not be {@literal null} or empty;
	 * @deprecated since 2.1, use {@link #of(int, int, OrderSpecifier...)} instead.
	 */
	@Deprecated
	public QPageRequest(int page, int size, OrderSpecifier<?>... orderSpecifiers) {
		this(page, size, new QSort(orderSpecifiers));
	}

	/**
	 * Creates a new {@link QPageRequest} with sort parameters applied.
	 *
	 * @param page must not be negative.
	 * @param size must be greater or equal to 0.
	 * @param sort must not be {@literal null}.
	 * @deprecated since 2.1, use {@link #of(int, int, QSort)} instead.
	 */
	@Deprecated
	public QPageRequest(int page, int size, QSort sort) {

		super(page, size);

		Assert.notNull(sort, "QSort must not be null!");

		this.sort = sort;
	}

	/**
	 * Creates a new {@link QPageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return the first
	 * page.
	 *
	 * @param page must not be negative.
	 * @param size must be greater or equal to 0.
	 * @since 2.1
	 */
	public static QPageRequest of(int page, int size) {
		return new QPageRequest(page, size, QSort.unsorted());
	}

	/**
	 * Creates a new {@link QPageRequest} with the given {@link OrderSpecifier}s applied.
	 *
	 * @param page must not be negative.
	 * @param size must be greater or equal to 0.
	 * @param orderSpecifiers must not be {@literal null} or empty;
	 * @since 2.1
	 */
	public static QPageRequest of(int page, int size, OrderSpecifier<?>... orderSpecifiers) {
		return new QPageRequest(page, size, new QSort(orderSpecifiers));
	}

	/**
	 * Creates a new {@link QPageRequest} with sort parameters applied.
	 *
	 * @param page must not be negative.
	 * @param size must be greater or equal to 0.
	 * @param sort must not be {@literal null}.
	 * @since 2.1
	 */
	public static QPageRequest of(int page, int size, QSort sort) {
		return new QPageRequest(page, size, sort);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getSort()
	 */
	@Override
	public Sort getSort() {
		return sort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractPageRequest#next()
	 */
	@Override
	public Pageable next() {
		return QPageRequest.of(getPageNumber() + 1, getPageSize(), sort);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractPageRequest#previous()
	 */
	@Override
	public Pageable previous() {
		return QPageRequest.of(getPageNumber() - 1, getPageSize(), sort);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractPageRequest#first()
	 */
	@Override
	public Pageable first() {
		return QPageRequest.of(0, getPageSize(), sort);
	}
}
