/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A Spliterator using a given Iterator for element operations. The spliterator implements {@code trySplit} to permit
 * limited parallelism.
 */
class IteratorSpliterator<T> implements Spliterator<T> {

	private static final int BATCH_UNIT = 1 << 10; // batch array size increment
	private static final int MAX_BATCH = 1 << 25; // max batch array size;
	private final Iterator<? extends T> it;
	private long est; // size estimate
	private int batch; // batch size for splits

	/**
	 * Creates a spliterator using the given iterator for traversal, and reporting the given initial size and
	 * characteristics.
	 *
	 * @param iterator the iterator for the source
	 */
	public IteratorSpliterator(Iterator<? extends T> iterator) {
		this.it = iterator;
		this.est = Long.MAX_VALUE;
	}

	@Override
	public Spliterator<T> trySplit() {
		/*
		 * Split into arrays of arithmetically increasing batch
		 * sizes.  This will only improve parallel performance if
		 * per-element Consumer actions are more costly than
		 * transferring them into an array.  The use of an
		 * arithmetic progression in split sizes provides overhead
		 * vs parallelism bounds that do not particularly favor or
		 * penalize cases of lightweight vs heavyweight element
		 * operations, across combinations of #elements vs #cores,
		 * whether or not either are known.  We generate
		 * O(sqrt(#elements)) splits, allowing O(sqrt(#cores))
		 * potential speedup.
		 */
		Iterator<? extends T> i = it;
		long s = est;
		if (s > 1 && i.hasNext()) {
			int n = batch + BATCH_UNIT;
			if (n > s) {
				n = (int) s;
			}
			if (n > MAX_BATCH) {
				n = MAX_BATCH;
			}
			Object[] a = new Object[n];
			int j = 0;
			do {
				a[j] = i.next();
			} while (++j < n && i.hasNext());
			batch = j;
			if (est != Long.MAX_VALUE) {
				est -= j;
			}
			return Spliterators.spliterator(a, 0, j, 0);
		}
		return null;
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		it.forEachRemaining(action);
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (it.hasNext()) {
			action.accept(it.next());
			return true;
		}
		return false;
	}

	@Override
	public long estimateSize() {
		return -1;
	}

	@Override
	public int characteristics() {
		return 0;
	}

	@Override
	public Comparator<? super T> getComparator() {
		if (hasCharacteristics(Spliterator.SORTED)) {
			return null;
		}
		throw new IllegalStateException();
	}
}
