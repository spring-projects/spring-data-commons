/*
 * Copyright 2023-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Limit.Limited;
import org.springframework.data.domain.Limit.Unlimited;

/**
 * {@link Limit} represents the maximum value up to which an operation should continue processing. It may be used for
 * defining the {@link #max() maximum} number of results within a repository finder method or if applicable a template
 * operation.
 * <p>
 * A {@link Limit#isUnlimited()} is used to indicate that there is no {@link Limit} defined, which should be favored
 * over using {@literal null} or {@link java.util.Optional#empty()} to indicate the absence of an actual {@link Limit}.
 * </p>
 * {@link Limit} itself does not make assumptions about the actual {@link #max()} value sign. This means that a negative
 * value may be valid within a defined context. A zero limit can be useful in cases where the result is not needed but
 * the underlying activity to compute results might be required.
 * <p>
 * Note that using a zero Limit with repository query methods returning {@link Page} is rejected because of a zero-page
 * size.
 *
 * @author Christoph Strobl
 * @author Oliver Drotbohm
 * @since 3.2
 */
public sealed interface Limit permits Limited, Unlimited {

	/**
	 * @return a {@link Limit} instance that does not define {@link #max()} and answers {@link #isUnlimited()} with
	 *         {@literal true}.
	 */
	static Limit unlimited() {
		return Unlimited.INSTANCE;
	}

	/**
	 * Create a new {@link Limit} from the given {@literal max} value.
	 *
	 * @param max the maximum value.
	 * @return new instance of {@link Limit}.
	 */
	static Limit of(int max) {
		return new Limited(max);
	}

	/**
	 * @return the max number of potential results.
	 */
	int max();

	/**
	 * @return {@literal true} if limiting (maximum value) should be applied.
	 */
	boolean isLimited();

	/**
	 * @return {@literal true} if no limiting (maximum value) should be applied.
	 */
	default boolean isUnlimited() {
		return !isLimited();
	}

	final class Limited implements Limit {

		private final int max;

		Limited(int max) {
			this.max = max;
		}

		@Override
		public int max() {
			return max;
		}

		@Override
		public boolean isLimited() {
			return true;
		}

		@Override
		public boolean equals(@Nullable Object obj) {

			if (obj == null) {
				return false;
			}

			if (!(obj instanceof Limit that)) {
				return false;
			}

			if (this.isUnlimited() ^ that.isUnlimited()) {
				return false;
			}

			return this.isUnlimited() && that.isUnlimited() || max() == that.max();
		}

		@Override
		public int hashCode() {
			return max;
		}
	}

	final class Unlimited implements Limit {

		static final Limit INSTANCE = new Unlimited();

		Unlimited() {}

		@Override
		public int max() {
			throw new IllegalStateException(
					"Unlimited does not define 'max'. Please check 'isLimited' before attempting to read 'max'");
		}

		@Override
		public boolean isLimited() {
			return false;
		}
	}
}
