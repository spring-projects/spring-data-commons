/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.repository.query;

import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Handler for Repository query methods returning a Java 8 Stream result by ensuring
 * the {@link Stream} elements match the expected return type of the query method.
 *
 * @author John Blum
 * @since 1.13.0
 * @see org.springframework.data.repository.query.ResultProcessor
 */
@RequiredArgsConstructor
class StreamQueryResultHandler<T> {

	private @NonNull ReturnedType returnType;
	private @NonNull Converter converter;

	/**
	 * Processes the given source object as a {@link Stream}, mapping each element
	 * to the required return type, converting if necessary.
	 *
	 * @param source the {@link Stream} of elements to process.
	 * @return a new {@link Stream} of elements who's type matches the return type
	 * of the Repository query method.
	 * @throws NullPointerException if source object is null.
	 * @see org.springframework.core.convert.converter.Converter
	 * @see org.springframework.data.repository.query.ReturnedType
	 * @see java.util.stream.Stream
	 */
	@SuppressWarnings("unchecked")
	T handle(Object source) {

		return (T) ((Stream<Object>) source).map(new Function<Object, T>() {

			@Override
			public T apply(Object element) {
				return (T) (returnType.isInstance(element) ? element : converter.convert(element));
			}
		});
	}
}
