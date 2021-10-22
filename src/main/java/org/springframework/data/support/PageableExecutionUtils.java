/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.support;

import java.util.List;
import java.util.function.LongSupplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Support for query execution using {@link Pageable}. Using {@link PageableExecutionUtils} assumes that data queries
 * are cheaper than {@code COUNT} queries and so some cases can take advantage of optimizations.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @since 2.4
 */
public abstract class PageableExecutionUtils {

	private PageableExecutionUtils() {}

	/**
	 * Constructs a {@link Page} based on the given {@code content}, {@link Pageable} and {@link LongSupplier} applying
	 * optimizations. The construction of {@link Page} omits a count query if the total can be determined based on the
	 * result size and {@link Pageable}.
	 *
	 * @param content must not be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 * @param totalSupplier must not be {@literal null}.
	 * @return the {@link Page}.
	 */
	public static <T> Page<T> getPage(List<T> content, Pageable pageable, LongSupplier totalSupplier) {

		Assert.notNull(content, "Content must not be null!");
		Assert.notNull(pageable, "Pageable must not be null!");
		Assert.notNull(totalSupplier, "TotalSupplier must not be null!");

		if (pageable.isUnpaged() || pageable.getOffset() == 0) {

			if (pageable.isUnpaged() || pageable.getPageSize() > content.size()) {
				return new PageImpl<>(content, pageable, content.size());
			}

			return new PageImpl<>(content, pageable, totalSupplier.getAsLong());
		}

		if (content.size() != 0 && pageable.getPageSize() > content.size()) {
			return new PageImpl<>(content, pageable, pageable.getOffset() + content.size());
		}

		return new PageImpl<>(content, pageable, totalSupplier.getAsLong());
	}
}
