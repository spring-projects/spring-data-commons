/*
 * Copyright 2013-present the original author or authors.
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

/**
 * Helper class to register JSR-310 specific {@link Converter} implementations.
 *
 * @author Oliver Gierke
 * @author Barak Schoster
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Jan Durovec
 */
public abstract class Jsr310Converters {

	private static final List<Class<?>> CLASSES = List.of(LocalDateTime.class, LocalDate.class, LocalTime.class,
			Instant.class, ZoneId.class, Duration.class, Period.class);

	/**
	 * Returns the converters to be registered.
	 *
	 * @return the converters to be registered.
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {

		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(DateToLocalDateTimeConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(DateToLocalDateConverter.INSTANCE);
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(DateToLocalTimeConverter.INSTANCE);
		converters.add(LocalTimeToDateConverter.INSTANCE);
		converters.add(DateToInstantConverter.INSTANCE);
		converters.add(InstantToDateConverter.INSTANCE);
		converters.add(LocalDateTimeToInstantConverter.INSTANCE);
		converters.add(InstantToLocalDateTimeConverter.INSTANCE);
		converters.add(ZoneIdToStringConverter.INSTANCE);
		converters.add(StringToZoneIdConverter.INSTANCE);
		converters.add(DurationToStringConverter.INSTANCE);
		converters.add(StringToDurationConverter.INSTANCE);
		converters.add(PeriodToStringConverter.INSTANCE);
		converters.add(StringToPeriodConverter.INSTANCE);
		converters.add(StringToLocalTimeConverter.INSTANCE);
		converters.add(LocalTimeToStringConverter.INSTANCE);
		converters.add(StringToLocalDateConverter.INSTANCE);
		converters.add(StringToLocalDateTimeConverter.INSTANCE);
		converters.add(StringToInstantConverter.INSTANCE);

		return converters;
	}

	public static boolean supports(Class<?> type) {
		return CLASSES.contains(type);
	}

	/**
	 * @return the collection of supported temporal classes.
	 * @since 3.2
	 */
	public static Collection<Class<?>> getSupportedClasses() {
		return CLASSES;
	}

	@ReadingConverter
	public enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(Date source) {
			return ofInstant(source.toInstant(), systemDefault());
		}
	}

	@WritingConverter
	public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalDateTime source) {
			return Date.from(source.atZone(systemDefault()).toInstant());
		}
	}

	@ReadingConverter
	public enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		@Override
		public LocalDate convert(Date source) {
			return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
		}
	}

	@WritingConverter
	public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalDate source) {
			return Date.from(source.atStartOfDay(systemDefault()).toInstant());
		}
	}

	@ReadingConverter
	public enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {

		INSTANCE;

		@Override
		public LocalTime convert(Date source) {
			return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalTime();
		}
	}

	@WritingConverter
	public enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalTime source) {
			return Date.from(source.atDate(LocalDate.now()).atZone(systemDefault()).toInstant());
		}
	}

	@ReadingConverter
	public enum DateToInstantConverter implements Converter<Date, Instant> {

		INSTANCE;

		@Override
		public Instant convert(Date source) {
			return source.toInstant();
		}
	}

	@WritingConverter
	public enum InstantToDateConverter implements Converter<Instant, Date> {

		INSTANCE;

		@Override
		public Date convert(Instant source) {
			return Date.from(source);
		}
	}

	@ReadingConverter
	public enum LocalDateTimeToInstantConverter implements Converter<LocalDateTime, Instant> {

		INSTANCE;

		@Override
		public Instant convert(LocalDateTime source) {
			return source.atZone(systemDefault()).toInstant();
		}
	}

	@ReadingConverter
	public enum InstantToLocalDateTimeConverter implements Converter<Instant, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(Instant source) {
			return LocalDateTime.ofInstant(source, systemDefault());
		}
	}

	@WritingConverter
	public enum ZoneIdToStringConverter implements Converter<ZoneId, String> {

		INSTANCE;

		@Override
		public String convert(ZoneId source) {
			return source.toString();
		}
	}

	@ReadingConverter
	public enum StringToZoneIdConverter implements Converter<String, ZoneId> {

		INSTANCE;

		@Override
		public ZoneId convert(String source) {
			return ZoneId.of(source);
		}
	}

	@WritingConverter
	public enum DurationToStringConverter implements Converter<Duration, String> {

		INSTANCE;

		@Override
		public String convert(Duration duration) {
			return duration.toString();
		}
	}

	@ReadingConverter
	public enum StringToDurationConverter implements Converter<String, Duration> {

		INSTANCE;

		@Override
		public Duration convert(String s) {
			return Duration.parse(s);
		}
	}

	@WritingConverter
	public enum PeriodToStringConverter implements Converter<Period, String> {

		INSTANCE;

		@Override
		public String convert(Period period) {
			return period.toString();
		}
	}

	@ReadingConverter
	public enum StringToPeriodConverter implements Converter<String, Period> {

		INSTANCE;

		@Override
		public Period convert(String s) {
			return Period.parse(s);
		}
	}

	/**
	 * @since 4.1.2
	 */
	@ReadingConverter
	public enum StringToLocalTimeConverter implements Converter<String, LocalTime> {

		INSTANCE;

		@Override
		public LocalTime convert(String source) {
			return LocalTime.parse(source);
		}
	}

	/**
	 * @since 4.1.2
	 */
	@ReadingConverter
	public enum LocalTimeToStringConverter implements Converter<LocalTime, String> {

		INSTANCE;

		@Override
		public String convert(LocalTime source) {
			return source.toString();
		}
	}

	@ReadingConverter
	public enum StringToLocalDateConverter implements Converter<String, LocalDate> {

		INSTANCE;

		@Override
		public LocalDate convert(String source) {
			return LocalDate.parse(source, DateTimeFormatter.ISO_DATE);
		}
	}

	@ReadingConverter
	public enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(String source) {
			return LocalDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
		}
	}

	@ReadingConverter
	public enum StringToInstantConverter implements Converter<String, Instant> {

		INSTANCE;

		@Override
		public Instant convert(String source) {
			return Instant.parse(source);
		}
	}
}
