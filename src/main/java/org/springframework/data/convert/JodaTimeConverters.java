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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

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
 * @author Christoph Strobl
 * @author Jens Schauder
 */
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

		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(DateTimeToDateConverter.INSTANCE);

		converters.add(DateToLocalDateConverter.INSTANCE);
		converters.add(DateToLocalDateTimeConverter.INSTANCE);
		converters.add(DateToDateTimeConverter.INSTANCE);

		converters.add(LocalDateTimeToJodaLocalDateTime.INSTANCE);
		converters.add(LocalDateTimeToJodaDateTime.INSTANCE);

		converters.add(LocalDateTimeToJsr310Converter.INSTANCE);

		return converters;
	}

	public enum LocalDateTimeToJsr310Converter implements Converter<LocalDateTime, java.time.LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public java.time.LocalDateTime convert(LocalDateTime source) {
			return java.time.LocalDateTime.ofInstant(source.toDate().toInstant(), ZoneId.of("UTC"));
		}
	}

	public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDate source) {
			return source.toDate();
		}
	}

	public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDateTime source) {
			return source.toDate();
		}
	}

	public enum DateTimeToDateConverter implements Converter<DateTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(DateTime source) {
			return source.toDate();
		}
	}

	public enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDate convert(Date source) {
			return new LocalDate(source.getTime());
		}
	}

	public enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDateTime convert(Date source) {
			return new LocalDateTime(source.getTime());
		}
	}

	public enum DateToDateTimeConverter implements Converter<Date, DateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public DateTime convert(Date source) {
			return new DateTime(source.getTime());
		}
	}

	public enum LocalDateTimeToJodaLocalDateTime implements Converter<java.time.LocalDateTime, LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDateTime convert(java.time.LocalDateTime source) {
			return LocalDateTime.fromDateFields(Jsr310Converters.LocalDateTimeToDateConverter.INSTANCE.convert(source));
		}
	}

	public enum LocalDateTimeToJodaDateTime implements Converter<java.time.LocalDateTime, DateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public DateTime convert(java.time.LocalDateTime source) {
			return new DateTime(Jsr310Converters.LocalDateTimeToDateConverter.INSTANCE.convert(source));
		}
	}
}
