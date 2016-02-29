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

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Support for query by example (QBE).
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @param <T>
 * @since 1.12
 */
public class Example<T> {

	private final T probe;
	private final ExampleSpec exampleSpec;

	/**
	 * Create a new {@link Example} including all non-null properties by default.
	 *
	 * @param probe The probe to use. Must not be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private Example(T probe) {

		Assert.notNull(probe, "Probe must not be null!");

		this.probe = probe;
		this.exampleSpec = ExampleSpec.untyped();
	}

	/**
	 * Create a new {@link Example} including all non-null properties by default.
	 *
	 * @param probe The probe to use. Must not be {@literal null}.
	 * @param exampleSpec The example specification to use. Must not be {@literal null}.
	 */
	private Example(T probe, ExampleSpec exampleSpec) {

		Assert.notNull(probe, "Probe must not be null!");

		this.probe = probe;
		this.exampleSpec = exampleSpec;
	}

	/**
	 * Create a new {@link Example} including all non-null properties by default.
	 *
	 * @param probe must not be {@literal null}.
	 * @return
	 */
	public static <T> Example<T> of(T probe) {
		return new Example<T>(probe);
	}

	/**
	 * Create a new {@link Example} with a configured {@link ExampleSpec}.
	 *
	 * @param probe must not be {@literal null}.
	 * @param exampleSpec must not be {@literal null}.
	 * @return
	 */
	public static <T> Example<T> of(T probe, ExampleSpec exampleSpec) {
		return new Example<T>(probe, exampleSpec);
	}

	/**
	 * Create a new {@link Example} with a configured {@link TypedExampleSpec}.
	 *
	 * @param probe must not be {@literal null}.
	 * @param exampleSpec must not be {@literal null}.
	 * @return
	 */
	public static <T, S extends T> Example<S> of(S probe, TypedExampleSpec<T> exampleSpec) {
		return new Example<S>(probe, exampleSpec);
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
	 * Get the {@link ExampleSpec} used.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleSpec getExampleSpec() {
		return exampleSpec;
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

	/**
	 * Get the actual result type for the query. The result type can be different when using {@link TypedExampleSpec}.
	 *
	 * @return
	 * @see ClassUtils#getUserClass(Class)
	 */
	@SuppressWarnings("unchecked")
	public <S extends T> Class<S> getResultType() {
		return (Class<S>) (exampleSpec instanceof TypedExampleSpec<?> ? ((TypedExampleSpec<T>) exampleSpec).getType()
				: getProbeType());
	}

}
