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
package org.springframework.data.support;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link Iterator} over multiple {@link Window Windows} obtained via a {@link Function window function}, that keeps
 * track of the current {@link ScrollPosition} allowing scrolling across all result elements.
 *
 * <pre class="code">
 * WindowIterator&lt;User&gt; users = WindowIterator.of(position -> repository.findFirst10By…("spring", position))
 *   .startingAt(ScrollPosition.offset());
 *
 * while (users.hasNext()) {
 *   User u = users.next();
 *   // consume user
 * }
 * </pre>
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.1
 */
public class WindowIterator<T> implements Iterator<T> {

	private final Function<ScrollPosition, Window<T>> windowFunction;

	private ScrollPosition currentPosition;

	private @Nullable Window<T> currentWindow;

	private @Nullable Iterator<T> currentIterator;

	/**
	 * Entrypoint to create a new {@link WindowIterator} for the given windowFunction.
	 *
	 * @param windowFunction must not be {@literal null}.
	 * @param <T>
	 * @return new instance of {@link WindowIteratorBuilder}.
	 */
	public static <T> WindowIteratorBuilder<T> of(Function<ScrollPosition, Window<T>> windowFunction) {
		return new WindowIteratorBuilder<>(windowFunction);
	}

	WindowIterator(Function<ScrollPosition, Window<T>> windowFunction, ScrollPosition position) {

		this.windowFunction = windowFunction;
		this.currentPosition = position;
	}

	@Override
	public boolean hasNext() {

		// use while loop instead of recursion to fetch the next window.
		do {
			if (currentWindow == null) {
				currentWindow = windowFunction.apply(currentPosition);
			}

			if (currentIterator == null) {
				if (currentWindow != null) {
					currentIterator = isBackwardsScrolling(currentPosition)
							? new ReverseListIterator<>(currentWindow.getContent())
							: currentWindow.iterator();
				}
			}

			if (currentIterator != null) {

				if (currentIterator.hasNext()) {
					return true;
				}

				if (currentWindow != null && currentWindow.hasNext()) {

					currentPosition = getNextPosition(currentPosition, currentWindow);
					currentIterator = null;
					currentWindow = null;
					continue;
				}
			}

			return false;
		} while (true);
	}

	@Override
	public T next() {

		if (!hasNext()) {
			throw new NoSuchElementException();
		}

		return currentIterator.next();
	}

	private static ScrollPosition getNextPosition(ScrollPosition currentPosition, Window<?> window) {

		if (isBackwardsScrolling(currentPosition)) {
			return window.positionAt(0);
		}

		return window.positionAt(window.size() - 1);
	}

	private static boolean isBackwardsScrolling(ScrollPosition position) {
		return position instanceof KeysetScrollPosition ksp ? ksp.scrollsBackward() : false;
	}

	/**
	 * Builder API to construct a {@link WindowIterator}.
	 *
	 * @param <T>
	 * @author Christoph Strobl
	 * @since 3.1
	 */
	public static class WindowIteratorBuilder<T> {

		private final Function<ScrollPosition, Window<T>> windowFunction;

		WindowIteratorBuilder(Function<ScrollPosition, Window<T>> windowFunction) {

			Assert.notNull(windowFunction, "WindowFunction must not be null");

			this.windowFunction = windowFunction;
		}

		/**
		 * Create a {@link WindowIterator} given {@link ScrollPosition}.
		 *
		 * @param position
		 * @return
		 */
		public WindowIterator<T> startingAt(ScrollPosition position) {

			Assert.notNull(position, "ScrollPosition must not be null");

			return new WindowIterator<>(windowFunction, position);
		}
	}

	private static class ReverseListIterator<T> implements Iterator<T> {

		private final ListIterator<T> delegate;

		public ReverseListIterator(List<T> list) {
			this.delegate = list.listIterator(list.size());
		}

		@Override
		public boolean hasNext() {
			return delegate.hasPrevious();
		}

		@Override
		public T next() {
			return delegate.previous();
		}

		@Override
		public void remove() {
			delegate.remove();
		}
	}
}
