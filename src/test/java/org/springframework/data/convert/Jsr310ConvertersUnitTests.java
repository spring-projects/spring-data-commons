/*
 * Copyright 2014-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.Jsr310ConvertersUnitTests.CommonTests;
import org.springframework.data.convert.Jsr310ConvertersUnitTests.DurationConversionTests;
import org.springframework.data.convert.Jsr310ConvertersUnitTests.PeriodConversionTests;

/**
 * Unit tests for {@link Jsr310Converters}.
 *
 * @author Oliver Gierke
 * @author Barak Schoster
 * @author Jens Schauder
 * @author Mark Paluch
 */
@RunWith(Suite.class)
@SuiteClasses({ CommonTests.class, DurationConversionTests.class, PeriodConversionTests.class })
public class Jsr310ConvertersUnitTests {

	static final Date NOW = new Date();
	static final ConversionService CONVERSION_SERVICE;

	static {

		GenericConversionService conversionService = new GenericConversionService();

		for (Converter<?, ?> converter : Jsr310Converters.getConvertersToRegister()) {
			conversionService.addConverter(converter);
		}

		CONVERSION_SERVICE = conversionService;
	}

	public static class CommonTests {

		static final String FORMAT_DATE = "yyyy-MM-dd";
		static final String FORMAT_TIME = "HH:mm:ss.SSS";
		static final String FORMAT_FULL = String.format("%s'T'%s", FORMAT_DATE, FORMAT_TIME);

		@Test // DATACMNS-606, DATACMNS-1091
		public void convertsDateToLocalDateTime() {
			assertThat(CONVERSION_SERVICE.convert(NOW, LocalDateTime.class)).matches(formatted(NOW, FORMAT_FULL));
		}

		@Test // DATACMNS-606, DATACMNS-1091
		public void convertsLocalDateTimeToDate() {

			LocalDateTime now = LocalDateTime.now();
			assertThat(CONVERSION_SERVICE.convert(now, Date.class)).matches(formatted(now, FORMAT_FULL));
		}

		@Test // DATACMNS-606, DATACMNS-1091
		public void convertsDateToLocalDate() {
			assertThat(CONVERSION_SERVICE.convert(NOW, LocalDate.class)).matches(formatted(NOW, FORMAT_DATE));
		}

		@Test // DATACMNS-606, DATACMNS-1091
		public void convertsLocalDateToDate() {

			LocalDate now = LocalDate.now();
			assertThat(CONVERSION_SERVICE.convert(now, Date.class)).matches(formatted(now, FORMAT_DATE));
		}

		@Test // DATACMNS-606, DATACMNS-1091
		public void convertsDateToLocalTime() {
			assertThat(CONVERSION_SERVICE.convert(NOW, LocalTime.class)).matches(formatted(NOW, FORMAT_TIME));
		}

		@Test // DATACMNS-606, DATACMNS-1091
		public void convertsLocalTimeToDate() {

			LocalTime now = LocalTime.now();
			assertThat(CONVERSION_SERVICE.convert(now, Date.class)).matches(formatted(now, FORMAT_TIME));
		}

		@Test // DATACMNS-623
		public void convertsDateToInstant() {

			Date now = new Date();
			assertThat(CONVERSION_SERVICE.convert(now, Instant.class)).isEqualTo(now.toInstant());
		}

		@Test // DATACMNS-623
		public void convertsInstantToDate() {

			Date now = new Date();
			assertThat(CONVERSION_SERVICE.convert(now.toInstant(), Date.class)).isEqualTo(now);
		}

		@Test
		public void convertsZoneIdToStringAndBack() {

			Map<String, ZoneId> ids = new HashMap<>();
			ids.put("Europe/Berlin", ZoneId.of("Europe/Berlin"));
			ids.put("+06:00", ZoneId.of("+06:00"));

			for (Entry<String, ZoneId> entry : ids.entrySet()) {
				assertThat(CONVERSION_SERVICE.convert(entry.getValue(), String.class)).isEqualTo(entry.getKey());
				assertThat(CONVERSION_SERVICE.convert(entry.getKey(), ZoneId.class)).isEqualTo(entry.getValue());
			}
		}

		@Test // DATACMNS-1243
		public void convertsLocalDateTimeToInstantAndBack() {

			LocalDateTime dateTime = LocalDateTime.now();

			Instant instant = CONVERSION_SERVICE.convert(dateTime, Instant.class);
			LocalDateTime convertedDateTime = CONVERSION_SERVICE.convert(dateTime, LocalDateTime.class);

			assertThat(convertedDateTime).isEqualTo(dateTime);
		}

		private static Predicate<Date> formatted(Temporal expected, String format) {

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
			return d -> format(d, format).equals(formatter.format(expected));
		}

		private static Predicate<Temporal> formatted(Date expected, String format) {

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
			return d -> formatter.format(d).equals(format(expected, format));
		}

		private static String format(Date date, String format) {
			return new SimpleDateFormat(format).format(date);
		}
	}

	@RunWith(Parameterized.class)
	public static class DurationConversionTests extends ConversionTest<Duration> {

		// DATACMNS-951
		@Parameters
		public static Collection<Object[]> data() {

			return Arrays.asList(new Object[][] { //
					{ "PT240H", Duration.ofDays(10) }, //
					{ "PT2H", Duration.ofHours(2) }, //
					{ "PT3M", Duration.ofMinutes(3) }, //
					{ "PT4S", Duration.ofSeconds(4) }, //
					{ "PT0.005S", Duration.ofMillis(5) }, //
					{ "PT0.000000006S", Duration.ofNanos(6) } //
			});
		}
	}

	public static class PeriodConversionTests extends ConversionTest<Period> {

		// DATACMNS-951
		@Parameters
		public static Collection<Object[]> data() {

			return Arrays.asList(new Object[][] { //
					{ "P2D", Period.ofDays(2) }, //
					{ "P21D", Period.ofWeeks(3) }, //
					{ "P4M", Period.ofMonths(4) }, //
					{ "P5Y", Period.ofYears(5) }, //
			});
		}
	}

	@RunWith(Parameterized.class)
	public static class ConversionTest<T> {

		public @Parameter(0) String string;
		public @Parameter(1) T target;

		@Test
		public void convertsPeriodToStringAndBack() {

			ResolvableType type = ResolvableType.forClass(ConversionTest.class, this.getClass());
			assertThat(CONVERSION_SERVICE.convert(target, String.class)).isEqualTo(string);
			assertThat(CONVERSION_SERVICE.convert(string, type.getGeneric(0).getRawClass())).isEqualTo(target);
		}
	}
}
