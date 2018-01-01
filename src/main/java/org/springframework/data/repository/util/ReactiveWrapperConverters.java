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

import static org.springframework.data.repository.util.ReactiveWrapperConverters.RegistryHolder.*;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.repository.util.ReactiveWrappers.ReactiveLibrary;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Conversion support for reactive wrapper types. This class is a reactive extension to
 * {@link QueryExecutionConverters}.
 * <p>
 * This class discovers reactive wrapper availability and their conversion support based on the class path. Reactive
 * wrapper types might be supported/on the class path but conversion may require additional dependencies.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.0
 * @see ReactiveWrappers
 * @see ReactiveAdapterRegistry
 */
@UtilityClass
public class ReactiveWrapperConverters {

	private static final List<ReactiveTypeWrapper<?>> REACTIVE_WRAPPERS = new ArrayList<>();
	private static final GenericConversionService GENERIC_CONVERSION_SERVICE = new GenericConversionService();

	static {

		if (ReactiveWrappers.isAvailable(ReactiveLibrary.RXJAVA1)) {

			REACTIVE_WRAPPERS.add(RxJava1SingleWrapper.INSTANCE);
			REACTIVE_WRAPPERS.add(RxJava1ObservableWrapper.INSTANCE);
		}

		if (ReactiveWrappers.isAvailable(ReactiveLibrary.RXJAVA2)) {

			REACTIVE_WRAPPERS.add(RxJava2SingleWrapper.INSTANCE);
			REACTIVE_WRAPPERS.add(RxJava2MaybeWrapper.INSTANCE);
			REACTIVE_WRAPPERS.add(RxJava2ObservableWrapper.INSTANCE);
			REACTIVE_WRAPPERS.add(RxJava2FlowableWrapper.INSTANCE);
		}

		if (ReactiveWrappers.isAvailable(ReactiveLibrary.PROJECT_REACTOR)) {

			REACTIVE_WRAPPERS.add(FluxWrapper.INSTANCE);
			REACTIVE_WRAPPERS.add(MonoWrapper.INSTANCE);
			REACTIVE_WRAPPERS.add(PublisherWrapper.INSTANCE);
		}

		registerConvertersIn(GENERIC_CONVERSION_SERVICE);
	}

	/**
	 * Registers converters for wrapper types found on the classpath.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	private static ConversionService registerConvertersIn(ConfigurableConversionService conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		if (ReactiveWrappers.isAvailable(ReactiveLibrary.PROJECT_REACTOR)) {

			if (ReactiveWrappers.isAvailable(ReactiveLibrary.RXJAVA1)) {

				conversionService.addConverter(PublisherToRxJava1CompletableConverter.INSTANCE);
				conversionService.addConverter(RxJava1CompletableToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava1CompletableToMonoConverter.INSTANCE);

				conversionService.addConverter(PublisherToRxJava1SingleConverter.INSTANCE);
				conversionService.addConverter(RxJava1SingleToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava1SingleToMonoConverter.INSTANCE);
				conversionService.addConverter(RxJava1SingleToFluxConverter.INSTANCE);

				conversionService.addConverter(PublisherToRxJava1ObservableConverter.INSTANCE);
				conversionService.addConverter(RxJava1ObservableToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava1ObservableToMonoConverter.INSTANCE);
				conversionService.addConverter(RxJava1ObservableToFluxConverter.INSTANCE);
			}

			if (ReactiveWrappers.isAvailable(ReactiveLibrary.RXJAVA2)) {

				conversionService.addConverter(PublisherToRxJava2CompletableConverter.INSTANCE);
				conversionService.addConverter(RxJava2CompletableToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava2CompletableToMonoConverter.INSTANCE);

				conversionService.addConverter(PublisherToRxJava2SingleConverter.INSTANCE);
				conversionService.addConverter(RxJava2SingleToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava2SingleToMonoConverter.INSTANCE);
				conversionService.addConverter(RxJava2SingleToFluxConverter.INSTANCE);

				conversionService.addConverter(PublisherToRxJava2ObservableConverter.INSTANCE);
				conversionService.addConverter(RxJava2ObservableToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava2ObservableToMonoConverter.INSTANCE);
				conversionService.addConverter(RxJava2ObservableToFluxConverter.INSTANCE);

				conversionService.addConverter(PublisherToRxJava2FlowableConverter.INSTANCE);
				conversionService.addConverter(RxJava2FlowableToPublisherConverter.INSTANCE);

				conversionService.addConverter(PublisherToRxJava2MaybeConverter.INSTANCE);
				conversionService.addConverter(RxJava2MaybeToPublisherConverter.INSTANCE);
				conversionService.addConverter(RxJava2MaybeToMonoConverter.INSTANCE);
				conversionService.addConverter(RxJava2MaybeToFluxConverter.INSTANCE);
			}

			conversionService.addConverter(PublisherToMonoConverter.INSTANCE);
			conversionService.addConverter(PublisherToFluxConverter.INSTANCE);

			if (ReactiveWrappers.isAvailable(ReactiveLibrary.RXJAVA1)) {
				conversionService.addConverter(RxJava1SingleToObservableConverter.INSTANCE);
				conversionService.addConverter(RxJava1ObservableToSingleConverter.INSTANCE);
			}

			if (ReactiveWrappers.isAvailable(ReactiveLibrary.RXJAVA2)) {
				conversionService.addConverter(RxJava2SingleToObservableConverter.INSTANCE);
				conversionService.addConverter(RxJava2ObservableToSingleConverter.INSTANCE);
				conversionService.addConverter(RxJava2ObservableToMaybeConverter.INSTANCE);
			}
		}

		return conversionService;
	}

	/**
	 * Returns whether the given type is supported for wrapper type conversion.
	 * <p>
	 * NOTE: A reactive wrapper type might be supported in general by {@link ReactiveWrappers#supports(Class)} but not
	 * necessarily for conversion using this method.
	 * </p>
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if the {@code type} is a supported reactive wrapper type.
	 */
	public static boolean supports(Class<?> type) {
		return RegistryHolder.REACTIVE_ADAPTER_REGISTRY != null
				&& RegistryHolder.REACTIVE_ADAPTER_REGISTRY.getAdapter(type) != null;
	}

