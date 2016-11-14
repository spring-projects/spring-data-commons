/*
 * Copyright 2016 the original author or authors.
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

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * Utility methods to work with {@link Optional}s.
 * 
 * @author Oliver Gierke
 */
@UtilityClass
public class Optionals {

	/**
	 * Returns whether any of the given {@link Optional}s is present.
	 * 
	 * @param optionals must not be {@literal null}.
	 * @return
	 */
	public static boolean isAnyPresent(Optional<?>... optionals) {

		Assert.notNull(optionals, "Optionals must not be null!");

		return Arrays.stream(optionals).anyMatch(Optional::isPresent);
	}

	/**
	 * Turns the given {@link Optional} into a one-element {@link Stream} or an empty one if not present.
	 * 
	 * @param optionals must not be {@literal null}.
	 * @return
	 */
	@SafeVarargs
	public static <T> Stream<T> toStream(Optional<? extends T>... optionals) {

		Assert.notNull(optionals, "Optional must not be null!");

		return Arrays.asList(optionals).stream().flatMap(it -> it.map(Stream::of).orElse(Stream.empty()));
	}

	public static <T> Optional<T> next(Iterator<T> iterator) {
		return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
	}
}
