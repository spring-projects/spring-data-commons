/*
 * Copyright 2016-2024 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;
import org.springframework.lang.Nullable;
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
 * @author Gerrit Meier
 * @author Hantsy Bai
 * @since 3.0
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
 */
public abstract class ReactiveWrappers {

	public static final boolean PROJECT_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Flux",
			ReactiveWrappers.class.getClassLoader());

	public static final boolean RXJAVA3_PRESENT = ClassUtils.isPresent("io.reactivex.rxjava3.core.Flowable",
			ReactiveWrappers.class.getClassLoader());

	public static final boolean KOTLIN_COROUTINES_PRESENT = ClassUtils.isPresent("kotlinx.coroutines.reactor.MonoKt",
			ReactiveWrappers.class.getClassLoader());

	public static final boolean MUTINY_PRESENT = ClassUtils.isPresent("io.smallrye.mutiny.Multi",
			ReactiveWrappers.class.getClassLoader());

	public static final boolean IS_REACTIVE_AVAILABLE = Arrays.stream(ReactiveLibrary.values())
			.anyMatch(ReactiveWrappers::isAvailable);
	private static final Map<Class<?>, Boolean> IS_REACTIVE_TYPE = new ConcurrentHashMap<>();

	private ReactiveWrappers() {}

	/**
	 * Enumeration of supported reactive libraries.
	 *
	 * @author Mark Paluch
	 */
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
		return IS_REACTIVE_AVAILABLE;
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
				return PROJECT_REACTOR_PRESENT;
			case RXJAVA3:
				return RXJAVA3_PRESENT;
			case KOTLIN_COROUTINES:
				return PROJECT_REACTOR_PRESENT && KOTLIN_COROUTINES_PRESENT;
			case MUTINY:
				return MUTINY_PRESENT;
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
		return isAvailable() && IS_REACTIVE_TYPE.computeIfAbsent(type, key -> isWrapper(ProxyUtils.getUserClass(key)));
	}

	/**
	 * Returns whether the given type uses any reactive wrapper type in its method signatures.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean usesReactiveType(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

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

		Assert.notNull(type, "Candidate type must not be null");

		return findDescriptor(type).map(ReactiveTypeDescriptor::isNoValue).orElse(false);
	}

	/**
	 * Returns {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@code type} is a reactive wrapper type for a single value.
	 */
	public static boolean isSingleValueType(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null");

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

		Assert.notNull(type, "Candidate type must not be null");

		// Prevent single-types with a multi-hierarchy supertype to be reported as multi type
		// See Mono implements Publisher
		return isSingleValueType(type) ? false
				: findDescriptor(type).map(ReactiveTypeDescriptor::isMultiValue).orElse(false);
	}

	/**
	 * Returns whether the given type is a reactive wrapper type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static boolean isWrapper(Class<?> type) {

		Assert.notNull(type, "Candidate type must not be null");

		return isNoValueType(type) || isSingleValueType(type) || isMultiValueType(type);
	}

	/**
	 * Looks up a {@link ReactiveTypeDescriptor} for the given wrapper type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static Optional<ReactiveTypeDescriptor> findDescriptor(Class<?> type) {

		Assert.notNull(type, "Wrapper type must not be null");

		ReactiveAdapterRegistry adapterRegistry = RegistryHolder.REACTIVE_ADAPTER_REGISTRY;

		if (adapterRegistry == null) {
			return Optional.empty();
		}

		ReactiveAdapter adapter = adapterRegistry.getAdapter(type);
		if (adapter != null && adapter.getDescriptor().isDeferred()) {
			return Optional.of(adapter.getDescriptor());
		}

		return Optional.empty();
	}

	/**
	 * Holder for delayed initialization of {@link ReactiveAdapterRegistry}.
	 *
	 * @author Mark Paluch
	 */
	static class RegistryHolder {

		static final @Nullable ReactiveAdapterRegistry REACTIVE_ADAPTER_REGISTRY;

		static {

			if (ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)) {
				REACTIVE_ADAPTER_REGISTRY = ReactiveAdapterRegistry.getSharedInstance();
			} else {
				REACTIVE_ADAPTER_REGISTRY = null;
			}
		}
	}
}
