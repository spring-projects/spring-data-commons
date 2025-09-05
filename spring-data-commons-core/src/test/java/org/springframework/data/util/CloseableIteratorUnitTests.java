/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CloseableIterator}.
 *
 * @author Mark Paluch
 */
class CloseableIteratorUnitTests {

	@Test // DATACMNS-1637
	void shouldCreateStream() {

		var iterator = new CloseableIteratorImpl<>(Arrays.asList("1", "2", "3").iterator());

		var collection = iterator.stream().map(it -> "hello " + it).collect(Collectors.toList());

		assertThat(collection).contains("hello 1", "hello 2", "hello 3");
		assertThat(iterator.closed).isFalse();
	}

	@Test // GH-2519
	void shouldCount() {

		var iterator = new CloseableIteratorImpl<>(Arrays.asList("1", "2", "3").iterator());

		var count = iterator.stream().count();

		assertThat(count).isEqualTo(3);
	}

	@Test // GH-2519
	void shouldCountLargeStream() {

		var iterator = new CloseableIteratorImpl<>(IntStream.range(0, 2048).boxed().iterator());

		var count = iterator.stream().count();

		assertThat(count).isEqualTo(2048);
	}

	@Test // GH-2519
	void shouldApplyToList() {

		var iterator = new CloseableIteratorImpl<>(Arrays.asList("1", "2", "3").iterator());

		var list = iterator.stream().toList();

		assertThat(list).isEqualTo(Arrays.asList("1", "2", "3"));
	}

	@Test // DATACMNS-1637
	void closeStreamShouldCloseIterator() {

		var iterator = new CloseableIteratorImpl<>(Arrays.asList("1", "2", "3").iterator());

		try (var stream = iterator.stream()) {
			assertThat(stream.findFirst()).hasValue("1");
		}

		assertThat(iterator.closed).isTrue();
	}

	static class CloseableIteratorImpl<T> implements CloseableIterator<T> {

		private final Iterator<T> delegate;
		private boolean closed = false;

		CloseableIteratorImpl(Iterator<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public void close() {
			closed = true;
		}

		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public T next() {
			return delegate.next();
		}
	}
}
