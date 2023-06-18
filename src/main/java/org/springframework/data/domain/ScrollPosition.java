/*
 * Copyright 2023 the original author or authors.
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

import java.util.Map;

/**
 * Interface to specify a position within a total query result. Scroll positions are used to start scrolling from the
 * beginning of a query result or to resume scrolling from a given position within the query result.
 *
 * @author Mark Paluch
 * @since 3.1
 */
public interface ScrollPosition {

	/**
	 * Returns whether the current scroll position is the initial one.
	 *
	 * @return
	 */
	boolean isInitial();

	/**
	 * Creates a new initial {@link ScrollPosition} to start scrolling using keyset-queries.
	 *
	 * @return will never be {@literal null}.
	 */
	static KeysetScrollPosition keyset() {
		return KeysetScrollPosition.initial();
	}

	/**
	 * Creates a new initial {@link ScrollPosition} to start scrolling using offset / limit.
	 *
	 * @return will never be {@literal null}.
	 */
	static OffsetScrollPosition offset() {
		return OffsetScrollPosition.initial();
	}

	/**
	 * Creates a new {@link ScrollPosition} from an {@code offset}.
	 *
	 * @param offset
	 * @return a new {@link OffsetScrollPosition} with the given {@code offset}.
	 */
	static OffsetScrollPosition offset(long offset) {
		return OffsetScrollPosition.of(offset);
	}

	/**
	 * Creates a new {@link ScrollPosition} from a key set scrolling forward.
	 *
	 * @param keys must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	static KeysetScrollPosition forward(Map<String, ?> keys) {
		return of(keys, Direction.FORWARD);
	}

	/**
	 * Creates a new {@link ScrollPosition} from a key set scrolling backward.
	 *
	 * @param keys must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	static KeysetScrollPosition backward(Map<String, ?> keys) {
		return of(keys, Direction.BACKWARD);
	}

	/**
	 * Creates a new {@link ScrollPosition} from a key set and {@link Direction}.
	 *
	 * @param keys must not be {@literal null}.
	 * @param direction must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	static KeysetScrollPosition of(Map<String, ?> keys, Direction direction) {
		return KeysetScrollPosition.of(keys, direction);
	}

	/**
	 * Keyset scrolling direction.
	 */
	enum Direction {

		/**
		 * Forward (default) direction to scroll from the beginning of the results to their end.
		 */
		FORWARD,

		/**
		 * Backward direction to scroll from the end of the results to their beginning.
		 */
		BACKWARD;

		Direction reverse() {
			return this == FORWARD ? BACKWARD : FORWARD;
		}
	}
}
