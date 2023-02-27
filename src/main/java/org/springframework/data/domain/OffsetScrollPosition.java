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

import java.util.function.IntFunction;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A {@link ScrollPosition} based on the offsets within query results.
 *
 * @author Mark Paluch
 * @since 3.1
 */
public final class OffsetScrollPosition implements ScrollPosition {

	private static final OffsetScrollPosition initial = new OffsetScrollPosition(0);

	private final long offset;

	private OffsetScrollPosition(long offset) {
		this.offset = offset;
	}

	/**
	 * Creates a new initial {@link OffsetScrollPosition} to start scrolling using offset/limit.
	 *
	 * @return a new initial {@link OffsetScrollPosition} to start scrolling using offset/limit.
	 */
	public static OffsetScrollPosition initial() {
		return initial;
	}

	/**
	 * Creates a new {@link OffsetScrollPosition} from an {@code offset}.
	 *
	 * @param offset
	 * @return a new {@link OffsetScrollPosition} with the given {@code offset}.
	 */
	public static OffsetScrollPosition of(long offset) {

		if (offset == 0) {
			return initial();
		}

		return new OffsetScrollPosition(offset);
	}

	/**
	 * Returns the {@link IntFunction position function} to calculate.
	 *
	 * @param startOffset the start offset to be used. Must not be negative.
	 * @return the offset-based position function.
	 */
	public static IntFunction<OffsetScrollPosition> positionFunction(long startOffset) {

		Assert.isTrue(startOffset >= 0, "Start offset must not be negative");

		return startOffset == 0 ? OffsetPositionFunction.ZERO : new OffsetPositionFunction(startOffset);
	}

	@Override
	public boolean isInitial() {
		return offset == 0;
	}

	/**
	 * @return the offset.
	 */
	public long getOffset() {
		return offset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		OffsetScrollPosition that = (OffsetScrollPosition) o;
		return offset == that.offset;
	}

	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * ObjectUtils.nullSafeHashCode(offset);

		return result;
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

			return of(startOffset + offset + 1);
		}
	}
}
