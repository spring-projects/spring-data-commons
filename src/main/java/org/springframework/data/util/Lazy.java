/*
 * Copyright 2016-2023 the original author or authors.
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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Simple value type to delay the creation of an object using a {@link Supplier} returning the produced object for
 * subsequent lookups. Note, that no concurrency control is applied during the lookup of {@link #get()}, which means in
 * concurrent access scenarios, the provided {@link Supplier} can be called multiple times.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Henning Rohlfs
 * @author Johannes Englmeier
 * @author Greg Turnquist
 * @since 2.0
 */
public class Lazy<T> implements Supplier<T> {

	private static final Lazy<?> EMPTY = new Lazy<>(() -> null, null, true);
	static final String UNRESOLVED = "[Unresolved]";

	private final Supplier<? extends T> supplier;

	private @Nullable T value;
	private volatile boolean resolved;

	/**
	 * Creates a new {@link Lazy} instance for the given supplier.
	 *
	 * @param supplier
	 */
	private Lazy(Supplier<? extends T> supplier) {
		this(supplier, null, false);
	}

	/**
	 * Creates a new {@link Lazy} for the given {@link Supplier}, value and whether it has been resolved or not.
	 *
	 * @param supplier must not be {@literal null}.
	 * @param value can be {@literal null}.
	 * @param resolved whether the value handed into the constructor represents a resolved value.
	 */
	private Lazy(Supplier<? extends T> supplier, @Nullable T value, boolean resolved) {

		this.supplier = supplier;
		this.value = value;
		this.resolved = resolved;
	}

	/**
	 * Creates a new {@link Lazy} to produce an object lazily.
	 *
	 * @param <T> the type of which to produce an object of eventually.
	 * @param supplier the {@link Supplier} to create the object lazily.
	 * @return
	 */
	public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
		return new Lazy<>(supplier);
	}

	/**
	 * Creates a new {@link Lazy} to return the given value.
	 *
	 * @param <T> the type of the value to return eventually.
	 * @param value the value to return.
	 * @return
	 */
	public static <T> Lazy<T> of(T value) {

		Assert.notNull(value, "Value must not be null");

		return new Lazy<>(() -> value);
	}

	/**
	 * Creates a pre-resolved empty {@link Lazy}.
	 *
	 * @return
	 * @since 2.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> Lazy<T> empty() {
		return (Lazy<T>) EMPTY;
	}

	/**
	 * Returns the value created by the configured {@link Supplier}. Will return the calculated instance for subsequent
	 * lookups.
	 *
	 * @return
	 */
	public T get() {

		T value = getNullable();

		if (value == null) {
			throw new IllegalStateException("Expected lazy evaluation to yield a non-null value but got null");
		}

		return value;
	}

	/**
	 * Returns the {@link Optional} value created by the configured {@link Supplier}, allowing the absence of values in
	 * contrast to {@link #get()}. Will return the calculated instance for subsequent lookups.
	 *
	 * @return
	 */
	public Optional<T> getOptional() {
		return Optional.ofNullable(getNullable());
	}

	/**
	 * Returns a new Lazy that will consume the given supplier in case the current one does not yield in a result.
	 *
	 * @param supplier must not be {@literal null}.
	 * @return
	 */
	public Lazy<T> or(Supplier<? extends T> supplier) {

		Assert.notNull(supplier, "Supplier must not be null");

		return Lazy.of(() -> orElseGet(supplier));
	}

	/**
	 * Returns a new Lazy that will return the given value in case the current one does not yield in a result.
	 *
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public Lazy<T> or(T value) {

		Assert.notNull(value, "Value must not be null");

		return Lazy.of(() -> orElse(value));
	}

	/**
	 * Returns the value of the lazy computation or the given default value in case the computation yields
	 * {@literal null}.
	 *
	 * @param value
	 * @return
	 */
	@Nullable
	public T orElse(@Nullable T value) {

		T nullable = getNullable();

		return nullable == null ? value : nullable;
	}

	/**
	 * Returns the value of the lazy computation or the value produced by the given {@link Supplier} in case the original
	 * value is {@literal null}.
	 *
	 * @param supplier must not be {@literal null}.
	 * @return
	 */
	@Nullable
	public T orElseGet(Supplier<? extends T> supplier) {

		Assert.notNull(supplier, "Default value supplier must not be null");

		T value = getNullable();

		return value == null ? supplier.get() : value;
	}

	/**
	 * Creates a new {@link Lazy} with the given {@link Function} lazily applied to the current one.
	 *
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public <S> Lazy<S> map(Function<? super T, ? extends S> function) {

		Assert.notNull(function, "Function must not be null");

		return Lazy.of(() -> function.apply(get()));
	}

	/**
	 * Creates a new {@link Lazy} with the given {@link Function} lazily applied to the current one.
	 *
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public <S> Lazy<S> flatMap(Function<? super T, Lazy<? extends S>> function) {

		Assert.notNull(function, "Function must not be null");

		return Lazy.of(() -> function.apply(get()).get());
	}

	/**
	 * Returns the {@link String} representation of the already resolved value or the one provided through the given
	 * {@link Supplier} if the value has not been resolved yet.
	 *
	 * @param fallback must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @since 3.0.1
	 */
	public String toString(Supplier<String> fallback) {

		Assert.notNull(fallback, "Fallback must not be null!");

		return resolved ? toString() : fallback.get();
	}

	/**
	 * Returns the value of the lazy evaluation.
	 *
	 * @return
	 * @since 2.2
	 */
	@Nullable
	public T getNullable() {

		if (resolved) {
			return value;
		}

		this.value = supplier.get();
		this.resolved = true;

		return value;
	}

	@Override
	public String toString() {

		if (!resolved) {
			return UNRESOLVED;
		}

		return value == null ? "null" : value.toString();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Lazy<?> lazy)) {
			return false;
		}

		if (resolved != lazy.resolved) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(supplier, lazy.supplier)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(value, lazy.value);
	}

	@Override
	public int hashCode() {

		int result = ObjectUtils.nullSafeHashCode(supplier);

		result = 31 * result + ObjectUtils.nullSafeHashCode(value);
		result = 31 * result + (resolved ? 1 : 0);

		return result;
	}
}
