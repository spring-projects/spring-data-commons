/*
 * Copyright 2016-2018 the original author or authors.
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

import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveTypeDescriptor;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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

	private static final boolean PROJECT_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Mono",
			ReactiveWrappers.class.getClassLoader());

	private static final boolean RXJAVA1_PRESENT = ClassUtils.isPresent("rx.Completable",
			ReactiveWrappers.class.getClassLoader());

	private static final boolean RXJAVA2_PRESENT = ClassUtils.isPresent("io.reactivex.Flowable",
			ReactiveWrappers.class.getClassLoader());

	private static final Collection<ReactiveTypeDescriptor> REACTIVE_WRAPPERS;

	/**
	 * Enumeration of supported reactive libraries.
	 *
	 * @author Mark Paluch
	 */
	static enum ReactiveLibrary {
		PROJECT_REACTOR, RXJAVA1, RXJAVA2;
	}

	static {

		Collection<ReactiveTypeDescriptor> reactiveWrappers = new ArrayList<>(5);

		if (RXJAVA1_PRESENT) {

			reactiveWrappers.add(ReactiveTypeDescriptor.singleRequiredValue(Single.class));
			reactiveWrappers.add(ReactiveTypeDescriptor.noValue(Completable.class, Completable::complete));
			reactiveWrappers.add(ReactiveTypeDescriptor.multiValue(Observable.class, Observable::empty));
		}

		if (RXJAVA2_PRESENT) {

			reactiveWrappers.add(ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.Single.class));
			reactiveWrappers
					.add(ReactiveTypeDescriptor.singleOptionalValue(io.reactivex.Maybe.class, io.reactivex.Maybe::empty));
			reactiveWrappers
					.add(ReactiveTypeDescriptor.noValue(io.reactivex.Completable.class, io.reactivex.Completable::complete));
			reactiveWrappers
					.add(ReactiveTypeDescriptor.multiValue(io.reactivex.Flowable.class, io.reactivex.Flowable::empty));
			reactiveWrappers
					.add(ReactiveTypeDescriptor.multiValue(io.reactivex.Observable.class, io.reactivex.Observable::empty));
		}

		if (PROJECT_REACTOR_PRESENT) {

			reactiveWrappers.add(ReactiveTypeDescriptor.singleOptionalValue(Mono.class, Mono::empty));
			reactiveWrappers.add(ReactiveTypeDescriptor.multiValue(Flux.class, Flux::empty));
			reactiveWrappers.add(ReactiveTypeDescriptor.multiValue(Publisher.class, Flux::empty));
		}

		REACTIVE_WRAPPERS = Collections.unmodifiableCollection(reactiveWrappers);
	}

	/**
	 * Returns {@literal true} if reactive support is available. More specifically, whether any of the libraries defined
	 * in {@link ReactiveLibrary} are on the class path.
	 *
	 * @return {@literal true} if reactive support is available.
	 */
	public static boolean isAvailable() {
		return Arrays.stream(ReactiveLibrary.values()).anyMatch(ReactiveWrappers::isAvailable);
	}

	/**
	 * Returns {@literal true} if the {@link ReactiveLibrary} is available.
	 *
	 * @param reactiveLibrary must not be {@literal null}.
	 * @return {@literal true} if the {@link ReactiveLibrary} is available.
	 */
	public static boolean isAvailable(ReactiveLibrary reactiveLibrary) {

		Assert.notNull(reactiveLibrary, "Reactive library must not be null!");

		switch (reactiveLibrary) {
			case PROJECT_REACTOR:
				return PROJECT_REACTOR_PRESENT;
			case RXJAVA1:
				return RXJAVA1_PRESENT;
			case RXJAVA2:
				return RXJAVA2_PRESENT;
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
		return isWrapper(ClassUtils.getUserClass(type));
	}

	/**
	 * Returns whether the given type uses any reactive wrapper type in its method signatures.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean usesReactiveType(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return Arrays.stream(type.getMethods())//
				.flatMap(ReflectionUtils::returnTypeAndParameters)//
				.anyMatch(ReactiveWrappers::supports);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type that contains no value.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type that contains no value.
	 */
	public static boolean isNoValueType(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null!");

		return findDescriptor(type).map(ReactiveTypeDescriptor::isNoValue).orElse(false);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 */
	public static boolean isSingleValueType(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null!");

		return findDescriptor(type).map(it -> !it.isMultiValue() && !it.isNoValue()).orElse(false);
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

		Assert.notNull(type, "Candidate type must not be null!");

		// Prevent single-types with a multi-hierarchy supertype to be reported as multi type
		// See Mono implements Publisher
		return isSingleValueType(type) ? false
				: findDescriptor(type).map(ReactiveTypeDescriptor::isMultiValue).orElse(false);
	}

	/**
	 * Returns a collection of no-value wrapper types.
	 *
	 * @return a collection of no-value wrapper types.
	 */
	public static Collection<Class<?>> getNoValueTypes() {

		return REACTIVE_WRAPPERS.stream()//
				.filter(ReactiveTypeDescriptor::isNoValue)//
				.map(ReactiveTypeDescriptor::getReactiveType)//
				.collect(Collectors.toList());
	}

	/**
	 * Returns a collection of single-value wrapper types.
	 *
	 * @return a collection of single-value wrapper types.
	 */
	public static Collection<Class<?>> getSingleValueTypes() {

		return REACTIVE_WRAPPERS.stream()//
				.filter(entry -> !entry.isMultiValue())//
				.map(ReactiveTypeDescriptor::getReactiveType).collect(Collectors.toList());
	}

	/**
	 * Returns a collection of multi-value wrapper types.
	 *
	 * @return a collection of multi-value wrapper types.
	 */
	public static Collection<Class<?>> getMultiValueTypes() {

		return REACTIVE_WRAPPERS.stream()//
				.filter(ReactiveTypeDescriptor::isMultiValue)//
				.map(ReactiveTypeDescriptor::getReactiveType)//
				.collect(Collectors.toList());
	}

	/**
	 * Returns whether the given type is a reactive wrapper type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static boolean isWrapper(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null!");

		return isNoValueType(type) || isSingleValueType(type) || isMultiValueType(type);
	}

	/**
	 * Looks up a {@link ReactiveTypeDescriptor} for the given wrapper type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static Optional<ReactiveTypeDescriptor> findDescriptor(Class<?> type) {

		Assert.notNull(type, "Wrapper type must not be null!");

		return REACTIVE_WRAPPERS.stream()//
				.filter(it -> ClassUtils.isAssignable(it.getReactiveType(), type))//
				.findFirst();
	}
}
