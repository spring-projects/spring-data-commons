/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

/**
 * Helper class to register JodaTime specific {@link Converter} implementations in case the library is present on the
 * classpath.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
public abstract class JodaTimeConverters {

	private static final boolean JODA_TIME_IS_PRESENT = ClassUtils.isPresent("org.joda.time.LocalDate", null);

	/**
	 * Returns the converters to be registered. Will only return converters in case JodaTime is present on the class.
	 * 
	 * @return
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {

		if (!JODA_TIME_IS_PRESENT) {
			return Collections.emptySet();
		}

		List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(DateTimeToDateConverter.INSTANCE);
		converters.add(DateMidnightToDateConverter.INSTANCE);

		converters.add(DateToLocalDateConverter.INSTANCE);
		converters.add(DateToLocalDateTimeConverter.INSTANCE);
		converters.add(DateToDateTimeConverter.INSTANCE);
		converters.add(DateToDateMidnightConverter.INSTANCE);

		converters.add(LocalDateTimeToJodaLocalDateTime.INSTANCE);
		converters.add(LocalDateTimeToJodaDateTime.INSTANCE);

		return converters;
	}

	public static enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		public Date convert(LocalDate source) {
			return source == null ? null : source.toDate();
		}
	}

	public static enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		public Date convert(LocalDateTime source) {
			return source == null ? null : source.toDate();
		}
	}

	public static enum DateTimeToDateConverter implements Converter<DateTime, Date> {

		INSTANCE;

		public Date convert(DateTime source) {
			return source == null ? null : source.toDate();
		}
	}

	public static enum DateMidnightToDateConverter implements Converter<DateMidnight, Date> {

		INSTANCE;

		public Date convert(DateMidnight source) {
			return source == null ? null : source.toDate();
		}
	}

	public static enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		public LocalDate convert(Date source) {
			return source == null ? null : new LocalDate(source.getTime());
		}
	}

	public static enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		public LocalDateTime convert(Date source) {
			return source == null ? null : new LocalDateTime(source.getTime());
		}
	}

	public static enum DateToDateTimeConverter implements Converter<Date, DateTime> {

		INSTANCE;

		public DateTime convert(Date source) {
			return source == null ? null : new DateTime(source.getTime());
		}
	}

	public static enum DateToDateMidnightConverter implements Converter<Date, DateMidnight> {

		INSTANCE;

		public DateMidnight convert(Date source) {
			return source == null ? null : new DateMidnight(source.getTime());
		}
	}

	public static enum LocalDateTimeToJodaLocalDateTime implements Converter<java.time.LocalDateTime, LocalDateTime> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public LocalDateTime convert(java.time.LocalDateTime source) {
			return source == null ? null
					: LocalDateTime.fromDateFields(
							org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter.INSTANCE.convert(source));
		}
	}

	public static enum LocalDateTimeToJodaDateTime implements Converter<java.time.LocalDateTime, DateTime> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public DateTime convert(java.time.LocalDateTime source) {
			return source == null ? null
					: new DateTime(
							org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter.INSTANCE.convert(source));
		}
	}
}
