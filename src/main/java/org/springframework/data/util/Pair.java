/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.util;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A tuple of things.
 *
 * @author Tobias Trelle
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @param <S> Type of the first thing.
 * @param <T> Type of the second thing.
 * @since 1.12
 */
public final class Pair<S, T> {

	private final S first;
	private final T second;

	private Pair(S first, T second) {

		Assert.notNull(first, "First must not be null!");
		Assert.notNull(second, "Second must not be null!");

		this.first = first;
		this.second = second;
	}

	/**
	 * Creates a new {@link Pair} for the given elements.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @return
	 */
	public static <S, T> Pair<S, T> of(S first, T second) {
		return new Pair<>(first, second);
	}

	/**
	 * Returns the first element of the {@link Pair}.
	 *
	 * @return
	 */
	public S getFirst() {
		return first;
	}

	/**
	 * Returns the second element of the {@link Pair}.
	 *
	 * @return
	 */
	public T getSecond() {
		return second;
	}

	/**
	 * A collector to create a {@link Map} from a {@link Stream} of {@link Pair}s.
	 *
	 * @return
	 */
	public static <S, T> Collector<Pair<S, T>, ?, Map<S, T>> toMap() {
		return Collectors.toMap(Pair::getFirst, Pair::getSecond);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Pair)) {
			return false;
		}

		Pair<?, ?> pair = (Pair<?, ?>) o;

		if (!ObjectUtils.nullSafeEquals(first, pair.first)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(second, pair.second);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(first);
		result = 31 * result + ObjectUtils.nullSafeHashCode(second);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s->%s", this.first, this.second);
	}
}
