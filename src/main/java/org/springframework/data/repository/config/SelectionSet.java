/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.repository.config;

import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Allows filtering of a collection in order to select a unique element. Once a unique element is found all further
 * filters are ignored.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @since 2.0
 */
@RequiredArgsConstructor(staticName = "of")
class SelectionSet<T> {

	private final Collection<T> collection;
	private final Function<Collection<T>, Optional<T>> fallback;

	/**
	 * creates a {@link SelectionSet} with a default fallback of {@literal null}, when no element is found and an
	 * {@link IllegalStateException} when no element is found.
	 */
	static <T> SelectionSet<T> of(Collection<T> collection) {
		return new SelectionSet<>(collection, defaultFallback());
	}

	/**
	 * If this {@code SelectionSet} contains exactly one element it gets returned. If no unique result can be identified
	 * the fallback function passed in at the constructor gets called and its return value becomes the return value of
	 * this method.
	 *
	 * @return a unique result, or the result of the callback provided in the constructor.
	 */
	Optional<T> uniqueResult() {

		Optional<T> uniqueResult = findUniqueResult();

		return uniqueResult.isPresent() ? uniqueResult : fallback.apply(collection);
	}

	/**
	 * Filters the collection with the predicate if there are still more then one elements in the collection.
	 *
	 * @param predicate To be used for filtering.
	 */
	SelectionSet<T> filterIfNecessary(Predicate<T> predicate) {

		return findUniqueResult().map(it -> this).orElseGet(
				() -> new SelectionSet<T>(collection.stream().filter(predicate).collect(Collectors.toList()), fallback));
	}

	private static <S> Function<Collection<S>, Optional<S>> defaultFallback() {

		return c -> {
			if (c.isEmpty()) {
				return Optional.empty();
			} else {
				throw new IllegalStateException("More then one element in collection.");
			}
		};
	}

	private Optional<T> findUniqueResult() {
		return Optional.ofNullable(collection.size() == 1 ? collection.iterator().next() : null);
	}
}
