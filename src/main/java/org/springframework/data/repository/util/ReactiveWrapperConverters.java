/*
 * Copyright 2016-2020 the original author or authors.
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

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
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

			conversionService.addConverter(PublisherToMonoConverter.INSTANCE);
			conversionService.addConverter(PublisherToFluxConverter.INSTANCE);

			if (ReactiveWrappers.isAvailable(ReactiveLibrary.KOTLIN_COROUTINES)) {
				conversionService.addConverter(PublisherToFlowConverter.INSTANCE);
			}

			if (RegistryHolder.REACTIVE_ADAPTER_REGISTRY != null) {
				conversionService.addConverterFactory(ReactiveAdapterConverterFactory.INSTANCE);
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
	// Coroutine converters
	// -------------------------------------------------------------------------

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Flow}.
	 *
	 * @author Mark Paluch
	 * @author 2.3
	 */
	private enum PublisherToFlowConverter implements Converter<Publisher<?>, Flow<?>> {

		INSTANCE;

		@Nonnull
		@Override
		public Flow<?> convert(Publisher<?> source) {
			return ReactiveFlowKt.asFlow(source);
		}
	}

	/**
	 * A {@link ConverterFactory} that adapts between reactive types using {@link ReactiveAdapterRegistry}.
	 */
	private enum ReactiveAdapterConverterFactory implements ConverterFactory<Object, Object>, ConditionalConverter {

		INSTANCE;

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return isSupported(sourceType) || isSupported(targetType);
		}

		private boolean isSupported(TypeDescriptor typeDescriptor) {
			return RegistryHolder.REACTIVE_ADAPTER_REGISTRY != null
					&& RegistryHolder.REACTIVE_ADAPTER_REGISTRY.getAdapter(typeDescriptor.getType()) != null;
		}

		@Override
		@SuppressWarnings({ "ConstantConditions", "unchecked" })
		public <T> Converter<Object, T> getConverter(Class<T> targetType) {
			return source -> {

				Publisher<?> publisher = source instanceof Publisher ? (Publisher<?>) source
						: RegistryHolder.REACTIVE_ADAPTER_REGISTRY.getAdapter(Publisher.class, source).toPublisher(source);

				ReactiveAdapter adapter = RegistryHolder.REACTIVE_ADAPTER_REGISTRY.getAdapter(targetType);

				return (T) adapter.fromPublisher(publisher);
			};
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
