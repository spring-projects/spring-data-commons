/*
 * Copyright 2014-2025 the original author or authors.
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

import java.io.Serial;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link Slice}.
 *
 * @author Oliver Gierke
 * @author Keegan Witt
 * @since 1.8
 */
public class SliceImpl<T> extends Chunk<T> {

	private static final @Serial long serialVersionUID = 867755909294344406L;

	private final boolean hasNext;
	private final Pageable pageable;

	/**
	 * Creates a new {@link Slice} with the given content and {@link Pageable}.
	 *
	 * @param content the content of this {@link Slice}, must not be {@literal null}.
	 * @param pageable the paging information, must not be {@literal null}.
	 * @param hasNext whether there's another slice following the current one.
	 */
	public SliceImpl(List<T> content, Pageable pageable, boolean hasNext) {

		super(content, pageable);

		this.hasNext = hasNext;
		this.pageable = pageable;
	}

	/**
	 * Creates a new {@link SliceImpl} with the given content. This will result in the created {@link Slice} being
	 * identical to the entire {@link List}.
	 *
	 * @param content must not be {@literal null}.
	 */
	public SliceImpl(List<T> content) {
		this(content, Pageable.unpaged(), false);
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public <U> Slice<U> map(Function<? super T, ? extends U> converter) {
		return new SliceImpl<>(getConvertedContent(converter), pageable, hasNext);
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof SliceImpl<?> that)) {
			return false;
		}

		return this.hasNext == that.hasNext && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + (hasNext ? 1 : 0);
	}

	@Override
	public String toString() {

		String contentType = "UNKNOWN";
		List<T> content = getContent();

		if (!content.isEmpty()) {
			contentType = content.get(0).getClass().getName();
		}

		return String.format("Slice %d containing %s instances", getNumber(), contentType);
	}

}
