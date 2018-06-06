/*
 * Copyright 2011-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.threeten.bp.LocalDateTime;

/**
 * Unit tests for {@link CustomConversions}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.0
 */
public class CustomConversionsUnitTests {

	@Test // DATACMNS-1035
	public void findsBasicReadAndWriteConversions() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(FormatToStringConverter.INSTANCE, StringToFormatConverter.INSTANCE));

		assertThat(conversions.getCustomWriteTarget(Format.class)).hasValue(String.class);
		assertThat(conversions.getCustomWriteTarget(String.class)).isNotPresent();

		assertThat(conversions.hasCustomReadTarget(String.class, Format.class)).isTrue();
		assertThat(conversions.hasCustomReadTarget(String.class, Locale.class)).isFalse();
	}

	@Test // DATACMNS-1035
	public void considersSubtypesCorrectly() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(NumberToStringConverter.INSTANCE, StringToNumberConverter.INSTANCE));

		assertThat(conversions.getCustomWriteTarget(Long.class)).hasValue(String.class);
		assertThat(conversions.hasCustomReadTarget(String.class, Long.class)).isTrue();
	}

	@Test // DATACMNS-1101
	public void considersSubtypeCachingCorrectly() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(NumberToStringConverter.INSTANCE, StringToNumberConverter.INSTANCE));

		assertThat(conversions.getCustomWriteTarget(Long.class, Object.class)).isEmpty();
		assertThat(conversions.getCustomWriteTarget(Long.class)).hasValue(String.class);
		assertThat(conversions.getCustomWriteTarget(Long.class, Object.class)).isEmpty();
	}

	@Test // DATACMNS-1035
	public void populatesConversionServiceCorrectly() {

		GenericConversionService conversionService = new DefaultConversionService();

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(StringToFormatConverter.INSTANCE));
		conversions.registerConvertersIn(conversionService);

		assertThat(conversionService.canConvert(String.class, Format.class), is(true));
	}

	@Test // DATAMONGO-259, DATACMNS-1035
	public void doesNotConsiderTypeSimpleIfOnlyReadConverterIsRegistered() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(StringToFormatConverter.INSTANCE));
		assertThat(conversions.isSimpleType(Format.class), is(false));
	}

	@Test // DATAMONGO-298, DATACMNS-1035
	public void discoversConvertersForSubtypesOfMongoTypes() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(StringToIntegerConverter.INSTANCE));
		assertThat(conversions.hasCustomReadTarget(String.class, Integer.class), is(true));
		assertThat(conversions.hasCustomWriteTarget(String.class, Integer.class), is(true));
	}

	@Test // DATAMONGO-795, DATACMNS-1035
	public void favorsCustomConverterForIndeterminedTargetType() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(DateTimeToStringConverter.INSTANCE));
		assertThat(conversions.getCustomWriteTarget(DateTime.class)).hasValue(String.class);
	}

	@Test // DATAMONGO-881, DATACMNS-1035
	public void customConverterOverridesDefault() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(CustomDateTimeConverter.INSTANCE));
		GenericConversionService conversionService = new DefaultConversionService();
		conversions.registerConvertersIn(conversionService);

		assertThat(conversionService.convert(new DateTime(), Date.class)).isEqualTo(new Date(0));
	}

	@Test // DATAMONGO-1001, DATACMNS-1035
	public void shouldSelectPropertCustomWriteTargetForCglibProxiedType() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(FormatToStringConverter.INSTANCE));
		assertThat(conversions.getCustomWriteTarget(createProxyTypeFor(Format.class))).hasValue(String.class);
	}

	@Test // DATAMONGO-1001, DATACMNS-1035
	public void shouldSelectPropertCustomReadTargetForCglibProxiedType() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(CustomObjectToStringConverter.INSTANCE));
		assertThat(conversions.hasCustomReadTarget(createProxyTypeFor(Object.class), String.class)).isTrue();
	}

	@Test // DATAMONGO-1131, DATACMNS-1035
	public void registersConvertersForJsr310() {

		CustomConversions customConversions = new CustomConversions(StoreConversions.NONE, Collections.emptyList());

		assertThat(customConversions.hasCustomWriteTarget(java.time.LocalDateTime.class)).isTrue();
	}

	@Test // DATAMONGO-1131, DATACMNS-1035
	public void registersConvertersForThreeTenBackPort() {

		CustomConversions customConversions = new CustomConversions(StoreConversions.NONE, Collections.emptyList());

		assertThat(customConversions.hasCustomWriteTarget(LocalDateTime.class)).isTrue();
	}

	@Test // DATAMONGO-1302, DATACMNS-1035
	public void registersConverterFactoryCorrectly() {

		StoreConversions conversions = StoreConversions.of(new SimpleTypeHolder(Collections.singleton(Format.class), true));

		CustomConversions customConversions = new CustomConversions(conversions,
				Collections.singletonList(new FormatConverterFactory()));

		assertThat(customConversions.getCustomWriteTarget(String.class, SimpleDateFormat.class)).isPresent();
	}

	@Test // DATACMNS-1034
	public void registersConverterFromConverterAware() {

		ConverterAware converters = ConverterBuilder //
				.reading(Locale.class, CustomType.class, left -> new CustomType()) //
				.andWriting(right -> Locale.GERMAN);

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE, Collections.singletonList(converters));

		assertThat(conversions.hasCustomWriteTarget(CustomType.class)).isTrue();
		assertThat(conversions.hasCustomReadTarget(Locale.class, CustomType.class)).isTrue();

		ConfigurableConversionService conversionService = new GenericConversionService();
		conversions.registerConvertersIn(conversionService);

		assertThat(conversionService.canConvert(CustomType.class, Locale.class)).isTrue();
		assertThat(conversionService.canConvert(Locale.class, CustomType.class)).isTrue();
	}

	private static Class<?> createProxyTypeFor(Class<?> type) {

		ProxyFactory factory = new ProxyFactory();
		factory.setProxyTargetClass(true);
		factory.setTargetClass(type);

		return factory.getProxy().getClass();
	}

	enum FormatToStringConverter implements Converter<Format, String> {

		INSTANCE;

		public String convert(Format source) {
			return source.toString();
		}
	}

	enum StringToFormatConverter implements Converter<String, Format> {

		INSTANCE;

		public Format convert(String source) {
			return DateFormat.getInstance();
		}
	}

	enum NumberToStringConverter implements Converter<Number, String> {

		INSTANCE;

		public String convert(Number source) {
			return source.toString();
		}
	}

	enum StringToNumberConverter implements Converter<String, Number> {

		INSTANCE;

		public Number convert(String source) {
			return 0L;
		}
	}

	enum StringToIntegerConverter implements Converter<String, Integer> {

		INSTANCE;

		public Integer convert(String source) {
			return 0;
		}
	}

	enum DateTimeToStringConverter implements Converter<DateTime, String> {

		INSTANCE;

		@Override
		public String convert(DateTime source) {
			return "";
		}
	}

	enum CustomDateTimeConverter implements Converter<DateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(DateTime source) {
			return new Date(0);
		}
	}

	enum CustomObjectToStringConverter implements Converter<Object, String> {

		INSTANCE;

		@Override
		public String convert(Object source) {
			return source != null ? source.toString() : null;
		}

	}

	@WritingConverter
	static class FormatConverterFactory implements ConverterFactory<String, Format> {

		@Override
		public <T extends Format> Converter<String, T> getConverter(Class<T> targetType) {
			return new StringToFormat<T>(targetType);
		}

		private static final class StringToFormat<T extends Format> implements Converter<String, T> {

			private final Class<T> targetType;

			public StringToFormat(Class<T> targetType) {
				this.targetType = targetType;
			}

			@Override
			public T convert(String source) {

				if (source.length() == 0) {
					return null;
				}

				try {
					return targetType.newInstance();
				} catch (Exception e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}
			}
		}
	}

	static class CustomType {}
}
