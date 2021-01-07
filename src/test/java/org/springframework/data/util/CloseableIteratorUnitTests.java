/*
 * Copyright 2020-2021 the original author or authors.
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CloseableIterator}.
 *
 * @author Mark Paluch
 */
class CloseableIteratorUnitTests {

	@Test // DATACMNS-1637
	void shouldCreateStream() {

		CloseableIteratorImpl<String> iterator = new CloseableIteratorImpl<>(Arrays.asList("1", "2", "3").iterator());

		List<String> collection = iterator.stream().map(it -> "hello " + it).collect(Collectors.toList());

		assertThat(collection).contains("hello 1", "hello 2", "hello 3");
		assertThat(iterator.closed).isFalse();
	}

	@Test // DATACMNS-1637
	void closeStreamShouldCloseIterator() {

		CloseableIteratorImpl<String> iterator = new CloseableIteratorImpl<>(Arrays.asList("1", "2", "3").iterator());

		try (Stream<String> stream = iterator.stream()) {
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
