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
 * Immutable value object representing a search result consisting of a content item and an associated {@link Score}.
 * <p>
 * Typically used in the context of similarity-based or vector search operations where each result carries a relevance
 * {@link Score}. Provides accessor methods for the content and its score, along with transformation support via
 * {@link #map(Function)}.
 *
 * @param <T> the type of the content object
 * @author Mark Paluch
 * @since 4.0
 * @see Score
 * @see Similarity
 */
public final class SearchResult<T> implements Serializable {

	private static final @Serial long serialVersionUID = 1637452570977581370L;

	private final T content;
	private final Score score;

	/**
	 * Creates a new {@link SearchResult} with the given content and {@link Score}.
	 *
	 * @param content the result content, must not be {@literal null}.
	 * @param score the result score, must not be {@literal null}.
	 */
	public SearchResult(T content, Score score) {

		Assert.notNull(content, "Content must not be null");
		Assert.notNull(score, "Score must not be null");

		this.content = content;
		this.score = score;
	}

	/**
	 * Create a new {@link SearchResult} with the given content and a raw score value.
	 *
	 * @param content the result content, must not be {@literal null}.
	 * @param score the score value.
	 */
	public SearchResult(T content, double score) {
		this(content, Score.of(score));
	}

	/**
	 * Returns the content associated with this result.
	 */
	public T getContent() {
		return this.content;
	}

	/**
	 * Returns the {@link Score} associated with this result.
	 */
	public Score getScore() {
		return this.score;
	}

	/**
	 * Creates a new {@link SearchResult} by applying the given mapping {@link Function} to this result's content.
	 *
	 * @param converter the mapping function to apply to the content, must not be {@literal null}.
	 * @return a new {@link SearchResult} instance with converted content.
	 * @param <U> the target type of the mapped content.
	 */
	public <U> SearchResult<U> map(Function<? super T, ? extends U> converter) {

		Assert.notNull(converter, "Function must not be null");

		return new SearchResult<>(converter.apply(getContent()), getScore());
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

}
