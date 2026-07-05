/*
 * Copyright 2023-present the original author or authors.
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

import org.springframework.util.Assert;

/**
 * A {@code ReadWriteLock} maintains a pair of associated {@link Lock locks}, one for read-only operations and one for
 * writing. The {@link #readLock read lock} may be held simultaneously by multiple reader threads, so long as there are
 * no writers. The {@link #writeLock write lock} is exclusive.
 *
 * @author Mark Paluch
 * @since 3.2
 * @see Lock
 */
public interface ReadWriteLock {

	/**
	 * Create a new {@link ReadWriteLock} adapter for the given {@link java.util.concurrent.locks.ReadWriteLock delegate}.
	 *
	 * @param delegate must not be {@literal null}.
	 * @return a new {@link ReadWriteLock} adapter.
	 */
	static ReadWriteLock of(java.util.concurrent.locks.ReadWriteLock delegate) {

		Assert.notNull(delegate, "Lock delegate must not be null");

		return new DefaultReadWriteLock(Lock.of(delegate.readLock()), Lock.of(delegate.writeLock()));
	}

	/**
	 * Returns the lock used for reading.
	 *
	 * @return the lock used for reading
	 * @see java.util.concurrent.locks.ReadWriteLock#readLock()
	 */
	Lock readLock();

	/**
	 * Returns the lock used for reading.
	 *
	 * @return the lock used for writing.
	 * @see java.util.concurrent.locks.ReadWriteLock#writeLock() ()
	 */
	Lock writeLock();

}
