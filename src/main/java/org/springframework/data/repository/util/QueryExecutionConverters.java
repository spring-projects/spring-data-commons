/*
 * Copyright 2014 the original author or authors.
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
import java.util.concurrent.Future;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.google.common.base.Optional;

/**
 * Converters to potentially wrap the execution of a repository method into a variety of wrapper types potentially being
 * available on the classpath.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
public class QueryExecutionConverters {

	private static final boolean GUAVA_PRESENT = ClassUtils.isPresent("com.google.common.base.Optional",
			QueryExecutionConverters.class.getClassLoader());
	private static final boolean JDK_PRESENT = ClassUtils.isPresent("java.util.Optional",
			QueryExecutionConverters.class.getClassLoader());

	private static final Set<Class<?>> WRAPPER_TYPES;
	private static final Set<Converter<?, ?>> CONVERTERS;

	static {

		Set<Class<?>> wrapperTypes = new HashSet<Class<?>>();
		Set<Converter<?, ?>> converters = new HashSet<Converter<?, ?>>();

		wrapperTypes.add(Future.class);
		converters.add(ObjectToFutureConverter.INSTANCE);

		if (GUAVA_PRESENT) {
			wrapperTypes.add(ObjectToGuavaOptionalConverter.INSTANCE.getWrapperType());
			converters.add(ObjectToGuavaOptionalConverter.INSTANCE);
		}

		if (JDK_PRESENT) {
			wrapperTypes.add(ObjectToJdk8OptionalConverter.INSTANCE.getWrapperType());
			converters.add(ObjectToJdk8OptionalConverter.INSTANCE);
		}

		WRAPPER_TYPES = Collections.unmodifiableSet(wrapperTypes);
		CONVERTERS = Collections.unmodifiableSet(converters);
	}

	/**
	 * Returns whether the given type is a supported wrapper type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supports(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");
		return WRAPPER_TYPES.contains(type);
	}

	/**
	 * Registers converters for wrapper types found on the classpath.
	 * 
	 * @param conversionService must not be {@literal null}.
	 */
	public static void registerConvertersIn(ConfigurableConversionService conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		for (Converter<?, ?> converter : CONVERTERS) {
			conversionService.addConverter(converter);
		}
	}

	/**
	 * A Spring {@link Converter} to support Google Guava's {@link Optional}.
	 * 
	 * @author Oliver Gierke
	 */
	private static enum ObjectToGuavaOptionalConverter implements Converter<NullableWrapper, Optional<Object>> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Optional<Object> convert(NullableWrapper source) {
			return Optional.fromNullable(source.getValue());
		}

		public Class<?> getWrapperType() {
			return Optional.class;
		}
	}

	/**
	 * A Spring {@link Converter} to support JDK 8's {@link java.util.Optional}.
	 * 
	 * @author Oliver Gierke
	 */
	private static enum ObjectToJdk8OptionalConverter implements Converter<NullableWrapper, java.util.Optional<Object>> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public java.util.Optional<Object> convert(NullableWrapper source) {
			return java.util.Optional.ofNullable(source.getValue());
		}

		public Class<?> getWrapperType() {
			return java.util.Optional.class;
		}
	}

	/**
	 * A Spring {@link Converter} to support returning {@link Future} instances from repository methods.
	 * 
	 * @author Oliver Gierke
	 */
	private static enum ObjectToFutureConverter implements Converter<NullableWrapper, Future<Object>> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Future<Object> convert(NullableWrapper source) {
			return new AsyncResult<Object>(source.getValue());
		}
	}
}
