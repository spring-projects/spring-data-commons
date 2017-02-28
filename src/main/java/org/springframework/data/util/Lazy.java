/*
 * Copyright 2016-2017 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * Simple value type to delay the creation of an object using a {@link Supplier} returning the produced object for
 * subsequent lookups. Note, that no concurrency control is applied during the lookup of {@link #get()}, which means in
 * concurrent access scenarios, the provided {@link Supplier} can be called multiple times.
 * 
 * @author Oliver Gierke
 * @since 2.0
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class Lazy<T> implements Supplier<T> {

	private final Supplier<T> supplier;
	private Optional<T> value;

	/**
	 * Creates a new {@link Lazy} to produce an object lazily.
	 * 
	 * @param <T> the type of which to produce an object of eventually.
	 * @param supplier the {@link Supplier} to create the object lazily.
	 * @return
	 */
	public static <T> Lazy<T> of(Supplier<T> supplier) {
		return new Lazy<>(supplier);
	}

	/**
	 * Returns the value created by the configured {@link Supplier}. Will return the calculated instance for subsequent
	 * lookups.
	 * 
	 * @return
	 */
	public T get() {

		if (value == null) {
			this.value = Optional.ofNullable(supplier.get());
		}

		return value.orElse(null);
	}

	/**
	 * Creates a new {@link Lazy} with the given {@link Function} lazily applied to the current one.
	 * 
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public <S> Lazy<S> map(Function<T, S> function) {

		Assert.notNull(function, "Function must not be null!");

		return Lazy.of(() -> function.apply(get()));
	}

	/**
	 * Creates a new {@link Lazy} with the given {@link Function} lazily applied to the current one.
	 * 
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public <S> Lazy<S> flatMap(Function<T, Lazy<S>> function) {

		Assert.notNull(function, "Function must not be null!");

		return Lazy.of(() -> function.apply(get()).get());
	}
}
