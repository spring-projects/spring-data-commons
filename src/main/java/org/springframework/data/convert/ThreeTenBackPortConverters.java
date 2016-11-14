/*
 * Copyright 2015 the original author or authors.
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

import static org.threeten.bp.DateTimeUtils.*;
import static org.threeten.bp.Instant.*;
import static org.threeten.bp.LocalDateTime.*;
import static org.threeten.bp.ZoneId.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;

/**
 * Helper class to register {@link Converter} implementations for the ThreeTen Backport project in case it's present on
 * the classpath.
 * 
 * @author Oliver Gierke
 * @see http://www.threeten.org/threetenbp
 * @since 1.10
 */
public abstract class ThreeTenBackPortConverters {

	private static final boolean THREE_TEN_BACK_PORT_IS_PRESENT = ClassUtils.isPresent("org.threeten.bp.LocalDateTime",
			ThreeTenBackPortConverters.class.getClassLoader());

	/**
	 * Returns the converters to be registered. Will only return converters in case we're running on Java 8.
	 * 
	 * @return
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {

		if (!THREE_TEN_BACK_PORT_IS_PRESENT) {
			return Collections.emptySet();
		}

		List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();
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

		return converters;
	}

	public static boolean supports(Class<?> type) {

		if (!THREE_TEN_BACK_PORT_IS_PRESENT) {
			return false;
		}

		return Arrays.<Class<?>> asList(LocalDateTime.class, LocalDate.class, LocalTime.class, Instant.class)
				.contains(type);
	}

	public static enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(Date source) {

			return source == null ? null : ofInstant(toInstant(source), systemDefault());
		}
	}

	public static enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalDateTime source) {
			return source == null ? null : toDate(source.atZone(systemDefault()).toInstant());
		}
	}

	public static enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		@Override
		public LocalDate convert(Date source) {
			return source == null ? null : ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
		}
	}

	public static enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalDate source) {
			return source == null ? null : toDate(source.atStartOfDay(systemDefault()).toInstant());
		}
	}

	public static enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {

		INSTANCE;

		@Override
		public LocalTime convert(Date source) {
			return source == null ? null : ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalTime();
		}
	}

	public static enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalTime source) {
			return source == null ? null : toDate(source.atDate(LocalDate.now()).atZone(systemDefault()).toInstant());
		}
	}

	public static enum DateToInstantConverter implements Converter<Date, Instant> {

		INSTANCE;

		@Override
		public Instant convert(Date source) {
			return source == null ? null : toInstant(source);
		}
	}

	public static enum InstantToDateConverter implements Converter<Instant, Date> {

		INSTANCE;

		@Override
		public Date convert(Instant source) {
			return source == null ? null : toDate(source.atZone(systemDefault()).toInstant());
		}
	}

	@WritingConverter
	public static enum ZoneIdToStringConverter implements Converter<ZoneId, String> {

		INSTANCE;

		@Override
		public String convert(ZoneId source) {
			return source.toString();
		}
	}

	@ReadingConverter
	public static enum StringToZoneIdConverter implements Converter<String, ZoneId> {

		INSTANCE;

		@Override
		public ZoneId convert(String source) {
			return ZoneId.of(source);
		}
	}

}
