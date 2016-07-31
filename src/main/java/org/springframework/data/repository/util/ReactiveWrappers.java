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
package org.springframework.data.repository.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapter.Descriptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Utility class to expose details about reactive wrapper types. This class exposes whether a reactive wrapper is
 * supported in general and whether a particular type is suitable for no-value/single-value/multi-value usage.
 * <p>
 * Supported types are discovered by their availability on the class path. This class is typically used to determine
 * multiplicity and whether a reactive wrapper type is acceptable for a specific operation.
 * 
 * @author Mark Paluch
 * @since 2.0
 * @see org.reactivestreams.Publisher
 * @see rx.Single
 * @see rx.Observable
 * @see rx.Completable
 * @see io.reactivex.Single
 * @see io.reactivex.Maybe
 * @see io.reactivex.Observable
 * @see io.reactivex.Completable
 * @see io.reactivex.Flowable
 * @see Mono
 * @see Flux
 */
@UtilityClass
public class ReactiveWrappers {

	static final boolean PROJECT_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Mono",
			ReactiveWrappers.class.getClassLoader());

	static final boolean RXJAVA1_PRESENT = ClassUtils.isPresent("rx.Completable",
			ReactiveWrappers.class.getClassLoader());

	static final boolean RXJAVA2_PRESENT = ClassUtils.isPresent("io.reactivex.Flowable",
			ReactiveWrappers.class.getClassLoader());

	private static final Map<Class<?>, Descriptor> REACTIVE_WRAPPERS;

	static {

		Map<Class<?>, Descriptor> reactiveWrappers = new LinkedHashMap<>(3);

		if (RXJAVA1_PRESENT) {

			reactiveWrappers.put(Single.class, new Descriptor(false, true, false));
			reactiveWrappers.put(Completable.class, new Descriptor(false, true, true));
			reactiveWrappers.put(Observable.class, new Descriptor(true, true, false));
		}

		if (RXJAVA2_PRESENT) {

			reactiveWrappers.put(io.reactivex.Single.class, new Descriptor(false, true, false));
			reactiveWrappers.put(io.reactivex.Maybe.class, new Descriptor(false, true, false));
			reactiveWrappers.put(io.reactivex.Completable.class, new Descriptor(false, true, true));
			reactiveWrappers.put(io.reactivex.Flowable.class, new Descriptor(true, true, false));
			reactiveWrappers.put(io.reactivex.Observable.class, new Descriptor(true, true, false));
		}

		if (PROJECT_REACTOR_PRESENT) {

			reactiveWrappers.put(Mono.class, new Descriptor(false, true, false));
			reactiveWrappers.put(Flux.class, new Descriptor(true, true, true));
			reactiveWrappers.put(Publisher.class, new Descriptor(true, true, true));
		}

		REACTIVE_WRAPPERS = Collections.unmodifiableMap(reactiveWrappers);
	}

	/**
	 * Returns {@literal true} if reactive support is available. More specifically, whether RxJava1/2 or Project Reactor
	 * libraries are on the class path.
	 *
	 * @return {@literal true} if reactive support is available.
	 */
	public static boolean isAvailable() {
		return RXJAVA1_PRESENT || RXJAVA2_PRESENT || PROJECT_REACTOR_PRESENT;
	}

	/**
	 * Returns {@literal true} if the {@code type} is a supported reactive wrapper type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if the {@code type} is a supported reactive wrapper type.
	 */
	public static boolean supports(Class<?> type) {
		return isNoValueType(type) || isSingleValueType(type) || isMultiValueType(type);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type that contains no value.
	 * 
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type that contains no value.
	 */
	public static boolean isNoValueType(Class<?> type) {

		Assert.notNull(type, "Class must not be null!");

		return findDescriptor(type).map(Descriptor::isNoValue).orElse(false);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 * 
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 */
	public static boolean isSingleValueType(Class<?> type) {

		Assert.notNull(type, "Class must not be null!");

		return findDescriptor(type).map((descriptor) -> !descriptor.isMultiValue() && !descriptor.isNoValue())
				.orElse(false);
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

		Assert.notNull(type, "Class must not be null!");

		// Prevent single-types with a multi-hierarchy supertype to be reported as multi type
		// See Mono implements Publisher
		if (isSingleValueType(type)) {
			return false;
		}

		return findDescriptor(type).map(Descriptor::isMultiValue).orElse(false);
	}

	/**
	 * Returns a collection of No-Value wrapper types.
	 *
	 * @return a collection of No-Value wrapper types.
	 */
	public static Collection<Class<?>> getNoValueTypes() {
		return REACTIVE_WRAPPERS.entrySet().stream().filter(entry -> entry.getValue().isNoValue()).map(Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a collection of Single-Value wrapper types.
	 *
	 * @return a collection of Single-Value wrapper types.
	 */
	public static Collection<Class<?>> getSingleValueTypes() {
		return REACTIVE_WRAPPERS.entrySet().stream().filter(entry -> !entry.getValue().isMultiValue()).map(Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * Returns a collection of Multi-Value wrapper types.
	 *
	 * @return a collection of Multi-Value wrapper types.
	 */
	public static Collection<Class<?>> getMultiValueTypes() {
		return REACTIVE_WRAPPERS.entrySet().stream().filter(entry -> entry.getValue().isMultiValue()).map(Entry::getKey)
				.collect(Collectors.toList());
	}

	private static Optional<Descriptor> findDescriptor(Class<?> rhsType) {

		for (Class<?> type : REACTIVE_WRAPPERS.keySet()) {
			if (org.springframework.util.ClassUtils.isAssignable(type, rhsType)) {
				return Optional.ofNullable(REACTIVE_WRAPPERS.get(type));
			}
		}
		return Optional.empty();
	}
}
