/*
 * Copyright 2011-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.convert.CustomConversions.ConverterConfiguration;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter;
import org.springframework.data.convert.ThreeTenBackPortConverters.LocalDateTimeToJavaTimeInstantConverter;
import org.springframework.data.geo.Point;
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
class CustomConversionsUnitTests {

	static final SimpleTypeHolder DATE_EXCLUDING_SIMPLE_TYPE_HOLDER = new SimpleTypeHolder(
			Collections.singleton(Date.class), true) {

		@Override
		public boolean isSimpleType(Class<?> type) {
			return type.getName().startsWith("java.time") ? false : super.isSimpleType(type);
		}
	};

	@Test // DATACMNS-1035
	void findsBasicReadAndWriteConversions() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(FormatToStringConverter.INSTANCE, StringToFormatConverter.INSTANCE));

		assertThat(conversions.getCustomWriteTarget(Format.class)).hasValue(String.class);
		assertThat(conversions.getCustomWriteTarget(String.class)).isNotPresent();

		assertThat(conversions.hasCustomReadTarget(String.class, Format.class)).isTrue();
		assertThat(conversions.hasCustomReadTarget(String.class, Locale.class)).isFalse();
	}

	@Test // DATACMNS-1035
	void considersSubtypesCorrectly() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(NumberToStringConverter.INSTANCE, StringToNumberConverter.INSTANCE));

		assertThat(conversions.getCustomWriteTarget(Long.class)).hasValue(String.class);
		assertThat(conversions.hasCustomReadTarget(String.class, Long.class)).isTrue();
	}

	@Test // DATACMNS-1101
	void considersSubtypeCachingCorrectly() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(NumberToStringConverter.INSTANCE, StringToNumberConverter.INSTANCE));

		assertThat(conversions.getCustomWriteTarget(Long.class, Object.class)).isEmpty();
		assertThat(conversions.getCustomWriteTarget(Long.class)).hasValue(String.class);
		assertThat(conversions.getCustomWriteTarget(Long.class, Object.class)).isEmpty();
	}

	@Test // DATACMNS-1035
	void populatesConversionServiceCorrectly() {

		GenericConversionService conversionService = new DefaultConversionService();

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(StringToFormatConverter.INSTANCE));
		conversions.registerConvertersIn(conversionService);

		assertThat(conversionService.canConvert(String.class, Format.class)).isTrue();
	}

	@Test // DATAMONGO-259, DATACMNS-1035
	void doesNotConsiderTypeSimpleIfOnlyReadConverterIsRegistered() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(StringToFormatConverter.INSTANCE));
		assertThat(conversions.isSimpleType(Format.class)).isFalse();
	}

	@Test // DATAMONGO-298, DATACMNS-1035
	void discoversConvertersForSubtypesOfMongoTypes() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(StringToIntegerConverter.INSTANCE));
		assertThat(conversions.hasCustomReadTarget(String.class, Integer.class)).isTrue();
		assertThat(conversions.hasCustomWriteTarget(String.class, Integer.class)).isTrue();
	}

	@Test // DATAMONGO-795, DATACMNS-1035
	void favorsCustomConverterForIndeterminedTargetType() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(DateTimeToStringConverter.INSTANCE));
		assertThat(conversions.getCustomWriteTarget(DateTime.class)).hasValue(String.class);
	}

	@Test // DATAMONGO-881, DATACMNS-1035
	void customConverterOverridesDefault() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(CustomDateTimeConverter.INSTANCE));
		GenericConversionService conversionService = new DefaultConversionService();
		conversions.registerConvertersIn(conversionService);

		assertThat(conversionService.convert(new DateTime(), Date.class)).isEqualTo(new Date(0));
	}

	@Test // DATAMONGO-1001, DATACMNS-1035
	void shouldSelectPropertCustomWriteTargetForCglibProxiedType() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(FormatToStringConverter.INSTANCE));
		assertThat(conversions.getCustomWriteTarget(createProxyTypeFor(Format.class))).hasValue(String.class);
	}

	@Test // DATAMONGO-1001, DATACMNS-1035
	void shouldSelectPropertCustomReadTargetForCglibProxiedType() {

		CustomConversions conversions = new CustomConversions(StoreConversions.NONE,
				Arrays.asList(CustomObjectToStringConverter.INSTANCE));
		assertThat(conversions.hasCustomReadTarget(createProxyTypeFor(Object.class), String.class)).isTrue();
	}

	@Test // DATAMONGO-1131, DATACMNS-1035
	void registersConvertersForJsr310() {

		CustomConversions customConversions = new CustomConversions(StoreConversions.NONE, Collections.emptyList());

		assertThat(customConversions.hasCustomWriteTarget(java.time.LocalDateTime.class)).isTrue();
	}

	@Test // DATAMONGO-1131, DATACMNS-1035
	void registersConvertersForThreeTenBackPort() {

		CustomConversions customConversions = new CustomConversions(StoreConversions.NONE, Collections.emptyList());

		assertThat(customConversions.hasCustomWriteTarget(LocalDateTime.class)).isTrue();
	}

	@Test // DATAMONGO-1302, DATACMNS-1035
	void registersConverterFactoryCorrectly() {

		StoreConversions conversions = StoreConversions.of(new SimpleTypeHolder(Collections.singleton(Format.class), true));

		CustomConversions customConversions = new CustomConversions(conversions,
				Collections.singletonList(new FormatConverterFactory()));

		assertThat(customConversions.getCustomWriteTarget(String.class, SimpleDateFormat.class)).isPresent();
	}

	@Test // DATACMNS-1034
	void registersConverterFromConverterAware() {

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

	@Test // DATACMNS-1615
	void skipsUnsupportedDefaultWritingConverter() {

		ConverterRegistry registry = mock(ConverterRegistry.class);

		new CustomConversions(StoreConversions.of(DATE_EXCLUDING_SIMPLE_TYPE_HOLDER), Collections.emptyList())
				.registerConvertersIn(registry);

		verify(registry, never()).addConverter(any(LocalDateTimeToJavaTimeInstantConverter.class));
	}

	@Test // DATACMNS-1665
	void registersStoreConverter() {

		ConverterRegistry registry = mock(ConverterRegistry.class);

		SimpleTypeHolder holder = new SimpleTypeHolder(Collections.emptySet(), true);

		CustomConversions conversions = new CustomConversions(StoreConversions.of(holder, PointToMapConverter.INSTANCE),
				Collections.emptyList());
		conversions.registerConvertersIn(registry);

		assertThat(conversions.isSimpleType(Point.class));
		verify(registry).addConverter(any(PointToMapConverter.class));
	}

	@Test // DATACMNS-1615
	void doesNotSkipUnsupportedUserConverter() {

		ConverterRegistry registry = mock(ConverterRegistry.class);

		new CustomConversions(StoreConversions.of(DATE_EXCLUDING_SIMPLE_TYPE_HOLDER),
				Collections.singletonList(LocalDateTimeToJavaTimeInstantConverter.INSTANCE)).registerConvertersIn(registry);

		verify(registry).addConverter(any(LocalDateTimeToJavaTimeInstantConverter.class));
	}

	@Test // DATACMNS-1615
	void skipsConverterBasedOnConfiguration() {

		ConverterRegistry registry = mock(ConverterRegistry.class);

		ConverterConfiguration config = new ConverterConfiguration(StoreConversions.NONE, Collections.emptyList(),
				Predicate.<ConvertiblePair> isEqual(new ConvertiblePair(java.time.LocalDateTime.class, Date.class)).negate());
		new CustomConversions(config).registerConvertersIn(registry);

		verify(registry, never()).addConverter(any(LocalDateTimeToDateConverter.class));
	}

	@Test // DATACMNS-1615
	void doesNotSkipUserConverterConverterEvenWhenConfigurationWouldNotAllowIt() {

		ConverterRegistry registry = mock(ConverterRegistry.class);

		ConverterConfiguration config = new ConverterConfiguration(StoreConversions.NONE,
				Collections.singletonList(LocalDateTimeToDateConverter.INSTANCE),
				Predicate.<ConvertiblePair> isEqual(new ConvertiblePair(java.time.LocalDateTime.class, Date.class)).negate());
		new CustomConversions(config).registerConvertersIn(registry);

		verify(registry).addConverter(any(LocalDateTimeToDateConverter.class));
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
	enum PointToMapConverter implements Converter<Point, Map<String, String>> {

		INSTANCE;

		@Override
		public Map<String, String> convert(Point source) {
			return source != null ? Collections.emptyMap() : null;
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

			StringToFormat(Class<T> targetType) {
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
