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

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Default {@link Window} implementation.
 *
 * @author Mark Paluch
 * @since 3.1
 */
class WindowImpl<T> implements Window<T> {

	private final List<T> items;
	private final IntFunction<? extends ScrollPosition> positionFunction;

	private final boolean hasNext;

	WindowImpl(List<T> items, IntFunction<? extends ScrollPosition> positionFunction, boolean hasNext) {

		Assert.notNull(items, "List of items must not be null");
		Assert.notNull(positionFunction, "Position function must not be null");

		this.items = items;
		this.positionFunction = positionFunction;
		this.hasNext = hasNext;
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.isEmpty();
	}

	@Override
	public List<T> getContent() {
		return items;
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public ScrollPosition positionAt(int index) {

		if (index < 0 || index >= size()) {
			throw new IndexOutOfBoundsException(index);
		}

		return positionFunction.apply(index);
	}

	@Override
	public <U> Window<U> map(Function<? super T, ? extends U> converter) {

		Assert.notNull(converter, "Function must not be null");

		return new WindowImpl<>(stream().map(converter).collect(Collectors.toList()), positionFunction, hasNext);
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return items.iterator();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WindowImpl<?> that = (WindowImpl<?>) o;
		return ObjectUtils.nullSafeEquals(items, that.items)
				&& ObjectUtils.nullSafeEquals(positionFunction, that.positionFunction)
				&& ObjectUtils.nullSafeEquals(hasNext, that.hasNext);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(items);
		result = 31 * result + ObjectUtils.nullSafeHashCode(positionFunction);
		result = 31 * result + ObjectUtils.nullSafeHashCode(hasNext);
		return result;
	}

	@Override
	public String toString() {
		return "Window " + items;
	}
}
