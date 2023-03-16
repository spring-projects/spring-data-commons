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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A {@link ScrollPosition} based on the last seen keyset. Keyset scrolling must be associated with a {@link Sort
 * well-defined sort} to be able to extract the keyset when resuming scrolling within the sorted result set.
 *
 * @author Mark Paluch
 * @since 3.1
 */
public final class KeysetScrollPosition implements ScrollPosition {

	private static final KeysetScrollPosition initial = new KeysetScrollPosition(Collections.emptyMap(),
			Direction.Forward);

	private final Map<String, Object> keys;

	private final Direction direction;

	private KeysetScrollPosition(Map<String, Object> keys, Direction direction) {
		this.keys = keys;
		this.direction = direction;
	}

	/**
	 * Creates a new initial {@link KeysetScrollPosition} to start scrolling using keyset-queries.
	 *
	 * @return a new initial {@link KeysetScrollPosition} to start scrolling using keyset-queries.
	 */
	public static KeysetScrollPosition initial() {
		return initial;
	}

	/**
	 * Creates a new {@link KeysetScrollPosition} from a keyset.
	 *
	 * @param keys must not be {@literal null}.
	 * @return a new {@link KeysetScrollPosition} for the given keyset.
	 */
	public static KeysetScrollPosition of(Map<String, ?> keys) {
		return of(keys, Direction.Forward);
	}

	/**
	 * Creates a new {@link KeysetScrollPosition} from a keyset and {@link Direction}.
	 *
	 * @param keys must not be {@literal null}.
	 * @param direction must not be {@literal null}.
	 * @return a new {@link KeysetScrollPosition} for the given keyset and {@link Direction}.
	 */
	public static KeysetScrollPosition of(Map<String, ?> keys, Direction direction) {

		Assert.notNull(keys, "Keys must not be null");
		Assert.notNull(direction, "Direction must not be null");

		if (keys.isEmpty()) {
			return initial();
		}

		return new KeysetScrollPosition(Collections.unmodifiableMap(new LinkedHashMap<>(keys)), direction);
	}

	@Override
	public boolean isInitial() {
		return keys.isEmpty();
	}

	/**
	 * @return the keyset.
	 */
	public Map<String, Object> getKeys() {
		return keys;
	}

	/**
	 * @return the scroll direction.
	 */
	public Direction getDirection() {
		return direction;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		KeysetScrollPosition that = (KeysetScrollPosition) o;
		return ObjectUtils.nullSafeEquals(keys, that.keys) && direction == that.direction;
	}

	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * ObjectUtils.nullSafeHashCode(keys);
		result += 31 * ObjectUtils.nullSafeHashCode(direction);

		return result;
	}

	@Override
	public String toString() {
		return String.format("KeysetScrollPosition [%s, %s]", direction, keys);
	}

	/**
	 * Keyset scrolling direction.
	 */
	public enum Direction {

		/**
		 * Forward (default) direction to scroll from the beginning of the results to their end.
		 */
		Forward,

		/**
		 * Backward direction to scroll from the end of the results to their beginning.
		 */
		Backward;
	}
}
