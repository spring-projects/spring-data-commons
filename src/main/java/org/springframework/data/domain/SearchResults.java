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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Value object to capture {@link SearchResult}s.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class SearchResults<T> implements Iterable<SearchResult<T>>, Serializable {

	private final List<? extends SearchResult<T>> results;

	public SearchResults(List<SearchResult<T>> results) {
		this.results = results;
	}

	/**
	 * Returns the actual content of the {@link SearchResult}s.
	 *
	 * @return the actual content.
	 */
	public List<SearchResult<T>> getContent() {
		return Collections.unmodifiableList(results);
	}

	@Override
	public Iterator<SearchResult<T>> iterator() {
		return (Iterator<SearchResult<T>>) results.iterator();
	}

	/**
	 * Returns new {@link SearchResults} with the content of the current one mapped by the given {@link Function}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return a new {@link SearchResults} with the content of the current one mapped by the given {@link Function}.
	 */
	public <U> SearchResults<U> map(Function<? super T, ? extends U> converter) {

		Assert.notNull(converter, "Function must not be null");

		List<SearchResult<U>> result = results.stream().map(it -> {

			SearchResult<U> mapped = it.map(converter);
			return mapped;
		}).collect(Collectors.toList());

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
		return String.format("SearchResults: [results: %s]", StringUtils.collectionToCommaDelimitedString(results));
	}

}
