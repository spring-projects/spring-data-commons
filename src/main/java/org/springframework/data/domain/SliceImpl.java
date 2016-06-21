/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.domain;

import java.util.List;

import org.springframework.core.convert.converter.Converter;

/**
 * Default implementation of {@link Slice}.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
public class SliceImpl<T> extends Chunk<T> {

	private static final long serialVersionUID = 867755909294344406L;

	private final boolean hasNext;
	private final Pageable pageable;

	/**
	 * Creates a new {@link Slice} with the given content and {@link Pageable}.
	 * 
	 * @param content the content of this {@link Slice}, must not be {@literal null}.
	 * @param pageable the paging information, can be {@literal null}.
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
		this(content, null, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Slice#hasNext()
	 */
	public boolean hasNext() {
		return hasNext;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Slice#transform(org.springframework.core.convert.converter.Converter)
	 */
	@Override
	public <U> Slice<U> map(Converter<? super T, ? extends U> converter) {
		return new SliceImpl<>(getConvertedContent(converter), pageable, hasNext);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		String contentType = "UNKNOWN";
		List<T> content = getContent();

		if (content.size() > 0) {
			contentType = content.get(0).getClass().getName();
		}

		return String.format("Slice %d containing %s instances", getNumber(), contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof SliceImpl<?>)) {
			return false;
		}

		SliceImpl<?> that = (SliceImpl<?>) obj;

		return this.hasNext == that.hasNext && super.equals(obj);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * (hasNext ? 1 : 0);
		result += 31 * super.hashCode();

		return result;
	}
}
