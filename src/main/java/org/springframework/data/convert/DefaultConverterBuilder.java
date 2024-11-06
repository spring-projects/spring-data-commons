/*
 * Copyright 2017-2024 the original author or authors.
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
package org.springframework.data.convert;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.convert.ConverterBuilder.ReadingConverterBuilder;
import org.springframework.data.convert.ConverterBuilder.WritingConverterBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Builder to easily set up (bidirectional) {@link Converter} instances for Spring Data type mapping using Lambdas. Use
 * factory methods on {@link ConverterBuilder} to create instances of this class.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 2.0
 * @see ConverterBuilder#writing(Class, Class, Function)
 * @see ConverterBuilder#reading(Class, Class, Function)
 * @soundtrack John Mayer - Still Feel Like Your Man (The Search for Everything)
 */
record DefaultConverterBuilder<S, T> (ConvertiblePair convertiblePair,
		@Nullable Function<? super S, ? extends T> writing, @Nullable Function<? super T, ? extends S> reading)
		implements ConverterAware, ReadingConverterBuilder<T, S>, WritingConverterBuilder<S, T> {

	@Override
	public ConverterAware andReading(Function<? super T, ? extends S> function) {
		return withReading(function);
	}

	@Override
	public ConverterAware andWriting(Function<? super S, ? extends T> function) {
		return withWriting(function);
	}

	@Override
	public GenericConverter getReadingConverter() {

		GenericConverter converter = getNullableReadingConverter();

		if (converter == null) {
			throw new IllegalStateException("No reading converter specified");
		}

		return converter;
	}

	@Override
	public GenericConverter getWritingConverter() {

		GenericConverter converter = getNullableWritingConverter();

		if (converter == null) {
			throw new IllegalStateException("No writing converter specified");
		}

		return converter;
	}

	@Override
	public Set<GenericConverter> getConverters() {

		GenericConverter reading = getNullableReadingConverter();
		GenericConverter writing = getNullableWritingConverter();

		if (reading == null && writing == null) {
			return Collections.emptySet();
		}

		if (reading == null) {
			return Set.of(writing);
		}

		if (writing == null) {
			return Set.of(reading);
		}

		return Set.of(reading, writing);
	}

	@Nullable
	private GenericConverter getNullableReadingConverter() {
		return reading != null ? new ConfigurableGenericConverter.Reading<>(convertiblePair, reading) : null;
	}

	@Nullable
	private GenericConverter getNullableWritingConverter() {
		return writing != null ? new ConfigurableGenericConverter.Writing<>(invertedPair(), writing) : null;
	}

	private ConvertiblePair invertedPair() {
		return new ConvertiblePair(convertiblePair.getTargetType(), convertiblePair.getSourceType());
	}

	DefaultConverterBuilder<S, T> withWriting(Function<? super S, ? extends T> writing) {
		return this.writing == writing ? this
				: new DefaultConverterBuilder<S, T>(this.convertiblePair, writing, this.reading);
	}

	DefaultConverterBuilder<S, T> withReading(Function<? super T, ? extends S> reading) {
		return this.reading == reading ? this
				: new DefaultConverterBuilder<S, T>(this.convertiblePair, this.writing, reading);
	}

	private static class ConfigurableGenericConverter<S, T> implements GenericConverter {

		private final ConvertiblePair convertiblePair;
		private final Function<? super S, ? extends T> function;

		public ConfigurableGenericConverter(ConvertiblePair convertiblePair, Function<? super S, ? extends T> function) {
			this.convertiblePair = convertiblePair;
			this.function = function;
		}

		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return function.apply((S) source);
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(convertiblePair);
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof ConfigurableGenericConverter<?, ?> that)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(convertiblePair, that.convertiblePair)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(function, that.function);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(convertiblePair);
			result = 31 * result + ObjectUtils.nullSafeHashCode(function);
			return result;
		}

		@WritingConverter
		private static class Writing<S, T> extends ConfigurableGenericConverter<S, T> {

			Writing(ConvertiblePair convertiblePair, Function<? super S, ? extends T> function) {
				super(convertiblePair, function);
			}
		}

		@ReadingConverter
		private static class Reading<S, T> extends ConfigurableGenericConverter<S, T> {

			Reading(ConvertiblePair convertiblePair, Function<? super S, ? extends T> function) {
				super(convertiblePair, function);
			}
		}
	}
}
