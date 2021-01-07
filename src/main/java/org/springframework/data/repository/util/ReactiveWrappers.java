/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;
import org.springframework.data.util.ProxyUtils;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class to expose details about reactive wrapper types. This class exposes whether a reactive wrapper is
 * supported in general and whether a particular type is suitable for no-value/single-value/multi-value usage.
 * <p>
 * Supported types are discovered by their availability on the class path. This class is typically used to determine
 * multiplicity and whether a reactive wrapper type is acceptable for a specific operation.
 * <p>
 * <strong>Note:</strong> As of Spring Data 2.4, support for RxJava 1.x is deprecated in favor of RxJava 2 and 3.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Gerrit Meier
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
 * @see io.reactivex.rxjava3.core.Single
 * @see io.reactivex.rxjava3.core.Maybe
 * @see io.reactivex.rxjava3.core.Observable
 * @see io.reactivex.rxjava3.core.Completable
 * @see io.reactivex.rxjava3.core.Flowable
 * @see Mono
 * @see Flux
 */
public abstract class ReactiveWrappers {

	private static final boolean PROJECT_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.publisher.Flux",
			ReactiveWrappers.class.getClassLoader());

	@Deprecated
	private static final boolean RXJAVA1_PRESENT = ClassUtils.isPresent("rx.Observable",
			ReactiveWrappers.class.getClassLoader())
			&& ClassUtils.isPresent("rx.RxReactiveStreams",
			ReactiveWrappers.class.getClassLoader());

	private static final boolean RXJAVA2_PRESENT = ClassUtils.isPresent("io.reactivex.Flowable",
			ReactiveWrappers.class.getClassLoader());

	private static final boolean RXJAVA3_PRESENT = ClassUtils.isPresent("io.reactivex.rxjava3.core.Flowable",
			ReactiveWrappers.class.getClassLoader());

	private static final boolean KOTLIN_COROUTINES_PRESENT = ClassUtils.isPresent("kotlinx.coroutines.reactor.MonoKt",
			ReactiveWrappers.class.getClassLoader());

	private ReactiveWrappers() {}

	/**
	 * Enumeration of supported reactive libraries.
	 *
	 * @author Mark Paluch
	 */
	public enum ReactiveLibrary {

		PROJECT_REACTOR,

		/**
		 * @deprecated since 2.4, use RxJava 2 or 3 instead.
		 */
		@Deprecated
		RXJAVA1, RXJAVA2, RXJAVA3, KOTLIN_COROUTINES;
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
			case RXJAVA3:
				return RXJAVA3_PRESENT;
			case KOTLIN_COROUTINES:
				return PROJECT_REACTOR_PRESENT && KOTLIN_COROUTINES_PRESENT;
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
		return isAvailable() && isWrapper(ProxyUtils.getUserClass(type));
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
	 * @deprecated not supported anymore.
	 */
	@Deprecated
	public static Collection<Class<?>> getNoValueTypes() {
		return Collections.emptyList();
	}

	/**
	 * Returns a collection of single-value wrapper types.
	 *
	 * @return a collection of single-value wrapper types.
	 * @deprecated not supported anymore.
	 */
	@Deprecated
	public static Collection<Class<?>> getSingleValueTypes() {
		return Collections.emptyList();
	}

	/**
	 * Returns a collection of multi-value wrapper types.
	 *
	 * @return a collection of multi-value wrapper types.
	 * @deprecated not supported anymore.
	 */
	@Deprecated
	public static Collection<Class<?>> getMultiValueTypes() {
		return Collections.emptyList();
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

		ReactiveAdapterRegistry adapterRegistry = ReactiveWrapperConverters.RegistryHolder.REACTIVE_ADAPTER_REGISTRY;

		if (adapterRegistry == null) {
			return Optional.empty();
		}

		ReactiveAdapter adapter = adapterRegistry.getAdapter(type);
		if (adapter != null && adapter.getDescriptor().isDeferred()) {
			return Optional.of(adapter.getDescriptor());
		}

		return Optional.empty();
	}
}
