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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link Limit}.
 *
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
	@ValueSource(ints = { Integer.MIN_VALUE, -100, 0, 100, Integer.MAX_VALUE })
	void limitOfCapturesMaxValue(int value) {
		assertThat(Limit.of(value)).extracting(Limit::max).isEqualTo(value);
	}

	@ParameterizedTest // GH-2827
	@ValueSource(ints = { Integer.MIN_VALUE, -100, 0, 100, Integer.MAX_VALUE })
	void isUnlimitedReturnsFalseIfLimited(int value) {
		assertThat(Limit.of(value).isUnlimited()).isFalse();
	}

	@Test // GH-2827
	void unlimitedErrorsOnMax() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> Limit.unlimited().max());
	}

	@Test // GH-3023
	void equalsProperly() {

		Limit unlimited = Limit.unlimited();
		Limit limited = Limit.of(5);

		assertThat(limited.equals(unlimited)).isFalse();
		assertThat(unlimited.equals(limited)).isFalse();
		assertThat(unlimited.equals(unlimited)).isTrue();
	}
}
