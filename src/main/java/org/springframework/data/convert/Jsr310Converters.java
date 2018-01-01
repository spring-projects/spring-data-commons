/*
 * Copyright 2013-2018 the original author or authors.
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

import static java.time.Instant.*;
import static java.time.LocalDateTime.*;
import static java.time.ZoneId.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

/**
 * Helper class to register JSR-310 specific {@link Converter} implementations in case the we're running on Java 8.
 *
 * @author Oliver Gierke
 * @author Barak Schoster
 * @author Christoph Strobl
 */
public abstract class Jsr310Converters {

	private static final boolean JAVA_8_IS_PRESENT = ClassUtils.isPresent("java.time.LocalDateTime",
			Jsr310Converters.class.getClassLoader());

	/**
	 * Returns the converters to be registered. Will only return converters in case we're running on Java 8.
	 *
	 * @return
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {

		if (!JAVA_8_IS_PRESENT) {
			return Collections.emptySet();
		}

		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(DateToLocalDateTimeConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(DateToLocalDateConverter.INSTANCE);
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(DateToLocalTimeConverter.INSTANCE);
		converters.add(LocalTimeToDateConverter.INSTANCE);
		converters.add(DateToInstantConverter.INSTANCE);
		converters.add(InstantToDateConverter.INSTANCE);
		converters.add(ZoneIdToStringConverter.INSTANCE);
		converters.add(StringToZoneIdConverter.INSTANCE);
		converters.add(DurationToStringConverter.INSTANCE);
		converters.add(StringToDurationConverter.INSTANCE);
		converters.add(PeriodToStringConverter.INSTANCE);
		converters.add(StringToPeriodConverter.INSTANCE);

		return converters;
	}

	public static boolean supports(Class<?> type) {

		if (!JAVA_8_IS_PRESENT) {
			return false;
		}

		return Arrays.<Class<?>> asList(LocalDateTime.class, LocalDate.class, LocalTime.class, Instant.class)
				.contains(type);
	}

	public static enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDateTime convert(Date source) {
			return ofInstant(source.toInstant(), systemDefault());
		}
	}

	public static enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDateTime source) {
			return Date.from(source.atZone(systemDefault()).toInstant());
		}
	}

	public static enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDate convert(Date source) {
			return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
		}
	}

	public static enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDate source) {
			return Date.from(source.atStartOfDay(systemDefault()).toInstant());
		}
	}

	public static enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalTime convert(Date source) {
			return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalTime();
		}
	}

	public static enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalTime source) {
			return Date.from(source.atDate(LocalDate.now()).atZone(systemDefault()).toInstant());
		}
	}

	public static enum DateToInstantConverter implements Converter<Date, Instant> {

		INSTANCE;

		@Nonnull
		@Override
		public Instant convert(Date source) {
			return source.toInstant();
		}
	}

	public static enum InstantToDateConverter implements Converter<Instant, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(Instant source) {
			return Date.from(source.atZone(systemDefault()).toInstant());
		}
	}

	@WritingConverter
	public static enum ZoneIdToStringConverter implements Converter<ZoneId, String> {

		INSTANCE;

		@Nonnull
		@Override
		public String convert(ZoneId source) {
			return source.toString();
		}
	}

	@ReadingConverter
	public static enum StringToZoneIdConverter implements Converter<String, ZoneId> {

		INSTANCE;

		@Nonnull
		@Override
		public ZoneId convert(String source) {
			return ZoneId.of(source);
		}
	}

	@WritingConverter
	public static enum DurationToStringConverter implements Converter<Duration, String> {

		INSTANCE;

		@Nonnull
		@Override
		public String convert(Duration duration) {
			return duration.toString();
		}
	}

	@ReadingConverter
	public static enum StringToDurationConverter implements Converter<String, Duration> {

		INSTANCE;

		@Nonnull
		@Override
		public Duration convert(String s) {
			return Duration.parse(s);
		}
	}

	@WritingConverter
	public static enum PeriodToStringConverter implements Converter<Period, String> {

		INSTANCE;

		@Nonnull
		@Override
		public String convert(Period period) {
			return period.toString();
		}
	}

	@ReadingConverter
	public static enum StringToPeriodConverter implements Converter<String, Period> {

		INSTANCE;

		@Nonnull
		@Override
		public Period convert(String s) {
			return Period.parse(s);
		}
	}
}
