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
 */
public class CloseableIteratorDisposingRunnable implements Runnable {

	private CloseableIterator<?> closeable;

	/**
	 * Creates a new {@link CloseableIteratorDisposingRunnable}.
	 * 
	 * @param closeable may be {@literal null}
	 */
	public CloseableIteratorDisposingRunnable(CloseableIterator<?> closeable) {
		this.closeable = closeable;
	}

	@Override
	public void run() {

		CloseableIterator<?> ci = closeable;
		if (ci == null) {
			return;
		}

		closeable = null;
		ci.close();
	}
}
