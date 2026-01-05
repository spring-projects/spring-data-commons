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

import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * {@code Lock} provides more extensive locking operations than can be obtained using {@code synchronized} methods and
 * {@link java.util.concurrent.locks.Lock}. It allows more flexible structuring and an improved usage model.
 * <p>
 * This Lock abstraction is an extension to the {@link java.util.concurrent.locks.Lock lock utilities} and intended for
 * easier functional and try-with-resources usage.
 *
 * <pre class="code">
 * ReentrantLock backend = new ReentrantLock();
 *
 * Lock lock = Lock.of(backend);
 *
 * lock.executeWithoutResult(() -> {
 *   // callback without returning a result
 * });
 *
 * lock.execute(() -> {
 *   // callback returning a result
 *   return …;
 * });
 * </pre>
 *
 * @author Mark Paluch
 * @since 3.2
 */
public interface Lock {

	/**
	 * Create a new {@link Lock} adapter for the given {@link java.util.concurrent.locks.Lock delegate}.
	 *
	 * @param delegate must not be {@literal null}.
	 * @return a new {@link Lock} adapter.
	 */
	static Lock of(java.util.concurrent.locks.Lock delegate) {

		Assert.notNull(delegate, "Lock delegate must not be null");

		return new DefaultLock(delegate);
	}

	/**
	 * Acquires the lock.
	 * <p>
	 * If the lock is not available then the current thread becomes disabled for thread scheduling purposes and lies
	 * dormant until the lock has been acquired.
	 *
	 * @see java.util.concurrent.locks.Lock#lock()
	 */
	AcquiredLock lock();

	/**
	 * Acquires the lock unless the current thread is {@linkplain Thread#interrupt interrupted}.
	 * <p>
	 * Acquires the lock if it is available and returns immediately.
	 * <p>
	 * If the lock is not available then the current thread becomes disabled for thread scheduling purposes and lies
	 * dormant until one of two things happens:
	 * <ul>
	 * <li>The lock is acquired by the current thread; or
	 * <li>Some other thread {@linkplain Thread#interrupt interrupts} the current thread, and interruption of lock
	 * acquisition is supported.
	 * </ul>
	 * <p>
	 * If the current thread:
	 * <ul>
	 * <li>has its interrupted status set on entry to this method; or
	 * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the lock, and interruption of lock acquisition is
	 * supported,
	 * </ul>
	 * then {@link InterruptedException} is thrown and the current thread's interrupted status is cleared.
	 */
	AcquiredLock lockInterruptibly() throws InterruptedException;

	/**
	 * Execute the action specified by the given callback object guarded by a lock and return its result. The
	 * {@code action} is only executed once the lock has been acquired.
	 *
	 * @param action the action to run.
	 * @return the result of the action.
	 * @param <T> type of the result.
	 * @throws RuntimeException if thrown by the action
	 */
	default <T> T execute(Supplier<T> action) {
		try (AcquiredLock l = lock()) {
			return action.get();
		}
	}

	/**
	 * Execute the action specified by the given callback object guarded by a lock. The {@code action} is only executed
	 * once the lock has been acquired.
	 *
	 * @param action the action to run.
	 * @throws RuntimeException if thrown by the action.
	 */
	default void executeWithoutResult(Runnable action) {
		try (AcquiredLock l = lock()) {
			action.run();
		}
	}

	/**
	 * An acquired lock can be used with try-with-resources for easier releasing.
	 */
	interface AcquiredLock extends AutoCloseable {

		/**
		 * Releases the lock.
		 *
		 * @see java.util.concurrent.locks.Lock#unlock()
		 */
		@Override
		void close();
	}

}
