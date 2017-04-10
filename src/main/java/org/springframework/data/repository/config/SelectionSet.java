/*
 * Copyright 2017 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Allows filtering of a collection in order to select a unique element. Once a unique element is found all further
 * filters are ignored.
 *
 * @author Jens Schauder
 */
class SelectionSet<T> {

	private final Collection<T> collection;
	private final Function<Collection<T>, T> fallback;

	/**
	 * creates a {@link SelectionSet} with a default fallback of {@literal null}, when no element is found and an
	 * {@link IllegalStateException} when no element is found.
	 */
	SelectionSet(Collection<T> collection) {
		this(collection, defaultFallback());
	}

	/**
	 * @param collection The collection from which a unique element will get picked.
	 * @param fallback Will get called once {@literal #uniqueResult} gets called if no unique result can be determined.
	 */
	SelectionSet(Collection<T> collection, Function<Collection<T>, T> fallback) {

		this.collection = collection;
		this.fallback = fallback;
	}

	/**
	 * If this <code>SelectionSet</code> contains exactly one element it gets returned. If no unique result can
	 * be identified the fallback function passed in at the constructor gets called and its return value becomes
	 * the return value of this method.
	 *
	 * @return a unique result, or the result of the callback provided in the constructor.
	 */
	T uniqueResult() {
		T uniqueResult = findUniqueResult();

		return uniqueResult != null ? uniqueResult : fallback.apply(collection);
	}

	/**
	 * Filters the collection with the predicate if there are still more then one elements in the collection.
	 *
	 * @param predicate To be used for filtering.
	 */
	SelectionSet<T> filterIfNecessary(Predicate<T> predicate) {

		if (findUniqueResult() != null) {
			return this;
		}

		List<T> fillteredList = collection.stream().filter(predicate).collect(Collectors.toList());
		return new SelectionSet<T>(fillteredList, fallback);
	}

	private static <S> Function<Collection<S>, S> defaultFallback() {

		return c -> {
			if (c.isEmpty()) {
				return null;
			} else {
				throw new IllegalStateException("More then one element in collection.");
			}
		};
	}

	private T findUniqueResult() {
		return collection.size() == 1 ? collection.iterator().next() : null;
	}
}
