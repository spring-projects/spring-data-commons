/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.util;

import org.springframework.util.Assert;

/**
 * A tuple of things.
 * 
 * @author Tobias Trelle
 * @author Oliver Gierke
 * @param <T> Type of the first thing.
 * @param <S> Type of the second thing.
 * @since 1.12
 */
public final class Tuple<S, T> {

	private final T second;
	private final S first;

	/**
	 * Creates a new {@link Tuple} for the given elements.
	 * 
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @return
	 */
	Tuple(S first, T second) {

		Assert.notNull(first, "First tuple element must not be null!");
		Assert.notNull(second, "Second tuple element must not be null!");

		this.second = second;
		this.first = first;
	}

	/**
	 * Creates a new {@link Tuple} for the given elements.
	 * 
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @return
	 */
	public static <S, T> Tuple<S, T> of(S first, T second) {
		return new Tuple<S, T>(first, second);
	}

	/**
	 * Returns the first element of the {@link com.querydsl.core.Tuple}.
	 * 
	 * @return
	 */
	public S getFirst() {
		return first;
	}

	/**
	 * Returns the second element of the {@link com.querydsl.core.Tuple}.
	 * 
	 * @return
	 */
	public T getSecond() {
		return second;
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

		if (!(obj instanceof Tuple)) {
			return false;
		}

		Tuple<?, ?> that = (Tuple<?, ?>) obj;

		return this.first.equals(that.first) && this.second.equals(that.second);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 31;

		result += 17 * first.hashCode();
		result += 17 * second.hashCode();

		return result;
	}
}