	/**
	 * Casts or adopts the given wrapper type to a target wrapper type.
	 *
	 * @param reactiveObject the stream, must not be {@literal null}.
	 * @param targetWrapperType must not be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T toWrapper(Object reactiveObject, Class<? extends T> targetWrapperType) {

		Assert.notNull(reactiveObject, "Reactive source object must not be null!");
		Assert.notNull(targetWrapperType, "Reactive target type must not be null!");

		if (targetWrapperType.isAssignableFrom(reactiveObject.getClass())) {
			return (T) reactiveObject;
		}

		return GENERIC_CONVERSION_SERVICE.convert(reactiveObject, targetWrapperType);
	}

	/**
	 * Maps elements of a reactive element stream to other elements.
	 *
	 * @param reactiveObject must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T map(Object reactiveObject, Function<Object, Object> converter) {

		Assert.notNull(reactiveObject, "Reactive source object must not be null!");
		Assert.notNull(converter, "Converter must not be null!");

		return REACTIVE_WRAPPERS.stream()//
				.filter(it -> ClassUtils.isAssignable(it.getWrapperClass(), reactiveObject.getClass()))//
				.findFirst()//
				.map(it -> (T) it.map(reactiveObject, converter))//
				.orElseThrow(() -> new IllegalStateException(String.format("Cannot apply converter to %s", reactiveObject)));
	}

	/**
	 * Return {@literal true} if objects of {@code sourceType} can be converted to the {@code targetType}.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return {@literal true} if a conversion can be performed.
	 */
	public static boolean canConvert(Class<?> sourceType, Class<?> targetType) {

		Assert.notNull(sourceType, "Source type must not be null!");
		Assert.notNull(targetType, "Target type must not be null!");

		return GENERIC_CONVERSION_SERVICE.canConvert(sourceType, targetType);
	}

	/**
	 * Returns the {@link ReactiveAdapter} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 * @throws IllegalStateException if no adapter registry could be found.
	 * @throws IllegalArgumentException if no adapter could be found for the given type.
	 */
	private static ReactiveAdapter getRequiredAdapter(Class<?> type) {

		ReactiveAdapterRegistry registry = REACTIVE_ADAPTER_REGISTRY;

		if (registry == null) {
			throw new IllegalStateException("No reactive adapter registry found!");
		}

		ReactiveAdapter adapter = registry.getAdapter(type);

		if (adapter == null) {
			throw new IllegalArgumentException(String.format("Expected to find reactive adapter for %s but couldn't!", type));
		}

		return adapter;
	}

