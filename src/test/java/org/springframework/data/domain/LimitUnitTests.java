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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Christoph Strobl
 * @soundtrack Rise Against - Tragedy + Time
 */
class LimitUnitTests {

	@Test // GH-2827
	void unlimitedReusesInstance() {
		assertThat(Limit.unlimited()).isSameAs(Limit.unlimited());
	}

	@Test // GH-2827
	void unlimitedReturnsTrueOnIsLimited() {
		assertThat(Limit.unlimited().isUnlimited()).isTrue();
	}

	@ParameterizedTest // GH-2827
	@ValueSource(longs = { Long.MIN_VALUE, -100, 0, 100, Long.MAX_VALUE })
	void limitOfCapturesMaxValue(long value) {
		assertThat(Limit.of(value)).extracting(Limit::max).isEqualTo(value);
	}

	@ParameterizedTest // GH-2827
	@ValueSource(longs = { Long.MIN_VALUE, -100, 0, 100, Long.MAX_VALUE })
	void isUnlimitedReturnsFalseIfLimited(long value) {
		assertThat(Limit.of(value).isUnlimited()).isFalse();
	}

	@Test // GH-2827
	void unlimitedErrorsOnMax() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Limit.unlimited().max());
	}

	@Test // GH-2827
	void unlimitedIsNotEqualToCustomUnlimited/*even if both return true for isUnlimited()*/() {
		assertThat(Limit.unlimited()).isNotEqualTo(new MyLimit(-1));
	}

	static class MyLimit implements Limit {

		private final long max;

		public MyLimit(long max) {
			this.max = max;
		}

		@Override
		public long max() {
			return max;
		}

		@Override
		public boolean isUnlimited() {
			if (Limit.super.isUnlimited()) {
				return true;
			}

			return max() < 0;
		}
	}
}
