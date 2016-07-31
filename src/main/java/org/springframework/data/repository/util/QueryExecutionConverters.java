/*
 * Copyright 2014-2016 the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.common.base.Optional;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;
import scala.Function0;
import scala.Option;
import scala.runtime.AbstractFunction0;

/**
 * Converters to potentially wrap the execution of a repository method into a variety of wrapper types potentially being
 * available on the classpath. Currently supported:
 * <ul>
 * <li>{@code java.util.Optional}</li>
 * <li>{@code com.google.common.base.Optional}</li>
 * <li>{@code scala.Option}</li>
 * <li>{@code java.util.concurrent.Future}</li>
 * <li>{@code java.util.concurrent.CompletableFuture}</li>
 * <li>{@code org.springframework.util.concurrent.ListenableFuture<}</li>
 * <li>Reactive wrappers supported by {@link ReactiveWrappers}</li>
 * </ul>
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.8
 * @see ReactiveWrappers
 */
public abstract class QueryExecutionConverters {

	private static final boolean SPRING_4_2_PRESENT = ClassUtils.isPresent(
			"org.springframework.core.annotation.AnnotationConfigurationException",
			QueryExecutionConverters.class.getClassLoader());

	private static final boolean ASYNC_RESULT_PRESENT = ClassUtils.isPresent(
			"org.springframework.scheduling.annotation.AsyncResult", QueryExecutionConverters.class.getClassLoader());

	private static final boolean GUAVA_PRESENT = ClassUtils.isPresent("com.google.common.base.Optional",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean JDK_8_PRESENT = ClassUtils.isPresent("java.util.Optional",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean SCALA_PRESENT = ClassUtils.isPresent("scala.Option",
			QueryExecutionConverters.class.getClassLoader());

	private static final Set<Class<?>> WRAPPER_TYPES = new HashSet<Class<?>>();
	private static final Set<Class<?>> UNWRAPPER_TYPES = new HashSet<Class<?>>();
	private static final Set<Converter<Object, Object>> UNWRAPPERS = new HashSet<Converter<Object, Object>>();
	private static final ReactiveAdapterRegistry REACTIVE_ADAPTER_REGISTRY = new ReactiveAdapterRegistry();

	static {

		WRAPPER_TYPES.add(Future.class);
		UNWRAPPER_TYPES.add(Future.class);
		WRAPPER_TYPES.add(ListenableFuture.class);
		UNWRAPPER_TYPES.add(ListenableFuture.class);

		if (GUAVA_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToGuavaOptionalConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToGuavaOptionalConverter.getWrapperType());
			UNWRAPPERS.add(GuavaOptionalUnwrapper.INSTANCE);
		}

		if (JDK_8_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToJdk8OptionalConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToJdk8OptionalConverter.getWrapperType());
			UNWRAPPERS.add(Jdk8OptionalUnwrapper.INSTANCE);
		}

		if (JDK_8_PRESENT && SPRING_4_2_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToCompletableFutureConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToCompletableFutureConverter.getWrapperType());
		}

