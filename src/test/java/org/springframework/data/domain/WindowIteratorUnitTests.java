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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link WindowIterator}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WindowIteratorUnitTests {

	@Test // GH-2151
	void loadsDataOnNext() {

		Function<ScrollPosition, Window<String>> fkt = mock(Function.class);
		WindowIterator<String> iterator = WindowIterator.of(fkt).startingAt(OffsetScrollPosition.initial());
		verifyNoInteractions(fkt);

		when(fkt.apply(any())).thenReturn(Window.from(Collections.emptyList(), value -> OffsetScrollPosition.initial()));

		iterator.hasNext();
		verify(fkt).apply(OffsetScrollPosition.initial());
	}

	@Test // GH-2151
	void hasNextReturnsFalseIfNoDataAvailable() {

		Window<Object> window = Window.from(Collections.emptyList(), value -> OffsetScrollPosition.initial());
		WindowIterator<Object> iterator = WindowIterator.of(it -> window).startingAt(OffsetScrollPosition.initial());

		assertThat(iterator.hasNext()).isFalse();
	}

	@Test // GH-2151
	void nextThrowsExceptionIfNoElementAvailable() {

		Window<Object> window = Window.from(Collections.emptyList(), value -> OffsetScrollPosition.initial());
		WindowIterator<Object> iterator = WindowIterator.of(it -> window).startingAt(OffsetScrollPosition.initial());

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
	}

	@Test // GH-2151
	void hasNextReturnsTrueIfDataAvailableButOnlyOnePage() {

		Window<String> window = Window.from(List.of("a", "b"), value -> OffsetScrollPosition.initial());
		WindowIterator<String> iterator = WindowIterator.of(it -> window).startingAt(OffsetScrollPosition.initial());

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("a");
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("b");
		assertThat(iterator.hasNext()).isFalse();
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test // GH-2151
	void hasNextReturnsCorrectlyIfNextPageIsEmpty() {

		Window<String> window = Window.from(List.of("a", "b"), value -> OffsetScrollPosition.initial());
		WindowIterator<String> iterator = WindowIterator.of(it -> {
			if (it.isInitial()) {
				return window;
			}

			return Window.from(Collections.emptyList(), OffsetScrollPosition::of, false);
		}).startingAt(OffsetScrollPosition.initial());

		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("a");
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("b");
		assertThat(iterator.hasNext()).isFalse();
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test // GH-2151
	void allowsToIterateAllWindows() {

		Window<String> window1 = Window.from(List.of("a", "b"), OffsetScrollPosition::of, true);
		Window<String> window2 = Window.from(List.of("c", "d"), value -> OffsetScrollPosition.of(2 + value));
		WindowIterator<String> iterator = WindowIterator.of(it -> {
			if (it.isInitial()) {
				return window1;
			}

			return window2;
		}).startingAt(OffsetScrollPosition.initial());

		List<String> capturedResult = new ArrayList<>(4);
		while (iterator.hasNext()) {
			capturedResult.add(iterator.next());
		}

		assertThat(capturedResult).containsExactly("a", "b", "c", "d");
	}
}
