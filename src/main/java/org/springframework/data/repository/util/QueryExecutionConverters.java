/*
 * Copyright 2014-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.util.NullableWrapper;
import org.springframework.data.util.NullableWrapperConverters;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Converters to potentially wrap the execution of a repository method into a variety of wrapper types potentially being
 * available on the classpath. Currently supported:
 * <ul>
 * <li>{@code java.util.concurrent.Future}</li>
 * <li>{@code java.util.concurrent.CompletableFuture}</li>
 * <li>{@code org.springframework.util.concurrent.ListenableFuture<}</li>
 * <li>{@code javaslang.collection.Seq}, {@code javaslang.collection.Map}, {@code javaslang.collection.Set} - as of
 * 1.13</li>
 * <li>{@code io.vavr.collection.Seq}, {@code io.vavr.collection.Map}, {@code io.vavr.collection.Set} - as of 2.0</li>
 * <li>Reactive wrappers supported by {@link ReactiveWrappers} - as of 2.0</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Maciek Opała
 * @author Jens Schauder
 * @since 1.8
 * @see NullableWrapperConverters
 */
public abstract class QueryExecutionConverters {

	private static final boolean VAVR_PRESENT = ClassUtils.isPresent("io.vavr.control.Try",
			QueryExecutionConverters.class.getClassLoader());

	private static final Set<WrapperType> WRAPPER_TYPES = new HashSet<>();
	private static final Set<WrapperType> UNWRAPPER_TYPES = new HashSet<WrapperType>();
	private static final Set<Converter<Object, Object>> UNWRAPPERS = new HashSet<>();
	private static final Set<Class<?>> ALLOWED_PAGEABLE_TYPES = new HashSet<>();
	private static final Map<Class<?>, ExecutionAdapter> EXECUTION_ADAPTER = new HashMap<>();
	private static final Map<Class<?>, Boolean> supportsCache = new ConcurrentReferenceHashMap<>();

