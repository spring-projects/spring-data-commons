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

import scala.Function0;
import scala.Option;
import scala.runtime.AbstractFunction0;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.reactivestreams.Publisher;
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

import reactor.core.converter.DependencyUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;
import scala.Option;

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
 * <li>{@code rx.Single}</li>
 * <li>{@code rx.Observable}</li>
 * <li>{@code rx.Completable}</li>
 * <li>{@code reactor.core.publisher.Mono}</li>
 * <li>{@code reactor.core.publisher.Flux}</li>
 * <li>{@code org.reactivestreams.Publisher}</li>
 * </ul>
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.8
 */
public abstract class QueryExecutionConverters {

	private static final boolean SPRING_4_2_PRESENT = ClassUtils.isPresent(
			"org.springframework.core.annotation.AnnotationConfigurationException",
			QueryExecutionConverters.class.getClassLoader());

	private static final boolean GUAVA_PRESENT = ClassUtils.isPresent("com.google.common.base.Optional",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean JDK_8_PRESENT = ClassUtils.isPresent("java.util.Optional",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean SCALA_PRESENT = ClassUtils.isPresent("scala.Option",
			QueryExecutionConverters.class.getClassLoader());

	private static final boolean PROJECT_REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.converter.DependencyUtils",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean RXJAVA_SINGLE_PRESENT = ClassUtils.isPresent("rx.Single",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean RXJAVA_OBSERVABLE_PRESENT = ClassUtils.isPresent("rx.Observable",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean RXJAVA_COMPLETABLE_PRESENT = ClassUtils.isPresent("rx.Completable",
			QueryExecutionConverters.class.getClassLoader());

	private static final Set<Class<?>> WRAPPER_TYPES = new HashSet<Class<?>>();
	private static final Set<Class<?>> UNWRAPPER_TYPES = new HashSet<Class<?>>();
	private static final Set<Converter<Object, Object>> UNWRAPPERS = new HashSet<Converter<Object, Object>>();

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

		if (PROJECT_REACTOR_PRESENT) {
			WRAPPER_TYPES.add(Publisher.class);
			WRAPPER_TYPES.add(Mono.class);
			WRAPPER_TYPES.add(Flux.class);
		}

		if (RXJAVA_SINGLE_PRESENT) {
			WRAPPER_TYPES.add(Single.class);
		}

		if (RXJAVA_COMPLETABLE_PRESENT) {
			WRAPPER_TYPES.add(Completable.class);
		}

		if (RXJAVA_OBSERVABLE_PRESENT) {
			WRAPPER_TYPES.add(Observable.class);
		}
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

		conversionService.addConverter(new NullableWrapperToFutureConverter(conversionService));

		if (PROJECT_REACTOR_PRESENT) {

			if (RXJAVA_COMPLETABLE_PRESENT) {
				conversionService.addConverter(PublisherToCompletableConverter.INSTANCE);
				conversionService.addConverter(CompletableToPublisherConverter.INSTANCE);
				conversionService.addConverter(CompletableToMonoConverter.INSTANCE);
			}

			if (RXJAVA_SINGLE_PRESENT) {
				conversionService.addConverter(PublisherToSingleConverter.INSTANCE);
				conversionService.addConverter(SingleToPublisherConverter.INSTANCE);
				conversionService.addConverter(SingleToMonoConverter.INSTANCE);
				conversionService.addConverter(SingleToFluxConverter.INSTANCE);
			}

			if (RXJAVA_OBSERVABLE_PRESENT) {
				conversionService.addConverter(PublisherToObservableConverter.INSTANCE);
				conversionService.addConverter(ObservableToPublisherConverter.INSTANCE);
				conversionService.addConverter(ObservableToMonoConverter.INSTANCE);
				conversionService.addConverter(ObservableToFluxConverter.INSTANCE);
			}

			conversionService.addConverter(PublisherToMonoConverter.INSTANCE);
			conversionService.addConverter(PublisherToFluxConverter.INSTANCE);
		}

		if (RXJAVA_SINGLE_PRESENT && RXJAVA_OBSERVABLE_PRESENT) {
			conversionService.addConverter(SingleToObservableConverter.INSTANCE);
			conversionService.addConverter(ObservableToSingleConverter.INSTANCE);
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
	public enum PublisherToSingleConverter implements Converter<Publisher<?>, Single<?>> {

		INSTANCE;

		@Override
		public Single<?> convert(Publisher<?> source) {
			return DependencyUtils.convertFromPublisher(source, Single.class);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Completable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToCompletableConverter implements Converter<Publisher<?>, Completable> {

		INSTANCE;

		@Override
		public Completable convert(Publisher<?> source) {
			return DependencyUtils.convertFromPublisher(source, Completable.class);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Publisher} to {@link Observable}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum PublisherToObservableConverter implements Converter<Publisher<?>, Observable<?>> {

		INSTANCE;

		@Override
		public Observable<?> convert(Publisher<?> source) {
			return DependencyUtils.convertFromPublisher(source, Observable.class);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum SingleToPublisherConverter implements Converter<Single<?>, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(Single<?> source) {
			return DependencyUtils.convertToPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum SingleToMonoConverter implements Converter<Single<?>, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(Single<?> source) {
			return PublisherToMonoConverter.INSTANCE.convert(DependencyUtils.convertToPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Single} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum SingleToFluxConverter implements Converter<Single<?>, Flux<?>> {

		INSTANCE;

		@Override
		public Flux<?> convert(Single<?> source) {
			return PublisherToFluxConverter.INSTANCE.convert(DependencyUtils.convertToPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Completable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum CompletableToPublisherConverter implements Converter<Completable, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(Completable source) {
			return DependencyUtils.convertToPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Completable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum CompletableToMonoConverter implements Converter<Completable, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(Completable source) {
			return Mono.from(CompletableToPublisherConverter.INSTANCE.convert(source));
		}
	}

	/**
	 * A {@link Converter} to convert an {@link Observable} to {@link Publisher}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum ObservableToPublisherConverter implements Converter<Observable<?>, Publisher<?>> {

		INSTANCE;

		@Override
		public Publisher<?> convert(Observable<?> source) {
			return DependencyUtils.convertToPublisher(source);
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Mono}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum ObservableToMonoConverter implements Converter<Observable<?>, Mono<?>> {

		INSTANCE;

		@Override
		public Mono<?> convert(Observable<?> source) {
			return PublisherToMonoConverter.INSTANCE.convert(DependencyUtils.convertToPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Flux}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum ObservableToFluxConverter implements Converter<Observable<?>, Flux<?>> {

		INSTANCE;

		@Override
		public Flux<?> convert(Observable<?> source) {
			return PublisherToFluxConverter.INSTANCE.convert(DependencyUtils.convertToPublisher(source));
		}
	}

	/**
	 * A {@link Converter} to convert a {@link Observable} to {@link Single}.
	 *
	 * @author Mark Paluch
	 * @author 2.0
	 */
	public enum ObservableToSingleConverter implements Converter<Observable<?>, Single<?>> {

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
	public enum SingleToObservableConverter implements Converter<Single<?>, Observable<?>> {

		INSTANCE;

		@Override
		public Observable<?> convert(Single<?> source) {
			return source.toObservable();
		}
	}
}
