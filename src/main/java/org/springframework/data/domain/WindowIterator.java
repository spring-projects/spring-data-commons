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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link Iterator} over multiple {@link Window Windows} obtained via a {@link Function window function}, that keeps track of
 * the current {@link ScrollPosition} returning the Window {@link Window#getContent() content} on {@link #next()}.
 * <pre class="code">
 * WindowIterator&lt;User&gt; users = WindowIterator.of(position -> repository.findFirst10By...("spring", position))
 *   .startingAt(OffsetScrollPosition.initial());
 * while (users.hasNext()) {
 *   users.next().forEach(user -> {
 *     // consume the user
 *   });
 * }
 * </pre>
 *
 * @author Christoph Strobl
 * @since 3.1
 */
public class WindowIterator<T> implements Iterator<List<T>> {

	private final Function<ScrollPosition, Window<T>> windowFunction;
	private ScrollPosition currentPosition;

	@Nullable //
	private Window<T> currentWindow;

	/**
	 * Entrypoint to create a new {@link WindowIterator} for the given windowFunction.
	 *
	 * @param windowFunction must not be {@literal null}.
	 * @param <T>
	 * @return new instance of {@link WindowIteratorBuilder}.
	 */
	public static <T> WindowIteratorBuilder<T> of(Function<ScrollPosition, Window<T>> windowFunction) {
		return new WindowIteratorBuilder(windowFunction);
	}

	WindowIterator(Function<ScrollPosition, Window<T>> windowFunction, ScrollPosition position) {

		this.windowFunction = windowFunction;
		this.currentPosition = position;
		this.currentWindow = doScroll();
	}

	@Override
	public boolean hasNext() {
		return currentWindow != null;
	}

	@Override
	public List<T> next() {

		List<T> toReturn = new ArrayList<>(currentWindow.getContent());
		currentPosition = currentWindow.positionAt(currentWindow.size() -1);
		currentWindow = doScroll();
		return toReturn;
	}

	@Nullable
	Window<T> doScroll() {

		if (currentWindow != null && !currentWindow.hasNext()) {
			return null;
		}

		Window<T> window = windowFunction.apply(currentPosition);
		if (window.isEmpty() && window.isLast()) {
			return null;
		}
		return window;
	}

	/**
	 * Builder API to construct a {@link WindowIterator}.
	 *
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 3.1
	 */
	public static class WindowIteratorBuilder<T> {

		private Function<ScrollPosition, Window<T>> windowFunction;

		WindowIteratorBuilder(Function<ScrollPosition, Window<T>> windowFunction) {
			this.windowFunction = windowFunction;
		}

		public WindowIterator<T> startingAt(ScrollPosition position) {

			Assert.state(windowFunction != null, "WindowFunction cannot not be null");
			return new WindowIterator<>(windowFunction, position);
		}
	}
}
