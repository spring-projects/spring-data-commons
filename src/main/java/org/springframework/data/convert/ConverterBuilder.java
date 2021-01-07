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

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.util.Assert;

/**
 * API to easily set up {@link GenericConverter} instances using Java 8 lambdas, mostly in bidirectional fashion for
 * easy registration as custom type converters of the Spring Data mapping subsystem. The registration starts either with
 * the definition of a reading or writing converter that can then be completed.
 *
 * @author Oliver Gierke
 * @since 2.0
 * @see #reading(Class, Class, Function)
 * @see #writing(Class, Class, Function)
 * @soundtrack John Mayer - Moving On and Getting Over (The Search for Everything)
 */
public interface ConverterBuilder {

	/**
	 * Creates a new {@link ReadingConverterBuilder} to produce a converter to read values of the given source (the store
	 * type) into the given target (the domain type).
	 *
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	static <S, T> ReadingConverterBuilder<S, T> reading(Class<S> source, Class<T> target,
			Function<? super S, ? extends T> function) {

		Assert.notNull(source, "Source type must not be null!");
		Assert.notNull(target, "Target type must not be null!");
		Assert.notNull(function, "Conversion function must not be null!");

		return new DefaultConverterBuilder<>(new ConvertiblePair(source, target), Optional.empty(), Optional.of(function));
	}

	/**
	 * Creates a new {@link WritingConverterBuilder} to produce a converter to write values of the given source (the
	 * domain type) into the given target (the store type).
	 *
	 * @param source must not be {@literal null}.
	 * @param target must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	static <S, T> WritingConverterBuilder<S, T> writing(Class<S> source, Class<T> target,
			Function<? super S, ? extends T> function) {

		Assert.notNull(source, "Source type must not be null!");
		Assert.notNull(target, "Target type must not be null!");
		Assert.notNull(function, "Conversion function must not be null!");

		return new DefaultConverterBuilder<>(new ConvertiblePair(target, source), Optional.of(function), Optional.empty());
	}

	/**
	 * Returns all {@link GenericConverter} instances to be registered for the current {@link ConverterBuilder}.
	 *
	 * @return
	 */
	Set<GenericConverter> getConverters();

	/**
	 * Exposes a writing converter.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	interface WritingConverterAware {

		/**
		 * Returns the writing converter already created.
		 *
		 * @return
		 */
		GenericConverter getWritingConverter();
	}

	/**
	 * Exposes a reading converter.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	interface ReadingConverterAware {

		/**
		 * Returns the reading converter already created.
		 *
		 * @return
		 */
		GenericConverter getReadingConverter();
	}

	/**
	 * Interface to represent an intermediate setup step of {@link ConverterAware} defining a reading converter first.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	interface ReadingConverterBuilder<T, S> extends ConverterBuilder, ReadingConverterAware {

		/**
		 * Creates a new {@link ConverterAware} by registering the given {@link Function} to add a write converter.
		 *
		 * @param function must not be {@literal null}.
		 * @return
		 */
		ConverterAware andWriting(Function<? super S, ? extends T> function);
	}

	/**
	 * Interface to represent an intermediate setup step of {@link ConverterAware} defining a writing converter first.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	interface WritingConverterBuilder<S, T> extends ConverterBuilder, WritingConverterAware {

		/**
		 * Creates a new {@link ConverterAware} by registering the given {@link Function} to add a write converter.
		 *
		 * @param function must not be {@literal null}.
		 * @return
		 */
		ConverterAware andReading(Function<? super T, ? extends S> function);
	}

	/**
	 * A {@link ConverterBuilder} aware of both a reading and writing converter.
	 *
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	interface ConverterAware extends ConverterBuilder, ReadingConverterAware, WritingConverterAware {}
}