		if (SCALA_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToScalaOptionConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToScalaOptionConverter.getWrapperType());
			UNWRAPPERS.add(ScalOptionUnwrapper.INSTANCE);
		}

		WRAPPER_TYPES.addAll(ReactiveWrappers.getNoValueTypes());
		WRAPPER_TYPES.addAll(ReactiveWrappers.getSingleValueTypes());
		WRAPPER_TYPES.addAll(ReactiveWrappers.getMultiValueTypes());
	}

	private QueryExecutionConverters() {}

	/**
	 * Returns whether the given type is a supported wrapper type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supports(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		for (Class<?> candidate : WRAPPER_TYPES) {
			if (candidate.isAssignableFrom(type)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns whether the given wrapper type supports unwrapping.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supportsUnwrapping(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		for (Class<?> candidate : UNWRAPPER_TYPES) {
			if (candidate.isAssignableFrom(type)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Registers converters for wrapper types found on the classpath.
	 * 
	 * @param conversionService must not be {@literal null}.
	 */
	public static void registerConvertersIn(ConfigurableConversionService conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		if (GUAVA_PRESENT) {
			conversionService.addConverter(new NullableWrapperToGuavaOptionalConverter(conversionService));
		}

		if (JDK_8_PRESENT) {
			conversionService.addConverter(new NullableWrapperToJdk8OptionalConverter(conversionService));
			conversionService.addConverter(new NullableWrapperToCompletableFutureConverter(conversionService));
		}

		if (SCALA_PRESENT) {
			conversionService.addConverter(new NullableWrapperToScalaOptionConverter(conversionService));
		}

		if (ASYNC_RESULT_PRESENT) {
			conversionService.addConverter(new NullableWrapperToFutureConverter(conversionService));
		}

		if (ReactiveWrappers.isAvailable()) {

			if (ReactiveWrappers.RXJAVA1_PRESENT) {

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

			if (ReactiveWrappers.RXJAVA2_PRESENT) {

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

			if (ReactiveWrappers.PROJECT_REACTOR_PRESENT) {
				conversionService.addConverter(PublisherToMonoConverter.INSTANCE);
				conversionService.addConverter(PublisherToFluxConverter.INSTANCE);
			}

			if (ReactiveWrappers.RXJAVA1_PRESENT) {
				conversionService.addConverter(RxJava1SingleToObservableConverter.INSTANCE);
				conversionService.addConverter(RxJava1ObservableToSingleConverter.INSTANCE);
			}

			if (ReactiveWrappers.RXJAVA2_PRESENT) {
				conversionService.addConverter(RxJava2SingleToObservableConverter.INSTANCE);
				conversionService.addConverter(RxJava2ObservableToSingleConverter.INSTANCE);
			}
		}
	}

	/**
	 * Unwraps the given source value in case it's one of the currently supported wrapper types detected at runtime.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	public static Object unwrap(Object source) {

		if (source == null || !supports(source.getClass())) {
			return source;
		}

		for (Converter<Object, Object> converter : UNWRAPPERS) {

			Object result = converter.convert(source);

			if (result != source) {
				return result;
			}
		}

		return source;
	}

	/**
	 * Base class for converters that create instances of wrapper types such as Google Guava's and JDK 8's
	 * {@code Optional} types.
	 *
	 * @author Oliver Gierke
	 */
	private static abstract class AbstractWrapperTypeConverter implements GenericConverter {

		@SuppressWarnings("unused") //
		private final ConversionService conversionService;
		private final Class<?>[] wrapperTypes;
		private final Object nullValue;

		/**
		 * Creates a new {@link AbstractWrapperTypeConverter} using the given {@link ConversionService} and wrapper type.
		 * 
		 * @param conversionService must not be {@literal null}.
		 * @param wrapperTypes must not be {@literal null}.
		 */
		protected AbstractWrapperTypeConverter(ConversionService conversionService, Object nullValue,
				Class<?>... wrapperTypes) {

			Assert.notNull(conversionService, "ConversionService must not be null!");
			Assert.notEmpty(wrapperTypes, "Wrapper type must not be empty!");

			this.conversionService = conversionService;
			this.wrapperTypes = wrapperTypes;
			this.nullValue = nullValue;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {

			Set<ConvertiblePair> pairs = new HashSet<ConvertiblePair>(wrapperTypes.length);

			for (Class<?> wrapperType : wrapperTypes) {
				pairs.add(new ConvertiblePair(NullableWrapper.class, wrapperType));
			}

			return Collections.unmodifiableSet(pairs);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Override
		public final Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			NullableWrapper wrapper = (NullableWrapper) source;
			Object value = wrapper.getValue();

			// TODO: Add Recursive conversion once we move to Spring 4
			return value == null ? nullValue : wrap(value);
		}

		/**
		 * Wrap the given, non-{@literal null} value into the wrapper type.
		 * 
		 * @param source will never be {@literal null}.
		 * @return must not be {@literal null}.
		 */
		protected abstract Object wrap(Object source);
	}

	/**
	 * A Spring {@link Converter} to support Google Guava's {@link Optional}.
	 * 
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToGuavaOptionalConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToGuavaOptionalConverter} using the given {@link ConversionService}.
		 * 
		 * @param conversionService must not be {@literal null}.
		 */
		public NullableWrapperToGuavaOptionalConverter(ConversionService conversionService) {
			super(conversionService, Optional.absent(), Optional.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return Optional.of(source);
		}

		public static Class<?> getWrapperType() {
			return Optional.class;
		}
	}

	/**
	 * A Spring {@link Converter} to support JDK 8's {@link java.util.Optional}.
	 * 
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToJdk8OptionalConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToJdk8OptionalConverter} using the given {@link ConversionService}.
		 * 
		 * @param conversionService must not be {@literal null}.
		 */
		public NullableWrapperToJdk8OptionalConverter(ConversionService conversionService) {
			super(conversionService, java.util.Optional.empty(), java.util.Optional.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return java.util.Optional.of(source);
		}

		public static Class<?> getWrapperType() {
			return java.util.Optional.class;
		}
	}

	/**
	 * A Spring {@link Converter} to support returning {@link Future} instances from repository methods.
	 * 
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToFutureConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToFutureConverter} using the given {@link ConversionService}.
		 * 
		 * @param conversionService must not be {@literal null}.
		 */
		public NullableWrapperToFutureConverter(ConversionService conversionService) {
			super(conversionService, new AsyncResult<Object>(null), Future.class, ListenableFuture.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return new AsyncResult<Object>(source);
		}
	}

	/**
	 * A Spring {@link Converter} to support returning {@link CompletableFuture} instances from repository methods.
	 * 
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToCompletableFutureConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToCompletableFutureConverter} using the given {@link ConversionService}.
		 * 
		 * @param conversionService must not be {@literal null}.
		 */
		public NullableWrapperToCompletableFutureConverter(ConversionService conversionService) {
			super(conversionService, CompletableFuture.completedFuture(null), CompletableFuture.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return source instanceof CompletableFuture ? source : CompletableFuture.completedFuture(source);
		}

		public static Class<?> getWrapperType() {
			return CompletableFuture.class;
		}
	}

	/**
	 * A Spring {@link Converter} to support Scala's {@link Option}.
	 *
	 * @author Oliver Gierke
	 * @since 1.13
	 */
	private static class NullableWrapperToScalaOptionConverter extends AbstractWrapperTypeConverter {

		public NullableWrapperToScalaOptionConverter(ConversionService conversionService) {
			super(conversionService, Option.empty(), Option.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return Option.apply(source);
		}

		public static Class<?> getWrapperType() {
			return Option.class;
		}
	}

	/**
	 * A {@link Converter} to unwrap Guava {@link Optional} instances.
	 *
	 * @author Oliver Gierke
	 * @since 1.12
	 */
	private static enum GuavaOptionalUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {
			return source instanceof Optional ? ((Optional<?>) source).orNull() : source;
		}
	}

	/**
	 * A {@link Converter} to unwrap JDK 8 {@link java.util.Optional} instances.
	 *
	 * @author Oliver Gierke
	 * @since 1.12
	 */
	private static enum Jdk8OptionalUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {
			return source instanceof java.util.Optional ? ((java.util.Optional<?>) source).orElse(null) : source;
		}
	}

	/**
	 * A {@link Converter} to unwrap a Scala {@link Option} instance.
	 *
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 * @author 1.13
	 */
	private static enum ScalOptionUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		private final Function0<Object> alternative = new AbstractFunction0<Object>() {

			/*
			 * (non-Javadoc)
			 * @see scala.Function0#apply()
			 */
			@Override
			public Option<Object> apply() {
				return null;
			}
		};

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {
			return source instanceof Option ? ((Option<?>) source).getOrElse(alternative) : source;
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToFluxConverter implements Converter<Publisher<?>, Flux<?>> {

		INSTANCE;

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
	public enum PublisherToMonoConverter implements Converter<Publisher<?>, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(Publisher<?> source) {
			return Mono.from(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToRxJava1SingleConverter implements Converter<Publisher<?>, Single<?>> {

		INSTANCE;

		@Override
		public Single<?> convert(Publisher<?> source) {
			return (Single<?>) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(Single.class).fromPublisher(Mono.from(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Completable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToRxJava1CompletableConverter implements Converter<Publisher<?>, Completable> {

		INSTANCE;

		@Override
		public Completable convert(Publisher<?> source) {
			return (Completable) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(Completable.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Observable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToRxJava1ObservableConverter implements Converter<Publisher<?>, Observable<?>> {

		INSTANCE;

		@Override
		public Observable<?> convert(Publisher<?> source) {
			return (Observable<?>) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(Observable.class).fromPublisher(Flux.from(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1SingleToPublisherConverter implements Converter<Single<?>, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(Single<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Single.class).toPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1SingleToMonoConverter implements Converter<Single<?>, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(Single<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Single.class).toMono(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1SingleToFluxConverter implements Converter<Single<?>, Flux<?>> {

		INSTANCE;

		@Override
		public Flux<?> convert(Single<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Single.class).toFlux(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Completable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1CompletableToPublisherConverter implements Converter<Completable, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(Completable source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Completable.class).toFlux(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Completable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1CompletableToMonoConverter implements Converter<Completable, Mono<?>> {

		INSTANCE;

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
	public enum RxJava1ObservableToPublisherConverter implements Converter<Observable<?>, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(Observable<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Observable.class).toFlux(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1ObservableToMonoConverter implements Converter<Observable<?>, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(Observable<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Observable.class).toMono(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava1ObservableToFluxConverter implements Converter<Observable<?>, Flux<?>> {

		INSTANCE;

		@Override
		public Flux<?> convert(Observable<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(Observable.class).toFlux(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToRxJava2SingleConverter implements Converter<Publisher<?>, io.reactivex.Single<?>> {

		INSTANCE;

		@Override
		public io.reactivex.Single<?> convert(Publisher<?> source) {
			return (io.reactivex.Single<?>) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(io.reactivex.Single.class)
					.fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Completable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToRxJava2CompletableConverter implements Converter<Publisher<?>, io.reactivex.Completable> {

		INSTANCE;

		@Override
		public io.reactivex.Completable convert(Publisher<?> source) {
			return (io.reactivex.Completable) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(io.reactivex.Completable.class)
					.fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link io.reactivex.Observable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToRxJava2ObservableConverter implements Converter<Publisher<?>, io.reactivex.Observable<?>> {

		INSTANCE;

		@Override
		public io.reactivex.Observable<?> convert(Publisher<?> source) {
			return (io.reactivex.Observable<?>) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(io.reactivex.Single.class)
					.fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2SingleToPublisherConverter implements Converter<io.reactivex.Single<?>, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(io.reactivex.Single<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(io.reactivex.Single.class).toMono(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Single} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2SingleToMonoConverter implements Converter<io.reactivex.Single<?>, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(io.reactivex.Single<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(io.reactivex.Single.class).toMono(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2SingleToFluxConverter implements Converter<io.reactivex.Single<?>, Flux<?>> {

		INSTANCE;

		@Override
		public Flux<?> convert(io.reactivex.Single<?> source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(io.reactivex.Single.class).toFlux(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Completable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2CompletableToPublisherConverter implements Converter<io.reactivex.Completable, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(io.reactivex.Completable source) {
			return REACTIVE_ADAPTER_REGISTRY.getAdapterFrom(io.reactivex.Completable.class).toFlux(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Completable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2CompletableToMonoConverter implements Converter<io.reactivex.Completable, Mono<?>> {

		INSTANCE;

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
	public enum RxJava2ObservableToPublisherConverter implements Converter<io.reactivex.Observable<?>, Publisher<?>> {

		INSTANCE;

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
	public enum RxJava2ObservableToMonoConverter implements Converter<io.reactivex.Observable<?>, Mono<?>> {

		INSTANCE;

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
	public enum RxJava2ObservableToFluxConverter implements Converter<io.reactivex.Observable<?>, Flux<?>> {

		INSTANCE;

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
	public enum PublisherToRxJava2FlowableConverter implements Converter<Publisher<?>, io.reactivex.Flowable<?>> {

		INSTANCE;

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
	public enum RxJava2FlowableToPublisherConverter implements Converter<io.reactivex.Flowable<?>, Publisher<?>> {

		INSTANCE;

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
	public enum PublisherToRxJava2MaybeConverter implements Converter<Publisher<?>, io.reactivex.Maybe<?>> {

		INSTANCE;

		@Override
		public io.reactivex.Maybe<?> convert(Publisher<?> source) {
			return (io.reactivex.Maybe<?>) REACTIVE_ADAPTER_REGISTRY.getAdapterTo(Maybe.class).fromPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link io.reactivex.Maybe} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2MaybeToPublisherConverter implements Converter<io.reactivex.Maybe<?>, Publisher<?>> {

		INSTANCE;

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
	public enum RxJava2MaybeToMonoConverter implements Converter<io.reactivex.Maybe<?>, Mono<?>> {

		INSTANCE;

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
	public enum RxJava2MaybeToFluxConverter implements Converter<io.reactivex.Maybe<?>, Flux<?>> {

		INSTANCE;

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
	public enum RxJava1ObservableToSingleConverter implements Converter<Observable<?>, Single<?>> {

		INSTANCE;

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
	public enum RxJava1SingleToObservableConverter implements Converter<Single<?>, Observable<?>> {

		INSTANCE;

		@Override
		public Observable<?> convert(Single<?> source) {
			return source.toObservable();
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2ObservableToSingleConverter
			implements Converter<io.reactivex.Observable<?>, io.reactivex.Single<?>> {

		INSTANCE;

		@Override
		public io.reactivex.Single<?> convert(io.reactivex.Observable<?> source) {
			return source.singleElement().toSingle();
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum RxJava2SingleToObservableConverter
			implements Converter<io.reactivex.Single<?>, io.reactivex.Observable<?>> {

		INSTANCE;

		@Override
		public io.reactivex.Observable<?> convert(io.reactivex.Single<?> source) {
			return source.toObservable();
		}
	}
}
