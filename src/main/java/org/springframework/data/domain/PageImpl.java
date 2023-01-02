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
package org.springframework.data.domain;

import java.util.List;
import java.util.function.Function;

import org.springframework.lang.Nullable;

/**
 * Basic {@code Page} implementation.
 *
 * @param <T> the type of which the page consists.
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class PageImpl<T> extends Chunk<T> implements Page<T> {

	private static final long serialVersionUID = 867755909294344406L;

	private final long total;

	/**
	 * Constructor of {@code PageImpl}.
	 *
	 * @param content the content of this page, must not be {@literal null}.
	 * @param pageable the paging information, must not be {@literal null}.
	 * @param total the total amount of items available. The total might be adapted considering the length of the content
	 *          given, if it is going to be the content of the last page. This is in place to mitigate inconsistencies.
	 */
	public PageImpl(List<T> content, Pageable pageable, long total) {

		super(content, pageable);

		this.total = pageable.toOptional().filter(it -> !content.isEmpty())//
				.filter(it -> it.getOffset() + it.getPageSize() > total)//
				.map(it -> it.getOffset() + content.size())//
				.orElse(total);
	}

	/**
	 * Creates a new {@link PageImpl} with the given content. This will result in the created {@link Page} being identical
	 * to the entire {@link List}.
	 *
	 * @param content must not be {@literal null}.
	 */
	public PageImpl(List<T> content) {
		this(content, Pageable.unpaged(), null == content ? 0 : content.size());
	}

	@Override
	public int getTotalPages() {
		return getSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getSize());
	}

	@Override
	public long getTotalElements() {
		return total;
	}

	@Override
	public boolean hasNext() {
		return getNumber() + 1 < getTotalPages();
	}

	@Override
	public boolean isLast() {
		return !hasNext();
	}

	@Override
	public <U> Page<U> map(Function<? super T, ? extends U> converter) {
		return new PageImpl<>(getConvertedContent(converter), getPageable(), total);
	}

	@Override
	public String toString() {

		String contentType = "UNKNOWN";
		List<T> content = getContent();

		if (!content.isEmpty() && content.get(0) != null) {
			contentType = content.get(0).getClass().getName();
		}

		return String.format("Page %s of %d containing %s instances", getNumber() + 1, getTotalPages(), contentType);
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof PageImpl<?> that)) {
			return false;
		}

		return this.total == that.total && super.equals(obj);
	}

	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * (int) (total ^ total >>> 32);
		result += 31 * super.hashCode();

		return result;
	}
}
