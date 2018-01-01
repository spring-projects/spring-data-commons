/*
 * Copyright 2017-2018 the original author or authors.
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

import lombok.Value;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Lazy implementation of {@link Streamable} obtains a {@link Stream} from a given {@link Supplier}.
 *
 * @author Oliver Gierke
 * @since 2.0
 */
@Value(staticConstructor = "of")
class LazyStreamable<T> implements Streamable<T> {

	private final Supplier<? extends Stream<T>> stream;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return stream().iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.Streamable#stream()
	 */
	@Override
	public Stream<T> stream() {
		return stream.get();
	}
}