	// -------------------------------------------------------------------------
	// Wrapper descriptors
	// -------------------------------------------------------------------------

	/**
	 * Wrapper descriptor that can apply a {@link Function} to map items inside its stream.
	 *
	 * @author Mark Paluch
	 * @author Christoph Strobl
	 */
	private interface ReactiveTypeWrapper<T> {

		/**
		 * @return the wrapper class.
		 */
		Class<? super T> getWrapperClass();

		/**
		 * Apply a {@link Function} to a reactive type.
		 *
		 * @param wrapper the reactive type, must not be {@literal null}.
		 * @param function the converter, must not be {@literal null}.
		 * @return the reactive type applying conversion.
		 */
		Object map(Object wrapper, Function<Object, Object> function);
	}

	/**
	 * Wrapper for Project Reactor's {@link Mono}.
	 */
	private enum MonoWrapper implements ReactiveTypeWrapper<Mono<?>> {

		INSTANCE;

		@Override
		public Class<? super Mono<?>> getWrapperClass() {
			return Mono.class;
		}

		@Override
		public Mono<?> map(Object wrapper, Function<Object, Object> function) {
			return ((Mono<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for Project Reactor's {@link Flux}.
	 */
	private enum FluxWrapper implements ReactiveTypeWrapper<Flux<?>> {

		INSTANCE;

		@Override
		public Class<? super Flux<?>> getWrapperClass() {
			return Flux.class;
		}

		public Flux<?> map(Object wrapper, Function<Object, Object> function) {
			return ((Flux<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for Reactive Stream's {@link Publisher}.
	 */
	private enum PublisherWrapper implements ReactiveTypeWrapper<Publisher<?>> {

		INSTANCE;

		@Override
		public Class<? super Publisher<?>> getWrapperClass() {
			return Publisher.class;
		}

		@Override
		public Publisher<?> map(Object wrapper, Function<Object, Object> function) {

			if (wrapper instanceof Mono) {
				return MonoWrapper.INSTANCE.map(wrapper, function);
			}

			if (wrapper instanceof Flux) {
				return FluxWrapper.INSTANCE.map(wrapper, function);
			}

			return FluxWrapper.INSTANCE.map(Flux.from((Publisher<?>) wrapper), function);
		}
	}

	/**
	 * Wrapper for RxJava 1's {@link Single}.
	 */
	private enum RxJava1SingleWrapper implements ReactiveTypeWrapper<Single<?>> {

		INSTANCE;

		@Override
		public Class<? super Single<?>> getWrapperClass() {
			return Single.class;
		}

		@Override
		public Single<?> map(Object wrapper, Function<Object, Object> function) {
			return ((Single<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for RxJava 1's {@link Observable}.
	 */
	private enum RxJava1ObservableWrapper implements ReactiveTypeWrapper<Observable<?>> {

		INSTANCE;

		@Override
		public Class<? super Observable<?>> getWrapperClass() {
			return Observable.class;
		}

		@Override
		public Observable<?> map(Object wrapper, Function<Object, Object> function) {
			return ((Observable<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for RxJava 2's {@link io.reactivex.Single}.
	 */
	private enum RxJava2SingleWrapper implements ReactiveTypeWrapper<io.reactivex.Single<?>> {

		INSTANCE;

		@Override
		public Class<? super io.reactivex.Single<?>> getWrapperClass() {
			return io.reactivex.Single.class;
		}

		@Override
		public io.reactivex.Single<?> map(Object wrapper, Function<Object, Object> function) {
			return ((io.reactivex.Single<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for RxJava 2's {@link io.reactivex.Maybe}.
	 */
	private enum RxJava2MaybeWrapper implements ReactiveTypeWrapper<Maybe<?>> {

		INSTANCE;

		@Override
		public Class<? super io.reactivex.Maybe<?>> getWrapperClass() {
			return io.reactivex.Maybe.class;
		}

		@Override
		public io.reactivex.Maybe<?> map(Object wrapper, Function<Object, Object> function) {
			return ((io.reactivex.Maybe<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for RxJava 2's {@link io.reactivex.Observable}.
	 */
	private enum RxJava2ObservableWrapper implements ReactiveTypeWrapper<io.reactivex.Observable<?>> {

		INSTANCE;

		@Override
		public Class<? super io.reactivex.Observable<?>> getWrapperClass() {
			return io.reactivex.Observable.class;
		}

		@Override
		public io.reactivex.Observable<?> map(Object wrapper, Function<Object, Object> function) {
			return ((io.reactivex.Observable<?>) wrapper).map(function::apply);
		}
	}

	/**
	 * Wrapper for RxJava 2's {@link io.reactivex.Flowable}.
	 */
	private enum RxJava2FlowableWrapper implements ReactiveTypeWrapper<Flowable<?>> {

		INSTANCE;

		@Override
		public Class<? super Flowable<?>> getWrapperClass() {
			return io.reactivex.Flowable.class;
		}

		@Override
		public io.reactivex.Flowable<?> map(Object wrapper, Function<Object, Object> function) {
			return ((io.reactivex.Flowable<?>) wrapper).map(function::apply);
		}
	}

	// -------------------------------------------------------------------------
	// ReactiveStreams converters
	// -------------------------------------------------------------------------

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToFluxConverter implements Converter<Publisher<?>, Flux<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flux<?> convert(Publisher<?> source) {
			return Flux.from(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToMonoConverter implements Converter<Publisher<?>, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(Publisher<?> source) {
			return Mono.from(source);
		}
	}

	// -------------------------------------------------------------------------
	// RxJava 1 converters
	// -------------------------------------------------------------------------

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava1SingleConverter implements Converter<Publisher<?>, Single<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Single<?> convert(Publisher<?> source) {
			return (Single<?>) getRequiredAdapter(Single.class).fromPublisher(Mono.from(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Completable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava1CompletableConverter implements Converter<Publisher<?>, Completable> {

		INSTANCE;

		@Nonnull
		@Override
		public Completable convert(Publisher<?> source) {
			return (Completable) getRequiredAdapter(Completable.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Observable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava1ObservableConverter implements Converter<Publisher<?>, Observable<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Observable<?> convert(Publisher<?> source) {
			return (Observable<?>) getRequiredAdapter(Observable.class).fromPublisher(Flux.from(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1SingleToPublisherConverter implements Converter<Single<?>, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(Single<?> source) {
			return Flux.defer(() -> getRequiredAdapter(Single.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1SingleToMonoConverter implements Converter<Single<?>, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(Single<?> source) {
			return Mono.defer(() -> Mono.from(getRequiredAdapter(Single.class).toPublisher(source)));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1SingleToFluxConverter implements Converter<Single<?>, Flux<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flux<?> convert(Single<?> source) {
			return Flux.defer(() -> getRequiredAdapter(Single.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Completable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1CompletableToPublisherConverter implements Converter<Completable, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(Completable source) {
			return Flux.defer(() -> getRequiredAdapter(Completable.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Completable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1CompletableToMonoConverter implements Converter<Completable, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(Completable source) {
			return Mono.from(RxJava1CompletableToPublisherConverter.INSTANCE.convert(source));
		}
	}

	/**
	 * A {@link Converter} to convert an {@link Observable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1ObservableToPublisherConverter implements Converter<Observable<?>, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(Observable<?> source) {
			return Flux.defer(() -> getRequiredAdapter(Observable.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1ObservableToMonoConverter implements Converter<Observable<?>, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(Observable<?> source) {
			return Mono.defer(() -> Mono.from(getRequiredAdapter(Observable.class).toPublisher(source)));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1ObservableToFluxConverter implements Converter<Observable<?>, Flux<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flux<?> convert(Observable<?> source) {
			return Flux.defer(() -> getRequiredAdapter(Observable.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1ObservableToSingleConverter implements Converter<Observable<?>, Single<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Single<?> convert(Observable<?> source) {
			return source.toSingle();
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava1SingleToObservableConverter implements Converter<Single<?>, Observable<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Observable<?> convert(Single<?> source) {
			return source.toObservable();
		}
	}

	// -------------------------------------------------------------------------
	// RxJava 2 converters
	// -------------------------------------------------------------------------

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava2SingleConverter implements Converter<Publisher<?>, io.reactivex.Single<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Single<?> convert(Publisher<?> source) {
			return (io.reactivex.Single<?>) getRequiredAdapter(io.reactivex.Single.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Completable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava2CompletableConverter implements Converter<Publisher<?>, io.reactivex.Completable> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Completable convert(Publisher<?> source) {
			return (io.reactivex.Completable) getRequiredAdapter(io.reactivex.Completable.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Observable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava2ObservableConverter implements Converter<Publisher<?>, io.reactivex.Observable<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Observable<?> convert(Publisher<?> source) {
			return (io.reactivex.Observable<?>) getRequiredAdapter(io.reactivex.Observable.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2SingleToPublisherConverter implements Converter<io.reactivex.Single<?>, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(io.reactivex.Single<?> source) {
			return getRequiredAdapter(io.reactivex.Single.class).toPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Single} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2SingleToMonoConverter implements Converter<io.reactivex.Single<?>, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(io.reactivex.Single<?> source) {
			return Mono.from(getRequiredAdapter(io.reactivex.Single.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2SingleToFluxConverter implements Converter<io.reactivex.Single<?>, Flux<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flux<?> convert(io.reactivex.Single<?> source) {
			return Flux.from(getRequiredAdapter(io.reactivex.Single.class).toPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Completable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2CompletableToPublisherConverter implements Converter<io.reactivex.Completable, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(io.reactivex.Completable source) {
			return getRequiredAdapter(io.reactivex.Completable.class).toPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Completable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2CompletableToMonoConverter implements Converter<io.reactivex.Completable, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(io.reactivex.Completable source) {
			return Mono.from(RxJava2CompletableToPublisherConverter.INSTANCE.convert(source));
		}
	}

	/**
	 * A {@link Converter} to convert an {@link io.reactivex.Observable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2ObservableToPublisherConverter implements Converter<io.reactivex.Observable<?>, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(io.reactivex.Observable<?> source) {
			return source.toFlowable(BackpressureStrategy.BUFFER);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Observable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2ObservableToMonoConverter implements Converter<io.reactivex.Observable<?>, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(io.reactivex.Observable<?> source) {
			return Mono.from(source.toFlowable(BackpressureStrategy.BUFFER));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Observable} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2ObservableToFluxConverter implements Converter<io.reactivex.Observable<?>, Flux<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flux<?> convert(io.reactivex.Observable<?> source) {
			return Flux.from(source.toFlowable(BackpressureStrategy.BUFFER));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Flowable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava2FlowableConverter implements Converter<Publisher<?>, io.reactivex.Flowable<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Flowable<?> convert(Publisher<?> source) {
			return Flowable.fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Flowable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2FlowableToPublisherConverter implements Converter<io.reactivex.Flowable<?>, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(io.reactivex.Flowable<?> source) {
			return source;
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Flowable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum PublisherToRxJava2MaybeConverter implements Converter<Publisher<?>, io.reactivex.Maybe<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Maybe<?> convert(Publisher<?> source) {
			return (io.reactivex.Maybe<?>) getRequiredAdapter(Maybe.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Maybe} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2MaybeToPublisherConverter implements Converter<io.reactivex.Maybe<?>, Publisher<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Publisher<?> convert(io.reactivex.Maybe<?> source) {
			return source.toFlowable();
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Maybe} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2MaybeToMonoConverter implements Converter<io.reactivex.Maybe<?>, Mono<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Mono<?> convert(io.reactivex.Maybe<?> source) {
			return Mono.from(source.toFlowable());
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Maybe} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2MaybeToFluxConverter implements Converter<io.reactivex.Maybe<?>, Flux<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flux<?> convert(io.reactivex.Maybe<?> source) {
			return Flux.from(source.toFlowable());
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2ObservableToSingleConverter
			implements Converter<io.reactivex.Observable<?>, io.reactivex.Single<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Single<?> convert(io.reactivex.Observable<?> source) {
			return source.singleOrError();
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Maybe}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2ObservableToMaybeConverter
			implements Converter<io.reactivex.Observable<?>, io.reactivex.Maybe<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Maybe<?> convert(io.reactivex.Observable<?> source) {
			return source.singleElement();
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	private enum RxJava2SingleToObservableConverter
			implements Converter<io.reactivex.Single<?>, io.reactivex.Observable<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public io.reactivex.Observable<?> convert(io.reactivex.Single<?> source) {
			return source.toObservable();
		}
	}

	/**
	 * Holder for delayed initialization of {@link ReactiveAdapterRegistry}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	static class RegistryHolder {

		static final @Nullable ReactiveAdapterRegistry REACTIVE_ADAPTER_REGISTRY;

		static {

			if (ReactiveWrappers.isAvailable(ReactiveLibrary.PROJECT_REACTOR)) {
				REACTIVE_ADAPTER_REGISTRY = new ReactiveAdapterRegistry();
			} else {
				REACTIVE_ADAPTER_REGISTRY = null;
			}
		}
	}
}
