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
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.WindowIterator.WindowIteratorBuilder;

/**
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WindowIteratorUnitTests<T> {

	@Mock Function<ScrollPosition, Window<T>> fkt;

	@Mock Window<T> window;

	@Mock ScrollPosition scrollPosition;

	@Captor ArgumentCaptor<ScrollPosition> scrollCaptor;

	@BeforeEach
	void beforeEach() {
		when(fkt.apply(any())).thenReturn(window);
	}

	@Test // GH-2151
	void loadsDataOnCreation() {

		WindowIteratorBuilder<T> of = WindowIterator.of(fkt);
		verifyNoInteractions(fkt);

		of.startingAt(scrollPosition);
		verify(fkt).apply(eq(scrollPosition));
	}

	@Test // GH-2151
	void hasNextReturnsFalseIfNoDataAvailable() {

		when(window.isLast()).thenReturn(true);
		when(window.isEmpty()).thenReturn(true);

		assertThat(WindowIterator.of(fkt).startingAt(scrollPosition).hasNext()).isFalse();
	}

	@Test // GH-2151
	void hasNextReturnsTrueIfDataAvailableButOnlyOnePage() {

		when(window.isLast()).thenReturn(true);
		when(window.isEmpty()).thenReturn(false);

		assertThat(WindowIterator.of(fkt).startingAt(scrollPosition).hasNext()).isTrue();
	}

	@Test // GH-2151
	void allowsToIterateAllWindows() {

		ScrollPosition p1 = mock(ScrollPosition.class);
		ScrollPosition p2 = mock(ScrollPosition.class);

		when(window.isEmpty()).thenReturn(false, false, false);
		when(window.isLast()).thenReturn(false, false, true);
		when(window.hasNext()).thenReturn(true, true, false);
		when(window.size()).thenReturn(1, 1, 1);
		when(window.positionAt(anyInt())).thenReturn(p1, p2);
		when(window.getContent()).thenReturn(List.of((T) "0"), List.of((T) "1"), List.of((T) "2"));

		WindowIterator<T> iterator = WindowIterator.of(fkt).startingAt(scrollPosition);
		List<T> capturedResult = new ArrayList<>(3);
		while (iterator.hasNext()) {
			capturedResult.addAll(iterator.next());
		}

		verify(fkt, times(3)).apply(scrollCaptor.capture());
		assertThat(scrollCaptor.getAllValues()).containsExactly(scrollPosition, p1, p2);
		assertThat(capturedResult).containsExactly((T) "0", (T) "1", (T) "2");
	}

	@Test // GH-2151
	void stopsAfterFirstPageIfOnlyOneWindowAvailable() {

		ScrollPosition p1 = mock(ScrollPosition.class);

		when(window.isEmpty()).thenReturn(false);
		when(window.isLast()).thenReturn(true);
		when(window.hasNext()).thenReturn(false);
		when(window.size()).thenReturn(1);
		when(window.positionAt(anyInt())).thenReturn(p1);
		when(window.getContent()).thenReturn(List.of((T) "0"));

		WindowIterator<T> iterator = WindowIterator.of(fkt).startingAt(scrollPosition);
		List<T> capturedResult = new ArrayList<>(1);
		while (iterator.hasNext()) {
			capturedResult.addAll(iterator.next());
		}

		verify(fkt).apply(scrollCaptor.capture());
		assertThat(scrollCaptor.getAllValues()).containsExactly(scrollPosition);
		assertThat(capturedResult).containsExactly((T) "0");
	}
}
