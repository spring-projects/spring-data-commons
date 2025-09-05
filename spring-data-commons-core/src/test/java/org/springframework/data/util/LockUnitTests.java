/*
 * Copyright 2023-2025 the original author or authors.
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

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.Test;
import org.springframework.data.util.Lock.AcquiredLock;

/**
 * Unit tests for {@link Lock}.
 *
 * @author Mark Paluch
 */
class LockUnitTests {

	@Test // GH-2944
	void shouldDelegateLock() {

		ReentrantLock backend = new ReentrantLock();

		Lock lock = Lock.of(backend);

		lock.executeWithoutResult(() -> {

			assertThat(backend.isLocked()).isTrue();
			assertThat(backend.isHeldByCurrentThread()).isTrue();
		});

		lock.execute(() -> {

			assertThat(backend.isLocked()).isTrue();
			assertThat(backend.isHeldByCurrentThread()).isTrue();
			return null;
		});

		assertThat(backend.isLocked()).isFalse();
	}

	@Test // GH-2944
	void shouldIncrementLockCount() {

		ReentrantLock backend = new ReentrantLock();
		backend.lock();

		Lock lock = Lock.of(backend);

		lock.executeWithoutResult(() -> {

			assertThat(backend.getHoldCount()).isEqualTo(2);
			assertThat(backend.isLocked()).isTrue();
			assertThat(backend.isHeldByCurrentThread()).isTrue();
		});

		assertThat(backend.getHoldCount()).isEqualTo(1);
		assertThat(backend.isLocked()).isTrue();
	}

	@Test // GH-2944
	void exceptionShouldCleanupLock() {

		ReentrantLock backend = new ReentrantLock();

		Lock lock = Lock.of(backend);

		assertThatIllegalStateException().isThrownBy(() -> lock.executeWithoutResult(() -> {
			throw new IllegalStateException();
		}));

		assertThat(backend.isLocked()).isFalse();

		assertThatIllegalStateException().isThrownBy(() -> lock.execute(() -> {
			throw new IllegalStateException();
		}));

		assertThat(backend.isLocked()).isFalse();
	}

	@Test // GH-2944
	void shouldDelegateReadWriteLock() {

		ReentrantReadWriteLock backend = new ReentrantReadWriteLock();

		ReadWriteLock lock = ReadWriteLock.of(backend);

		lock.readLock().executeWithoutResult(() -> {
			assertThat(backend.getReadLockCount()).isEqualTo(1);
		});

		lock.writeLock().executeWithoutResult(() -> {
			assertThat(backend.isWriteLocked()).isTrue();
		});

		assertThat(backend.getReadLockCount()).isEqualTo(0);
		assertThat(backend.isWriteLocked()).isFalse();
	}

	@Test // GH-2944
	void lockTryWithResources() {

		ReentrantLock backend = new ReentrantLock();
		Lock lock = Lock.of(backend);

		try (AcquiredLock l = lock.lock()) {
			assertThat(backend.isLocked()).isTrue();
			assertThat(backend.isHeldByCurrentThread()).isTrue();
		}

		assertThat(backend.isLocked()).isFalse();
	}

	@Test // GH-2944
	void lockInterruptiblyTryWithResources() {

		ReentrantLock backend = new ReentrantLock();
		Lock lock = Lock.of(backend);

		try (AcquiredLock l = lock.lockInterruptibly()) {
			assertThat(backend.isLocked()).isTrue();
			assertThat(backend.isHeldByCurrentThread()).isTrue();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		assertThat(backend.isLocked()).isFalse();
	}

	@Test // GH-2944
	void shouldReturnResult() {

		ReentrantLock backend = new ReentrantLock();

		Lock lock = Lock.of(backend);

		String result = lock.execute(() -> "foo");

		assertThat(result).isEqualTo("foo");
	}
}
