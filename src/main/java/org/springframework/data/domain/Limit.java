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

import java.util.Optional;

import org.springframework.util.ClassUtils;

/**
 * {@link Limit} represents the maximum value up to which an operation should continue processing. It may be used for
 * defining the {@link #max() maximum} number of results within a repository finder method or if applicable a template
 * operation.
 * <p>
 * A {@link Limit#isUnlimited()} is used to indicate that there is no {@link Limit} defined, which should be favoured
 * over using {@literal null} or {@link Optional#empty()} to indicate the absence of an actual {@link Limit}.
 * </p>
 * {@link Limit} itself does not make assumptions about the actual {@link #max()} value sign. This means that a negative
 * value may be valid within a defined context. Implementations should override {@link #isUnlimited()} to fit their
 * needs and enforce restrictions if needed.
 * 
 * @author Christoph Strobl
 * @since 3.2
 */
public interface Limit {

	/**
	 * A {@link Limit} that defaults {@link #isUnlimited()} to {@literal true} and errors on {@link #max()}.
	 */
	Limit UNLIMITED = new Limit() {

		@Override
		public long max() {
			throw new IllegalStateException(
					"Unlimited does not define 'max'. Please check 'isUnlimited' before attempting to read 'max'");
		}

		@Override
		public boolean isUnlimited() {
			return true;
		}
	};

	/**
	 * @return the max number of potential results.
	 */
	long max();

	/**
	 * @return {@literal true} if no limiting (maximum value) should be applied.
	 */
	default boolean isUnlimited() {
		return UNLIMITED.equals(this);
	}

	/**
	 * @return a {@link Limit} instance that does not define {@link #max()} and answers {@link #isUnlimited()} with
	 *         {@literal true}.
	 */
	static Limit unlimited() {
		return UNLIMITED;
	}

	/**
	 * Create a new {@link Limit} from the given {@literal max} value.
	 *
	 * @param max the maximum value.
	 * @return new instance of {@link Limit}.
	 */
	static Limit of(long max) {
		return new Limit() {
			@Override
			public long max() {
				return max;
			}

			@Override
			public boolean equals(Object obj) {

				if (obj == null) {
					return false;
				}
				if (!ClassUtils.isAssignable(Limit.class, obj.getClass())) {
					return false;
				}
				Limit that = (Limit) obj;
				if (this.isUnlimited() && that.isUnlimited()) {
					return true;
				}
				return max() == that.max();
			}
		};
	}
}
