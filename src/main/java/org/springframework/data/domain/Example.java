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
package org.springframework.data.domain;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.springframework.util.ClassUtils;

/**
 * Support for query by example (QBE). An {@link Example} takes a {@code probe} to define the example. Matching options
 * and type safety can be tuned using {@link ExampleMatcher}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Gierke
 * @param <T> the type of the probe.
 * @since 1.12
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Example<T> {

	private final @NonNull T probe;
	private final @NonNull ExampleMatcher matcher;

	/**
	 * Create a new {@link Example} including all non-null properties by default.
	 *
	 * @param probe must not be {@literal null}.
	 * @return
	 */
	public static <T> Example<T> of(T probe) {
		return new Example<>(probe, ExampleMatcher.matching());
	}

	/**
	 * Create a new {@link Example} using the given {@link ExampleMatcher}.
	 *
	 * @param probe must not be {@literal null}.
	 * @param matcher must not be {@literal null}.
	 * @return
	 */
	public static <T> Example<T> of(T probe, ExampleMatcher matcher) {
		return new Example<>(probe, matcher);
	}

	/**
	 * Get the example used.
	 *
	 * @return never {@literal null}.
	 */
	public T getProbe() {
		return probe;
	}

	/**
	 * Get the {@link ExampleMatcher} used.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleMatcher getMatcher() {
		return matcher;
	}

	/**
	 * Get the actual type for the probe used. This is usually the given class, but the original class in case of a
	 * CGLIB-generated subclass.
	 *
	 * @return
	 * @see ClassUtils#getUserClass(Class)
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getProbeType() {
		return (Class<T>) ClassUtils.getUserClass(probe.getClass());
	}
}
