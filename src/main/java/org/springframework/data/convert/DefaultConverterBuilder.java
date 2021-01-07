/*
 * Copyright 2017-2021 the original author or authors.
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.convert.ConverterBuilder.ReadingConverterBuilder;
import org.springframework.data.convert.ConverterBuilder.WritingConverterBuilder;
import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Builder to easily set up (bi-directional) {@link Converter} instances for Spring Data type mapping using Lambdas. Use
 * factory methods on {@link ConverterBuilder} to create instances of this class.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 2.0
 * @see ConverterBuilder#writing(Class, Class, Function)
 * @see ConverterBuilder#reading(Class, Class, Function)
 * @soundtrack John Mayer - Still Feel Like Your Man (The Search for Everything)
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class DefaultConverterBuilder<S, T>
		implements ConverterAware, ReadingConverterBuilder<T, S>, WritingConverterBuilder<S, T> {

	private final ConvertiblePair convertiblePair;
	private final Optional<Function<? super S, ? extends T>> writing;
	private final Optional<Function<? super T, ? extends S>> reading;

	DefaultConverterBuilder(ConvertiblePair convertiblePair, Optional<Function<? super S, ? extends T>> writing,
			Optional<Function<? super T, ? extends S>> reading) {
		this.convertiblePair = convertiblePair;
		this.writing = writing;
		this.reading = reading;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.WritingConverterBuilder#andReading(java.util.function.Function)
	 */
	@Override
	public ConverterAware andReading(Function<? super T, ? extends S> function) {
		return withReading(Optional.of(function));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.ReadingConverterBuilder#andWriting(java.util.function.Function)
	 */
	@Override
	public ConverterAware andWriting(Function<? super S, ? extends T> function) {
		return withWriting(Optional.of(function));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.ReadingConverterBuilder#getRequiredReadingConverter()
	 */
	@Override
	public GenericConverter getReadingConverter() {
		return getOptionalReadingConverter()
				.orElseThrow(() -> new IllegalStateException("No reading converter specified!"));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.WritingConverterBuilder#getRequiredWritingConverter()
	 */
	@Override
	public GenericConverter getWritingConverter() {
		return getOptionalWritingConverter()
				.orElseThrow(() -> new IllegalStateException("No writing converter specified!"));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.ConverterBuilder#getConverters()
	 */
	@Override
	public Set<GenericConverter> getConverters() {

		return Optionals//
				.toStream(getOptionalReadingConverter(), getOptionalWritingConverter())//
				.collect(Collectors.toSet());
	}

	private Optional<GenericConverter> getOptionalReadingConverter() {
		return reading.map(it -> new ConfigurableGenericConverter.Reading<>(convertiblePair, it));
	}

	private Optional<GenericConverter> getOptionalWritingConverter() {
		return writing.map(it -> new ConfigurableGenericConverter.Writing<>(invertedPair(), it));
	}

	private ConvertiblePair invertedPair() {
		return new ConvertiblePair(convertiblePair.getTargetType(), convertiblePair.getSourceType());
	}

	DefaultConverterBuilder<S, T> withWriting(Optional<Function<? super S, ? extends T>> writing) {
		return this.writing == writing ? this
				: new DefaultConverterBuilder<S, T>(this.convertiblePair, writing, this.reading);
	}

	DefaultConverterBuilder<S, T> withReading(Optional<Function<? super T, ? extends S>> reading) {
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

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object, org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return function.apply((S) source);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(convertiblePair);
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

			if (!(o instanceof ConfigurableGenericConverter)) {
				return false;
			}

			ConfigurableGenericConverter<?, ?> that = (ConfigurableGenericConverter<?, ?>) o;

			if (!ObjectUtils.nullSafeEquals(convertiblePair, that.convertiblePair)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(function, that.function);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
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
