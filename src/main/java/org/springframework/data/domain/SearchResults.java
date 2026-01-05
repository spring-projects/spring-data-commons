/*
 * Copyright 2025-present the original author or authors.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object encapsulating a collection of {@link SearchResult} instances.
 * <p>
 * Typically used as the result type for search or similarity queries, exposing access to the result content and
 * supporting mapping operations to transform the result content type.
 *
 * @param <T> the type of content contained within each {@link SearchResult}.
 * @author Mark Paluch
 * @since 4.0
 * @see SearchResult
 */
public class SearchResults<T> implements Iterable<SearchResult<T>>, Serializable {

	private final List<? extends SearchResult<T>> results;

	/**
	 * Creates a new {@link SearchResults} instance from the given list of {@link SearchResult} items.
	 *
	 * @param results the search results to encapsulate, must not be {@code null}
	 */
	public SearchResults(List<? extends SearchResult<T>> results) {
		this.results = results;
	}

	/**
	 * Return the actual content of the {@link SearchResult} items as an unmodifiable list.
	 */
	public List<SearchResult<T>> getContent() {
		return Collections.unmodifiableList(results);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<SearchResult<T>> iterator() {
		return (Iterator<SearchResult<T>>) results.iterator();
	}

	/**
	 * Returns a sequential {@link Stream} containing {@link SearchResult} items in this {@code SearchResults} instance.
	 *
	 * @return a sequential {@link Stream} containing {@link SearchResult} items in this {@code SearchResults} instance.
	 */
	public Stream<SearchResult<T>> stream() {
		return Streamable.of(this).stream();
	}

	/**
	 * Returns a sequential {@link Stream} containing {@link #getContent() unwrapped content} items in this
	 * {@code SearchResults} instance.
	 *
	 * @return a sequential {@link Stream} containing {@link #getContent() unwrapped content} items in this
	 *         {@code SearchResults} instance.
	 */
	public Stream<T> contentStream() {
		return getContent().stream().map(SearchResult::getContent);
	}

	/**
	 * Creates a new {@code SearchResults} instance with the content of the current results mapped via the given
	 * {@link Function}.
	 *
	 * @param converter the mapping function to apply to the content of each {@link SearchResult}, must not be
	 *          {@literal null}.
	 * @param <U> the target type of the mapped content.
	 * @return a new {@code SearchResults} instance containing mapped result content.
	 */
	public <U> SearchResults<U> map(Function<? super T, ? extends U> converter) {

		Assert.notNull(converter, "Function must not be null");

		List<SearchResult<U>> result = results.stream().map(it -> it.<U> map(converter)).collect(Collectors.toList());

		return new SearchResults<>(result);
	}

	@Override
	public boolean equals(Object o) {

		if (o == this) {
			return true;
		}

		if (!(o instanceof SearchResults<?> that)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(results, that.results);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(results);
	}

	@Override
	public String toString() {
		return results.isEmpty() ? "SearchResults [empty]" : String.format("SearchResults [size: %s]", results.size());
	}

}
