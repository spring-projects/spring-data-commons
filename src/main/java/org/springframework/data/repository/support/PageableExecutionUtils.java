/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.support;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Support for query execution using {@link Pageable}. Using {@link PageableExecutionUtils} assumes that data queries
 * are cheaper than {@code COUNT} queries and so some cases can take advantage of optimizations.
 *
 * @author Mark Paluch
 * @since 1.13
 */
public abstract class PageableExecutionUtils {

	private PageableExecutionUtils() {}

	/**
	 * Constructs a {@link Page} based on the given {@code content}, {@link Pageable} and {@link TotalSupplier} applying
	 * optimizations. The construction of {@link Page} omits a count query if the total can be determined based on the
	 * result size and {@link Pageable}.
	 *
	 * @param content must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @param totalSupplier must not be {@literal null}.
	 * @return the {@link Page}.
	 */
	public static <T> Page<T> getPage(List<T> content, Pageable pageable, TotalSupplier totalSupplier) {

		Assert.notNull(content, "Content must not be null!");
		Assert.notNull(totalSupplier, "TotalSupplier must not be null!");

		if (pageable == null || pageable.getOffset() == 0) {

			if (pageable == null || pageable.getPageSize() > content.size()) {
				return new PageImpl<T>(content, pageable, content.size());
			}

			return new PageImpl<T>(content, pageable, totalSupplier.get());
		}

		if (content.size() != 0 && pageable.getPageSize() > content.size()) {
			return new PageImpl<T>(content, pageable, pageable.getOffset() + content.size());
		}

		return new PageImpl<T>(content, pageable, totalSupplier.get());
	}

	/**
	 * Supplies the total count for a particular query. Can be replaced with a Java 8 Supplier when upgrading to Java 8.
	 *
	 * @author Mark Paluch
	 */
	public interface TotalSupplier {

		/**
		 * @return the total count for a particular query.
		 */
		long get();
	}
}
