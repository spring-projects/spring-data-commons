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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import com.google.common.base.Optional;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;
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
 * </ul>
 * Also allows to register custom converters using {@link ServiceLoader} of {@link NullableWrapperConverter}'s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Darek Kaczynski
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

	private static final List<NullableWrapperConverter> CONVERTERS = new ArrayList<NullableWrapperConverter>();
	private static final List<NullableUnwrappingAdapter> UNWRAPPERS = new ArrayList<NullableUnwrappingAdapter>();

	static {
		CONVERTERS.add(NullableWrapperToFutureConverter.INSTANCE);

		if (GUAVA_PRESENT) {
			CONVERTERS.add(NullableWrapperToGuavaOptionalConverter.INSTANCE);
		}

		if (JDK_8_PRESENT) {
			CONVERTERS.add(NullableWrapperToJdk8OptionalConverter.INSTANCE);
		}

		if (JDK_8_PRESENT && SPRING_4_2_PRESENT) {
			CONVERTERS.add(NullableWrapperToCompletableFutureConverter.INSTANCE);
		}

		if (SCALA_PRESENT) {
			CONVERTERS.add(NullableWrapperToScalaOptionConverter.INSTANCE);
		}

		Iterator<NullableWrapperConverter> services = ServiceLoader
				.load(NullableWrapperConverter.class, Thread.currentThread().getContextClassLoader()).iterator();

		while (services.hasNext()) {
			CONVERTERS.add(services.next());
		}

		for (NullableWrapperConverter converter : CONVERTERS) {
			UNWRAPPERS.add(new NullableUnwrappingAdapter(converter));
		}
	}

	private QueryExecutionConverters() {
	}

	/**
	 * Returns whether the given type is a supported wrapper type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supports(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		for (NullableWrapperConverter converter : CONVERTERS) {
			for (Class<?> candidate : converter.getWrapperTypes()) {
				if (candidate.isAssignableFrom(type)) {
					return true;
				}
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

		for (NullableWrapperConverter converter : CONVERTERS) {
			conversionService.addConverter(new NullableWrappingAdapter(converter, conversionService));
		}
	}

	/**
	 * Unwraps the given source value in case it's one of the currently supported wrapper types detected at runtime.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	public static Object unwrap(Object source) {

		if (source == null) {
			return null;
		}

		for (NullableUnwrappingAdapter converter : UNWRAPPERS) {
			if (converter.supports(source.getClass())) {
				return converter.convert(source);
			}
		}

		return source;
	}

	/**
	 * Adapter for converters that creates instances of wrapper types such as Google Guava's and JDK 8's
	 * {@code Optional} types.
	 *
	 * @author Darek Kaczynski
	 */
	private static class NullableWrappingAdapter implements GenericConverter {

		private final NullableWrapperConverter converter;
		private final ConversionService conversionService;

		protected NullableWrappingAdapter(NullableWrapperConverter converter, ConversionService conversionService) {
			this.converter = converter;
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Class<?>[] wrapperTypes = converter.getWrapperTypes();

			Set<ConvertiblePair> pairs = new HashSet<ConvertiblePair>(wrapperTypes.length);

			for (Class<?> wrapperType : wrapperTypes) {
				pairs.add(new ConvertiblePair(NullableWrapper.class, wrapperType));
			}

			return Collections.unmodifiableSet(pairs);
		}

		@Override
		public final Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			NullableWrapper wrapper = (NullableWrapper) source;
			Object value = wrapper.getValue();

			// TODO: Add Recursive conversion once we move to Spring 4
			return value == null ? converter.getNullValue() : converter.wrap(value, conversionService);
		}
	}

	/**
	 * Adapter for converters that unwraps instances of wrapper types such as Google Guava's and JDK 8's
	 * {@code Optional} types.
	 *
	 * @author Darek Kaczynski
	 */
	private static class NullableUnwrappingAdapter implements Converter<Object, Object> {

		private final NullableWrapperConverter converter;

		public NullableUnwrappingAdapter(NullableWrapperConverter converter) {
			this.converter = converter;
		}

		public boolean supports(Class<?> type) {
			if (!converter.canUnwrap()) {
				return false;
			}

			for (Class<?> candidate : converter.getWrapperTypes()) {
				if (candidate.isAssignableFrom(type)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public Object convert(Object source) {
			return converter.unwrap(source);
		}
	}

	/**
	 * A Spring {@link NullableWrapperConverter} to support Google Guava's {@link Optional}.
	 *
	 * @author Oliver Gierke
	 * @author Darek Kaczynski
	 */
	private static enum NullableWrapperToGuavaOptionalConverter implements NullableWrapperConverter {

		INSTANCE;

		@Override
		public Class<?>[] getWrapperTypes() {
			return new Class<?>[]{Optional.class};
		}

		@Override
		public Object getNullValue() {
			return Optional.absent();
		}

		@Override
		public boolean canUnwrap() {
			return true;
		}

		@Override
		public Object wrap(Object source, ConversionService conversionService) {
			return Optional.of(source);
		}

		@Override
		public Object unwrap(Object source) {
			return ((Optional<?>) source).orNull();
		}
	}

	/**
	 * A Spring {@link NullableWrapperConverter} to support JDK 8's {@link java.util.Optional}.
	 *
	 * @author Oliver Gierke
	 * @author Darek Kaczynski
	 */
	private static enum NullableWrapperToJdk8OptionalConverter implements NullableWrapperConverter {

		INSTANCE;

		@Override
		public Class<?>[] getWrapperTypes() {
			return new Class<?>[]{java.util.Optional.class};
		}

		@Override
		public Object getNullValue() {
			return java.util.Optional.empty();
		}

		@Override
		public boolean canUnwrap() {
			return true;
		}

		@Override
		public Object wrap(Object source, ConversionService conversionService) {
			return java.util.Optional.of(source);
		}

		@Override
		public Object unwrap(Object source) {
			return ((java.util.Optional<?>) source).orElse(null);
		}
	}

	/**
	 * A Spring {@link NullableWrapperConverter} to support returning {@link Future} instances from repository methods.
	 *
	 * @author Oliver Gierke
	 * @author Darek Kaczynski
	 */
	private static enum NullableWrapperToFutureConverter implements NullableWrapperConverter {

		INSTANCE;

		@Override
		public Class<?>[] getWrapperTypes() {
			return new Class<?>[]{Future.class, ListenableFuture.class};
		}

		@Override
		public Object getNullValue() {
			return new AsyncResult<Object>(null);
		}

		@Override
		public boolean canUnwrap() {
			return false;
		}

		@Override
		public Object wrap(Object source, ConversionService conversionService) {
			return new AsyncResult<Object>(source);
		}

		@Override
		public Object unwrap(Object source) {
			throw new UnsupportedOperationException("Cannot unwrap Future.");
		}
	}

	/**
	 * A Spring {@link NullableWrapperConverter} to support returning {@link CompletableFuture} instances from repository methods.
	 *
	 * @author Oliver Gierke
	 * @author Darek Kaczynski
	 */
	private static enum NullableWrapperToCompletableFutureConverter implements NullableWrapperConverter {

		INSTANCE;

		@Override
		public Class<?>[] getWrapperTypes() {
			return new Class<?>[]{CompletableFuture.class};
		}

		@Override
		public Object getNullValue() {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public boolean canUnwrap() {
			return false;
		}

		@Override
		public Object wrap(Object source, ConversionService conversionService) {
			return CompletableFuture.completedFuture(source);
		}

		@Override
		public Object unwrap(Object source) {
			throw new UnsupportedOperationException("Cannot unwrap CompletableFuture.");
		}
	}

	/**
	 * A Spring {@link NullableWrapperConverter} to support Scala's {@link Option}.
	 *
	 * @author Oliver Gierke
	 * @author Darek Kaczynski
	 * @since 1.13
	 */
	private static enum NullableWrapperToScalaOptionConverter implements NullableWrapperConverter {

		INSTANCE;

		private final Function0<Object> alternative = new AbstractFunction0<Object>() {
			@Override
			public Option<Object> apply() {
				return null;
			}
		};

		@Override
		public Class<?>[] getWrapperTypes() {
			return new Class<?>[]{Option.class};
		}

		@Override
		public Object getNullValue() {
			return Option.empty();
		}

		@Override
		public boolean canUnwrap() {
			return true;
		}

		@Override
		public Object wrap(Object source, ConversionService conversionService) {
			return Option.apply(source);
		}

		@Override
		public Object unwrap(Object source) {
			return ((Option<?>) source).getOrElse(alternative);
		}
	}
}