	static {

		WRAPPER_TYPES.add(WrapperType.singleValue(Future.class));
		UNWRAPPER_TYPES.add(WrapperType.singleValue(Future.class));
		WRAPPER_TYPES.add(WrapperType.singleValue(ListenableFuture.class));
		UNWRAPPER_TYPES.add(WrapperType.singleValue(ListenableFuture.class));

		ALLOWED_PAGEABLE_TYPES.add(Slice.class);
		ALLOWED_PAGEABLE_TYPES.add(Page.class);
		ALLOWED_PAGEABLE_TYPES.add(List.class);

		WRAPPER_TYPES.add(NullableWrapperToCompletableFutureConverter.getWrapperType());

		if (VAVR_PRESENT) {

			WRAPPER_TYPES.add(VavrCollections.ToJavaConverter.INSTANCE.getWrapperType());
			UNWRAPPERS.add(VavrTraversableUnwrapper.INSTANCE);

			// Try support
			WRAPPER_TYPES.add(WrapperType.singleValue(io.vavr.control.Try.class));
			EXECUTION_ADAPTER.put(io.vavr.control.Try.class, it -> io.vavr.control.Try.of(it::get));

			ALLOWED_PAGEABLE_TYPES.add(io.vavr.collection.Seq.class);
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

		return supportsCache.computeIfAbsent(type, key -> {

			for (WrapperType candidate : WRAPPER_TYPES) {
				if (candidate.getType().isAssignableFrom(key)) {
					return true;
				}
			}

			return NullableWrapperConverters.supports(type);
		});
	}

	/**
	 * Returns whether the given wrapper type supports unwrapping.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supportsUnwrapping(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		if (NullableWrapperConverters.supportsUnwrapping(type)) {
			return NullableWrapperConverters.supportsUnwrapping(type);
		}

		for (WrapperType candidate : UNWRAPPER_TYPES) {
			if (candidate.getType().isAssignableFrom(type)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSingleValue(Class<?> type) {

		if (NullableWrapperConverters.supports(type)) {
			return NullableWrapperConverters.isSingleValue(type);
		}

		for (WrapperType candidate : WRAPPER_TYPES) {
			if (candidate.getType().isAssignableFrom(type)) {
				return candidate.isSingleValue();
			}
		}

		return false;
	}

	/**
	 * Returns the types that are supported on paginating query methods. Will include custom collection types of e.g.
	 * Vavr.
	 *
	 * @return
	 */
	public static Set<Class<?>> getAllowedPageableTypes() {
		return Collections.unmodifiableSet(ALLOWED_PAGEABLE_TYPES);
	}

	/**
	 * Registers converters for wrapper types found on the classpath.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public static void registerConvertersIn(ConfigurableConversionService conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		conversionService.removeConvertible(Collection.class, Object.class);

		NullableWrapperConverters.registerConvertersIn(conversionService);

		if (VAVR_PRESENT) {
			conversionService.addConverter(VavrCollections.FromJavaConverter.INSTANCE);
		}

		conversionService.addConverter(new NullableWrapperToCompletableFutureConverter());
		conversionService.addConverter(new NullableWrapperToFutureConverter());
		conversionService.addConverter(new IterableToStreamableConverter());
	}

	/**
	 * Unwraps the given source value in case it's one of the currently supported wrapper types detected at runtime.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	@Nullable
	public static Object unwrap(@Nullable Object source) {

		source = NullableWrapperConverters.unwrap(source);

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
	 * Recursively unwraps well known wrapper types from the given {@link TypeInformation}.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static TypeInformation<?> unwrapWrapperTypes(TypeInformation<?> type) {

		Assert.notNull(type, "type must not be null");

		Class<?> rawType = type.getType();

		boolean needToUnwrap = type.isCollectionLike() //
				|| Slice.class.isAssignableFrom(rawType) //
				|| GeoResults.class.isAssignableFrom(rawType) //
				|| rawType.isArray() //
				|| supports(rawType) //
				|| Stream.class.isAssignableFrom(rawType);

		return needToUnwrap ? unwrapWrapperTypes(type.getRequiredComponentType()) : type;
	}

	/**
	 * Returns the {@link ExecutionAdapter} to be used for the given return type.
	 *
	 * @param returnType must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	public static ExecutionAdapter getExecutionAdapter(Class<?> returnType) {

		Assert.notNull(returnType, "Return type must not be null!");

		return EXECUTION_ADAPTER.get(returnType);
	}

	public interface ThrowingSupplier {
		Object get() throws Throwable;
	}

	public interface ExecutionAdapter {
		Object apply(ThrowingSupplier supplier) throws Throwable;
	}

	/**
	 * Base class for converters that create instances of wrapper types such as Google Guava's and JDK 8's
	 * {@code Optional} types.
	 *
	 * @author Oliver Gierke
	 */
	private static abstract class AbstractWrapperTypeConverter implements GenericConverter {

		private final Object nullValue;
		private final Iterable<Class<?>> wrapperTypes;

		/**
		 * Creates a new {@link AbstractWrapperTypeConverter} using the given {@link ConversionService} and wrapper type.
		 *
		 * @param nullValue must not be {@literal null}.
		 */
		AbstractWrapperTypeConverter(Object nullValue) {

			Assert.notNull(nullValue, "Null value must not be null!");

			this.nullValue = nullValue;
			this.wrapperTypes = Collections.singleton(nullValue.getClass());
		}

		AbstractWrapperTypeConverter(Object nullValue,
				Iterable<Class<?>> wrapperTypes) {
			this.nullValue = nullValue;
			this.wrapperTypes = wrapperTypes;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {

			return Streamable.of(wrapperTypes)//
					.map(it -> new ConvertiblePair(NullableWrapper.class, it))//
					.stream().collect(StreamUtils.toUnmodifiableSet());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Nullable
		@Override
		public final Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (source == null) {
				return null;
			}

			org.springframework.data.util.NullableWrapper wrapper = (NullableWrapper) source;
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
	 * A Spring {@link Converter} to support returning {@link Future} instances from repository methods.
	 *
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToFutureConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToFutureConverter} using the given {@link ConversionService}.
		 */
		NullableWrapperToFutureConverter() {
			super(new AsyncResult<>(null), Arrays.asList(Future.class, ListenableFuture.class));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return new AsyncResult<>(source);
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
		 */
		NullableWrapperToCompletableFutureConverter() {
			super(CompletableFuture.completedFuture(null));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return source instanceof CompletableFuture ? source : CompletableFuture.completedFuture(source);
		}

		static WrapperType getWrapperType() {
			return WrapperType.singleValue(CompletableFuture.class);
		}
	}

	/**
	 * Converter to unwrap Vavr {@link io.vavr.collection.Traversable} instances.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	private enum VavrTraversableUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public Object convert(Object source) {

			if (source instanceof io.vavr.collection.Traversable) {
				return VavrCollections.ToJavaConverter.INSTANCE.convert(source);
			}

			return source;
		}
	}


	private static class IterableToStreamableConverter implements ConditionalGenericConverter {

		private static final TypeDescriptor STREAMABLE = TypeDescriptor.valueOf(Streamable.class);

		private final Map<TypeDescriptor, Boolean> targetTypeCache = new ConcurrentHashMap<>();
		private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

		IterableToStreamableConverter() {}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(Iterable.class, Object.class));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.ConditionalConverter#matches(org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (sourceType.isAssignableTo(targetType)) {
				return false;
			}

			if (!Iterable.class.isAssignableFrom(sourceType.getType())) {
				return false;
			}

			if (Streamable.class.equals(targetType.getType())) {
				return true;
			}

			return targetTypeCache.computeIfAbsent(targetType, it -> {
				return conversionService.canConvert(STREAMABLE, targetType);
			});
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@SuppressWarnings("unchecked")
		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			Streamable<Object> streamable = source == null //
					? Streamable.empty() //
					: Streamable.of(Iterable.class.cast(source));

			return Streamable.class.equals(targetType.getType()) //
					? streamable //
					: conversionService.convert(streamable, STREAMABLE, targetType);
		}
	}

	public static final class WrapperType {

		private WrapperType(Class<?> type, Cardinality cardinality) {
			this.type = type;
			this.cardinality = cardinality;
		}

		public Class<?> getType() {
			return this.type;
		}

		public Cardinality getCardinality() {
			return cardinality;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof WrapperType)) {
				return false;
			}

			WrapperType that = (WrapperType) o;

			if (!ObjectUtils.nullSafeEquals(type, that.type)) {
				return false;
			}

			return cardinality == that.cardinality;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(type);
			result = 31 * result + ObjectUtils.nullSafeHashCode(cardinality);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "QueryExecutionConverters.WrapperType(type=" + this.getType() + ", cardinality=" + this.getCardinality()
					+ ")";
		}

		enum Cardinality {
			NONE, SINGLE, MULTI;
		}

		private final Class<?> type;
		private final Cardinality cardinality;

		public static WrapperType singleValue(Class<?> type) {
			return new WrapperType(type, Cardinality.SINGLE);
		}

		public static WrapperType multiValue(Class<?> type) {
			return new WrapperType(type, Cardinality.MULTI);
		}

		public static WrapperType noValue(Class<?> type) {
			return new WrapperType(type, Cardinality.NONE);
		}

		boolean isSingleValue() {
			return cardinality.equals(Cardinality.SINGLE);
		}
	}
}
