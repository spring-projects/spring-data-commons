/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.convert;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.convert.ConverterBuilder.ReadingConverterBuilder;
import org.springframework.data.convert.ConverterBuilder.WritingConverterBuilder;
import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;

/**
 * Builder to easily set up (bi-directional) {@link Converter} instances for Spring Data type mapping using Lambdas. Use
 * factory methods on {@link ConverterBuilder} to create instances of this class.
 *
 * @author Oliver Gierke
 * @since 2.0
 * @see ConverterBuilder#writing(Class, Class, Function)
 * @see ConverterBuilder#reading(Class, Class, Function)
 * @soundtrack John Mayer - Still Feel Like Your Man (The Search for Everything)
 */
@Wither(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DefaultConverterBuilder<S, T>
		implements ConverterAware, ReadingConverterBuilder<T, S>, WritingConverterBuilder<S, T> {

	private final @NonNull ConvertiblePair convertiblePair;
	private final @NonNull Optional<Function<? super S, ? extends T>> writing;
	private final @NonNull Optional<Function<? super T, ? extends S>> reading;

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

	@RequiredArgsConstructor
	@EqualsAndHashCode
	private static class ConfigurableGenericConverter<S, T> implements GenericConverter {

		private final ConvertiblePair convertiblePair;
		private final Function<? super S, ? extends T> function;

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
		@Nonnull
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(convertiblePair);
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
