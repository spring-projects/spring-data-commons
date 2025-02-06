/*
 * Copyright 2023-2025 the original author or authors.
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

import java.util.Objects;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A {@link ScrollPosition} based on the offsets within query results.
 * <p>
 * An initial {@link OffsetScrollPosition} does not point to a specific element and is different to a position
 * {{@link ScrollPosition#offset(long)}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @author Christoph Strobl
 * @author Yanming Zhou
 * @since 3.1
 */
public final class OffsetScrollPosition implements ScrollPosition {

	private static final OffsetScrollPosition INITIAL = new OffsetScrollPosition(-1);

	private final long offset;

	/**
	 * Creates a new {@link OffsetScrollPosition} for the given non-negative offset.
	 *
	 * @param offset must be greater or equal to zero.
	 */
	private OffsetScrollPosition(long offset) {
		this.offset = offset;
	}

	/**
	 * Creates a new initial {@link OffsetScrollPosition} to start scrolling using offset/limit.
	 *
	 * @return will never be {@literal null}.
	 */
	static OffsetScrollPosition initial() {
		return INITIAL;
	}

	/**
	 * Creates a new {@link OffsetScrollPosition} from an {@code offset}.
	 *
	 * @param offset the non-negative offset to start at.
	 * @return will never be {@literal null}.
	 */
	static OffsetScrollPosition of(long offset) {

		Assert.isTrue(offset >= 0, "Offset must not be negative");
		return new OffsetScrollPosition(offset);
	}

	/**
	 * Returns a {@link IntFunction position function} starting at {@code startOffset}.
	 *
	 * @param startOffset the start offset to be used. Must not be negative.
	 * @return the offset-based position function.
	 */
	public static IntFunction<OffsetScrollPosition> positionFunction(long startOffset) {

		Assert.isTrue(startOffset >= 0, "Start offset must not be negative");
		return startOffset == 0 ? OffsetPositionFunction.ZERO : new OffsetPositionFunction(startOffset);
	}

	/**
	 * Returns the {@link IntFunction position function} starting after the current {@code offset}.
	 *
	 * @return the offset-based position function.
	 * @since 3.3
	 */
	public IntFunction<OffsetScrollPosition> positionFunction() {
		return positionFunction(offset + 1);
	}

	/**
	 * The zero or positive offset.
	 * <p>
	 * An {@link #isInitial() initial} position does not define an offset and will raise an error.
	 *
	 * @return the offset.
	 * @throws IllegalStateException if {@link #isInitial()}.
	 */
	public long getOffset() {

		Assert.state(offset >= 0, "Initial state does not have an offset. Make sure to check #isInitial()");
		return offset;
	}

	/**
	 * Returns a new {@link OffsetScrollPosition} that has been advanced by the given value. Negative deltas will be
	 * constrained so that the new offset is at least zero.
	 *
	 * @param delta the value to advance the current offset by.
	 * @return will never be {@literal null}.
	 */
	public OffsetScrollPosition advanceBy(long delta) {

		long value = isInitial() ? delta : offset + delta;
		return new OffsetScrollPosition(value < 0 ? 0 : value);
	}

	@Override
	public boolean isInitial() {
		return offset == -1;
	}


	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof OffsetScrollPosition that)) {
			return false;
		}

		return Objects.equals(offset, that.offset);
	}

	@Override
	public int hashCode() {
		return Objects.hash(offset);
	}

	@Override
	public String toString() {
		return String.format("OffsetScrollPosition [%s]", offset);
	}

	private record OffsetPositionFunction(long startOffset) implements IntFunction<OffsetScrollPosition> {

		static final OffsetPositionFunction ZERO = new OffsetPositionFunction(0);

		@Override
		public OffsetScrollPosition apply(int offset) {

			if (offset < 0) {
				throw new IndexOutOfBoundsException(offset);
			}

			return of(startOffset + offset);
		}
	}
}
