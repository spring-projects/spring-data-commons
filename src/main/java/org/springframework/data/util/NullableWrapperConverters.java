/*
 * Copyright 2020-2021 the original author or authors.
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

import scala.Function0;
import scala.Option;
import scala.runtime.AbstractFunction0;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

import com.google.common.base.Optional;

/**
 * Converters to wrap and unwrap nullable wrapper types potentially being available on the classpath. Currently
 * supported:
 * <ul>
 * <li>{@code java.util.Optional}</li>
 * <li>{@code com.google.common.base.Optional}</li>
 * <li>{@code scala.Option}</li>
 * <li>{@code javaslang.control.Option}</li>
 * <li>{@code io.vavr.control.Option}</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Maciek Opała
 * @author Jens Schauder
 * @since 2.4
 */
public abstract class NullableWrapperConverters {

	private static final boolean GUAVA_PRESENT = ClassUtils.isPresent("com.google.common.base.Optional",
			NullableWrapperConverters.class.getClassLoader());
	private static final boolean SCALA_PRESENT = ClassUtils.isPresent("scala.Option",
			NullableWrapperConverters.class.getClassLoader());
	private static final boolean VAVR_PRESENT = ClassUtils.isPresent("io.vavr.control.Option",
			NullableWrapperConverters.class.getClassLoader());

	private static final Set<WrapperType> WRAPPER_TYPES = new HashSet<WrapperType>();
	private static final Set<WrapperType> UNWRAPPER_TYPES = new HashSet<WrapperType>();
	private static final Set<Converter<Object, Object>> UNWRAPPERS = new HashSet<Converter<Object, Object>>();
	private static final Map<Class<?>, Boolean> supportsCache = new ConcurrentReferenceHashMap<>();

	static {

		WRAPPER_TYPES.add(NullableWrapperToJdk8OptionalConverter.getWrapperType());
		UNWRAPPER_TYPES.add(NullableWrapperToJdk8OptionalConverter.getWrapperType());
		UNWRAPPERS.add(Jdk8OptionalUnwrapper.INSTANCE);

		if (GUAVA_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToGuavaOptionalConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToGuavaOptionalConverter.getWrapperType());
			UNWRAPPERS.add(GuavaOptionalUnwrapper.INSTANCE);
		}

