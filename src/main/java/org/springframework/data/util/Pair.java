/*
 * Copyright 2015-2016 the original author or authors.
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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A tuple of things.
 * 
 * @author Tobias Trelle
 * @author Oliver Gierke
 * @param <S> Type of the first thing.
 * @param <T> Type of the second thing.
 * @since 1.12
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Pair<S, T> {

	private final @NonNull S first;
	private final @NonNull T second;

	/**
	 * Creates a new {@link Pair} for the given elements.
	 * 
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 * @return
	 */
	public static <S, T> Pair<S, T> of(S first, T second) {
		return new Pair<S, T>(first, second);
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

	public static <S, T> Collector<Pair<S, T>, ?, Map<S, T>> toMap() {
		return Collectors.toMap(Pair::getFirst, Pair::getSecond);
	}
}
