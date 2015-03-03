/*
 * Copyright 2015 the original author or authors.
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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

/**
 * Spring Data specific Java {@link Stream} utility methods and classes.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.10
 */
public class StreamUtils {

	private StreamUtils() {}

	/**
	 * Returns a {@link Stream} backed by the given {@link Iterator}.
	 * <p>
	 * If the given iterator is an {@link CloseableIterator} add a {@link CloseableIteratorDisposingRunnable} wrapping the
	 * given iterator to propagate {@link Stream#close()} accordingly.
	 * 
	 * @param iterator must not be {@literal null}
	 * @return
	 * @since 1.8
	 */
	public static <T> Stream<T> createStreamFromIterator(Iterator<T> iterator) {

		Assert.notNull(iterator, "Iterator must not be null!");

		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
		Stream<T> stream = StreamSupport.stream(spliterator, false);

		return iterator instanceof CloseableIterator ? stream.onClose(new CloseableIteratorDisposingRunnable(
				(CloseableIterator<T>) iterator)) : stream;
	}

	/**
	 * A {@link Runnable} that closes the given {@link CloseableIterator} in its {@link #run()} method. If the given
	 * {@code closeable} is {@literal null} the {@link #run()} method effectively becomes a noop.
	 * <p>
	 * Can be used in conjunction with streams as close action via:
	 * 
	 * <pre>
	 * CloseableIterator<T> result = ...;
	 * Spliterator<T> spliterator = ...;
	 * 
	 * return StreamSupport.stream(spliterator, false).onClose(new CloseableIteratorDisposingRunnable(result));
	 * </pre>
	 * 
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @since 1.10
	 */
	private static class CloseableIteratorDisposingRunnable implements Runnable {

		private CloseableIterator<?> closeable;

		/**
		 * Creates a new {@link CloseableIteratorDisposingRunnable} for the given {@link CloseableIterator}.
		 * 
		 * @param closeable can be {@literal null}.
		 */
		public CloseableIteratorDisposingRunnable(CloseableIterator<?> closeable) {
			this.closeable = closeable;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {

			if (closeable != null) {
				closeable.close();
			}
		}
	}
}