		if (SCALA_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToScalaOptionConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToScalaOptionConverter.getWrapperType());
			UNWRAPPERS.add(ScalOptionUnwrapper.INSTANCE);
		}

		if (VAVR_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToVavrOptionConverter.getWrapperType());
			UNWRAPPERS.add(VavrOptionUnwrapper.INSTANCE);
		}
	}

	private NullableWrapperConverters() {}

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

			return false;
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

		for (WrapperType candidate : UNWRAPPER_TYPES) {
			if (candidate.getType().isAssignableFrom(type)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSingleValue(Class<?> type) {

		for (WrapperType candidate : WRAPPER_TYPES) {
			if (candidate.getType().isAssignableFrom(type)) {
				return candidate.isSingleValue();
			}
		}

		return false;
	}

	/**
	 * Registers converters for wrapper types found on the classpath.
	 *
	 * @param registry must not be {@literal null}.
	 */
	public static void registerConvertersIn(ConverterRegistry registry) {

		Assert.notNull(registry, "ConversionService must not be null!");

		registry.addConverter(NullableWrapperToJdk8OptionalConverter.INSTANCE);

		if (GUAVA_PRESENT) {
			registry.addConverter(NullableWrapperToGuavaOptionalConverter.INSTANCE);
		}

		if (SCALA_PRESENT) {
			registry.addConverter(NullableWrapperToScalaOptionConverter.INSTANCE);
		}

		if (VAVR_PRESENT) {
			registry.addConverter(NullableWrapperToVavrOptionConverter.INSTANCE);
		}
	}

	/**
	 * Unwraps the given source value in case it's one of the currently supported wrapper types detected at runtime.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	@Nullable
	public static Object unwrap(@Nullable Object source) {

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
	public static TypeInformation<?> unwrapActualType(TypeInformation<?> type) {

		Assert.notNull(type, "type must not be null");

		Class<?> rawType = type.getType();

		boolean needToUnwrap = supports(rawType) //
				|| Stream.class.isAssignableFrom(rawType);

		return needToUnwrap ? unwrapActualType(type.getRequiredComponentType()) : type;
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
		 * Creates a new {@link AbstractWrapperTypeConverter} using the given wrapper type.
		 *
		 * @param nullValue must not be {@literal null}.
		 */
		protected AbstractWrapperTypeConverter(Object nullValue) {

			Assert.notNull(nullValue, "Null value must not be null!");

			this.nullValue = nullValue;
			this.wrapperTypes = Collections.singleton(nullValue.getClass());
		}

		public AbstractWrapperTypeConverter(Object nullValue, Iterable<Class<?>> wrapperTypes) {
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

			NullableWrapper wrapper = (NullableWrapper) source;
			Object value = wrapper.getValue();

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
	 * A Spring {@link Converter} to support JDK 8's {@link java.util.Optional}.
	 *
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToJdk8OptionalConverter extends AbstractWrapperTypeConverter {

		public static final NullableWrapperToJdk8OptionalConverter INSTANCE = new NullableWrapperToJdk8OptionalConverter();

		private NullableWrapperToJdk8OptionalConverter() {
			super(java.util.Optional.empty());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return java.util.Optional.of(source);
		}

		public static WrapperType getWrapperType() {
			return WrapperType.singleValue(java.util.Optional.class);
		}
	}

	/**
	 * A Spring {@link Converter} to support Google Guava's {@link Optional}.
	 *
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToGuavaOptionalConverter extends AbstractWrapperTypeConverter {

		public static final NullableWrapperToGuavaOptionalConverter INSTANCE = new NullableWrapperToGuavaOptionalConverter();

		private NullableWrapperToGuavaOptionalConverter() {
			super(Optional.absent(), Collections.singleton(Optional.class));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return Optional.of(source);
		}

		public static WrapperType getWrapperType() {
			return WrapperType.singleValue(Optional.class);
		}

	}

	/**
	 * A Spring {@link Converter} to support Scala's {@link Option}.
	 *
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToScalaOptionConverter extends AbstractWrapperTypeConverter {

		public static final NullableWrapperToScalaOptionConverter INSTANCE = new NullableWrapperToScalaOptionConverter();

		private NullableWrapperToScalaOptionConverter() {
			super(Option.empty(), Collections.singleton(Option.class));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return Option.apply(source);
		}

		public static WrapperType getWrapperType() {
			return WrapperType.singleValue(Option.class);
		}
	}

	/**
	 * Converter to convert from {@link NullableWrapper} into JavaSlang's {@link io.vavr.control.Option}.
	 *
	 * @author Oliver Gierke
	 */
	private static class NullableWrapperToVavrOptionConverter extends AbstractWrapperTypeConverter {

		public static final NullableWrapperToVavrOptionConverter INSTANCE = new NullableWrapperToVavrOptionConverter();

		private NullableWrapperToVavrOptionConverter() {
			super(io.vavr.control.Option.none(), Collections.singleton(io.vavr.control.Option.class));
		}

		public static WrapperType getWrapperType() {
			return WrapperType.singleValue(io.vavr.control.Option.class);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.util.QueryExecutionConverters.AbstractWrapperTypeConverter#wrap(java.lang.Object)
		 */
		@Override
		protected Object wrap(Object source) {
			return io.vavr.control.Option.of(source);
		}
	}

	/**
	 * A {@link Converter} to unwrap Guava {@link Optional} instances.
	 *
	 * @author Oliver Gierke
	 */
	private enum GuavaOptionalUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Nullable
		@Override
		public Object convert(Object source) {
			return source instanceof Optional ? ((Optional<?>) source).orNull() : source;
		}
	}

	/**
	 * A {@link Converter} to unwrap JDK 8 {@link java.util.Optional} instances.
	 *
	 * @author Oliver Gierke
	 */
	private enum Jdk8OptionalUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Nullable
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
	 * @since 1.12
	 */
	private enum ScalOptionUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		private final Function0<Object> alternative = new AbstractFunction0<Object>() {

			/*
			 * (non-Javadoc)
			 * @see scala.Function0#apply()
			 */
			@Nullable
			@Override
			public Option<Object> apply() {
				return null;
			}
		};

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Nullable
		@Override
		public Object convert(Object source) {
			return source instanceof Option ? ((Option<?>) source).getOrElse(alternative) : source;
		}
	}

	/**
	 * Converter to unwrap Vavr {@link io.vavr.control.Option} instances.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	private enum VavrOptionUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public Object convert(Object source) {

			if (source instanceof io.vavr.control.Option) {
				return ((io.vavr.control.Option<Object>) source).getOrElse(() -> null);
			}

			return source;
		}
	}

	private static final class WrapperType {

		private WrapperType(Class<?> type, Cardinality cardinality) {
			this.type = type;
			this.cardinality = cardinality;
		}

		Class<?> getType() {
			return this.type;
		}

		Cardinality getCardinality() {
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
			return "WrapperType(type=" + this.getType() + ", cardinality=" + this.getCardinality() + ")";
		}

		enum Cardinality {
			NONE, SINGLE, MULTI;
		}

		private final Class<?> type;
		private final Cardinality cardinality;

		static WrapperType singleValue(Class<?> type) {
			return new WrapperType(type, Cardinality.SINGLE);
		}

		static WrapperType multiValue(Class<?> type) {
			return new WrapperType(type, Cardinality.MULTI);
		}

		static WrapperType noValue(Class<?> type) {
			return new WrapperType(type, Cardinality.NONE);
		}

		boolean isSingleValue() {
			return cardinality.equals(Cardinality.SINGLE);
		}
	}
}
