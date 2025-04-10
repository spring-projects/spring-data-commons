/*
 * Copyright 2025 the original author or authors.
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
import java.io.Serializable;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object capturing some arbitrary object plus a distance.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public final class SearchResult<T> implements Serializable {

	private static final @Serial long serialVersionUID = 1637452570977581370L;

	private final T content;
	private final Score score;

	public SearchResult(T content, Score score) {

		Assert.notNull(content, "Content must not be null");
		Assert.notNull(score, "Score must not be null");

		this.content = content;
		this.score = score;
	}

	public SearchResult(T content, double score) {
		this(content, Score.of(score));
	}

	public T getContent() {
		return this.content;
	}

	public Score getScore() {
		return this.score;
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof SearchResult<?> result)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(content, result.content)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(score, result.score);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(content, score);
	}

	@Override
	public String toString() {
		return String.format("SearchResult [content: %s, score: %s]", content, score);
	}

	/**
	 * Returns new {@link SearchResults} with the content of the current one mapped by the given {@link Function}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return a new {@link SearchResults} with the content of the current one mapped by the given {@link Function}.
	 */
	public <U> SearchResult<U> map(Function<? super T, ? extends U> converter) {

		Assert.notNull(converter, "Function must not be null");

		return new SearchResult<>(converter.apply(getContent()), getScore());
	}

}
