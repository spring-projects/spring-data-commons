/*
 * Copyright 2012-2018 the original author or authors.
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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Test;

/**
 * Unit tests for {@link IterableUtils}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.util.IterableUtils
 * @since 1.0.0
 */
public class IterableUtilsUnitTests {

	@Test
	public void isEmptyReturnsTrueWithEmptyIterable() {
		assertThat(IterableUtils.isEmpty(Collections.emptySet())).isTrue();
	}

	@Test
	public void isEmptyReturnsTrueWithNullIterable() {
		assertThat(IterableUtils.isEmpty(null)).isTrue();
	}

	@Test
	public void isEmptyReturnsFalseWithNonEmptyIterable() {
		assertThat(IterableUtils.isEmpty(Collections.singleton(1))).isFalse();
	}

	@Test
	public void isNotEmptyReturnsTrueWithNonEmptyIterable() {
		assertThat(IterableUtils.isNotEmpty(Collections.singletonList(1))).isTrue();
	}

	@Test
	public void isNotEmptyReturnsFalseWithEmptyIterable() {
		assertThat(IterableUtils.isNotEmpty(Collections.emptyList())).isFalse();
	}

	@Test
	public void isNotEmptyReturnsFalseWithNullIterable() {
		assertThat(IterableUtils.isNotEmpty(null)).isFalse();
	}

	@Test
	public void nullSafeIterableReturnsNonNullIterable() {

		Iterable<?> mockIterable = mock(Iterable.class);

		assertThat(IterableUtils.nullSafeIterable(mockIterable)).isSameAs(mockIterable);
	}

	@Test
	public void nullSafeIterableReturnsEmptyIterableForNull() {

		Iterable<?> iterable = IterableUtils.nullSafeIterable(null);

		assertThat(iterable).isNotNull();
		assertThat(iterable).isEmpty();
	}
}
