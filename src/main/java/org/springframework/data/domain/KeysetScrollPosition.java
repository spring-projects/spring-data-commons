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
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A {@link ScrollPosition} based on the last seen key set. Keyset scrolling must be associated with a {@link Sort
 * well-defined sort} to be able to extract the key set when resuming scrolling within the sorted result set.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 3.1
 */
public final class KeysetScrollPosition implements ScrollPosition {

	private static final KeysetScrollPosition INITIAL = new KeysetScrollPosition(Collections.emptyMap(),
			Direction.FORWARD);

	private final Map<String, Object> keys;
	private final Direction direction;

	private KeysetScrollPosition(Map<String, Object> keys, Direction direction) {

		Assert.notNull(keys, "Keys must not be null");
		Assert.notNull(direction, "Direction must not be null");

		this.keys = keys;
		this.direction = direction;
	}

	/**
	 * Creates a new initial {@link KeysetScrollPosition} to start scrolling using keyset-queries.
	 *
	 * @return will never be {@literal null}.
	 */
	static KeysetScrollPosition initial() {
		return INITIAL;
	}

	/**
	 * Creates a new {@link KeysetScrollPosition} from a key set and {@link Direction}.
	 *
	 * @param keys must not be {@literal null}.
	 * @param direction must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	static KeysetScrollPosition of(Map<String, ?> keys, Direction direction) {

		Assert.notNull(keys, "Keys must not be null");
		Assert.notNull(direction, "Direction must not be null");

		return keys.isEmpty()
				? initial()
				: new KeysetScrollPosition(Collections.unmodifiableMap(new LinkedHashMap<>(keys)), direction);
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

	/**
	 * Returns whether the current {@link KeysetScrollPosition} scrolls forward.
	 *
	 * @return whether the current {@link KeysetScrollPosition} scrolls forward.
	 */
	public boolean scrollsForward() {
		return direction == Direction.FORWARD;
	}

	/**
	 * Returns whether the current {@link KeysetScrollPosition} scrolls backward.
	 *
	 * @return whether the current {@link KeysetScrollPosition} scrolls backward.
	 */
	public boolean scrollsBackward() {
		return direction == Direction.BACKWARD;
	}

	/**
	 * Returns a {@link KeysetScrollPosition} based on the same keyset and scrolling forward.
	 *
	 * @return will never be {@literal null}.
	 */
	public KeysetScrollPosition forward() {
		return direction == Direction.FORWARD ? this : new KeysetScrollPosition(keys, Direction.FORWARD);
	}

	/**
	 * Returns a {@link KeysetScrollPosition} based on the same keyset and scrolling backward.
	 *
	 * @return will never be {@literal null}.
	 */
	public KeysetScrollPosition backward() {
		return direction == Direction.BACKWARD ? this : new KeysetScrollPosition(keys, Direction.BACKWARD);
	}

	/**
	 * Returns a new {@link KeysetScrollPosition} with the direction reversed.
	 *
	 * @return will never be {@literal null}.
	 */
	public KeysetScrollPosition reverse() {
		return new KeysetScrollPosition(keys, direction.reverse());
	}

	@Override
	public boolean isInitial() {
		return keys.isEmpty();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof KeysetScrollPosition that)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(keys, that.keys) //
				&& direction == that.direction;
	}

	@Override
	public int hashCode() {
		return Objects.hash(keys, direction);
	}

	@Override
	public String toString() {
		return String.format("KeysetScrollPosition [%s, %s]", direction, keys);
	}
}
