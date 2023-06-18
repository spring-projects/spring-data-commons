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
package org.springframework.data.repository.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * Utility class to expose details about reactive wrapper types. This class exposes whether a reactive wrapper is
 * supported in general and whether a particular type is suitable for no-value/single-value/multi-value usage.
 * <p>
 * Supported types are discovered by their availability on the class path. This class is typically used to determine
 * multiplicity and whether a reactive wrapper type is acceptable for a specific operation.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Gerrit Meier
 * @author Hantsy Bai
 * @since 2.0
 * @see org.reactivestreams.Publisher
 * @see io.reactivex.rxjava3.core.Single
 * @see io.reactivex.rxjava3.core.Maybe
 * @see io.reactivex.rxjava3.core.Observable
 * @see io.reactivex.rxjava3.core.Completable
 * @see io.reactivex.rxjava3.core.Flowable
 * @see io.smallrye.mutiny.Multi
 * @see io.smallrye.mutiny.Uni
 * @see Mono
 * @see Flux
 * @deprecated since 3.0, use {@link org.springframework.data.util.ReactiveWrappers} instead as the utility was moved
 *             into the {@code org.springframework.data.util} package.
 */
@Deprecated(since = "3.0", forRemoval = true)
public abstract class ReactiveWrappers {

	private ReactiveWrappers() {}

	/**
	 * Enumeration of supported reactive libraries.
	 *
	 * @author Mark Paluch
	 * @deprecated use {@link org.springframework.data.util.ReactiveWrappers.ReactiveLibrary} instead.
	 */
	@Deprecated(since = "3.0", forRemoval = true)
	public enum ReactiveLibrary {

		PROJECT_REACTOR, RXJAVA3, KOTLIN_COROUTINES, MUTINY;
	}

	/**
	 * Returns {@literal true} if reactive support is available. More specifically, whether any of the libraries defined
	 * in {@link ReactiveLibrary} are on the class path.
	 *
	 * @return {@literal true} if reactive support is available.
	 */
	public static boolean isAvailable() {
		return org.springframework.data.util.ReactiveWrappers.isAvailable();
	}

	/**
	 * Returns {@literal true} if the {@link ReactiveLibrary} is available.
	 *
	 * @param reactiveLibrary must not be {@literal null}.
	 * @return {@literal true} if the {@link ReactiveLibrary} is available.
	 */
	public static boolean isAvailable(ReactiveLibrary reactiveLibrary) {

		Assert.notNull(reactiveLibrary, "Reactive library must not be null");

		switch (reactiveLibrary) {
			case PROJECT_REACTOR:
				return org.springframework.data.util.ReactiveWrappers.PROJECT_REACTOR_PRESENT;
			case RXJAVA3:
				return org.springframework.data.util.ReactiveWrappers.RXJAVA3_PRESENT;
			case KOTLIN_COROUTINES:
				return org.springframework.data.util.ReactiveWrappers.PROJECT_REACTOR_PRESENT
						&& org.springframework.data.util.ReactiveWrappers.KOTLIN_COROUTINES_PRESENT;
			case MUTINY:
				return org.springframework.data.util.ReactiveWrappers.MUTINY_PRESENT;
			default:
				throw new IllegalArgumentException(String.format("Reactive library %s not supported", reactiveLibrary));
		}
	}

	/**
	 * Returns {@literal true} if the {@code type} is a supported reactive wrapper type.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if the {@code type} is a supported reactive wrapper type.
	 */
	public static boolean supports(Class<?> type) {
		return org.springframework.data.util.ReactiveWrappers.supports(type);
	}

	/**
	 * Returns whether the given type uses any reactive wrapper type in its method signatures.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean usesReactiveType(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

		return org.springframework.data.util.ReactiveWrappers.usesReactiveType(type);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type that contains no value.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type that contains no value.
	 */
	public static boolean isNoValueType(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null");

		return org.springframework.data.util.ReactiveWrappers.isNoValueType(type);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 */
	public static boolean isSingleValueType(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null");

		return org.springframework.data.util.ReactiveWrappers.isSingleValueType(type);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type supporting multiple values ({@code 0..N}
	 * elements).
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type supporting multiple values ({@code 0..N}
	 *         elements).
	 */
	public static boolean isMultiValueType(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null");

		return org.springframework.data.util.ReactiveWrappers.isMultiValueType(type);
	}

}
