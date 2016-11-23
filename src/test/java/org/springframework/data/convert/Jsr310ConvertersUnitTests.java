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
package org.springframework.data.convert;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * Unit tests for {@link Jsr310Converters}.
 * 
 * @author Oliver Gierke & Barak Schoster
 */
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

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsDateToLocalDateTime() {
		assertThat(CONVERSION_SERVICE.convert(NOW, LocalDateTime.class).toString(),
				is(format(NOW, "yyyy-MM-dd'T'HH:mm:ss.SSS")));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsLocalDateTimeToDate() {

		LocalDateTime now = LocalDateTime.now();
		assertThat(format(CONVERSION_SERVICE.convert(now, Date.class), "yyyy-MM-dd'T'HH:mm:ss.SSS"), is(now.toString()));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsDateToLocalDate() {
		assertThat(CONVERSION_SERVICE.convert(NOW, LocalDate.class).toString(), is(format(NOW, "yyyy-MM-dd")));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsLocalDateToDate() {

		LocalDate now = LocalDate.now();
		assertThat(format(CONVERSION_SERVICE.convert(now, Date.class), "yyyy-MM-dd"), is(now.toString()));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsDateToLocalTime() {
		assertThat(CONVERSION_SERVICE.convert(NOW, LocalTime.class).toString(), is(format(NOW, "HH:mm:ss.SSS")));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsLocalTimeToDate() {

		LocalTime now = LocalTime.now();
		assertThat(format(CONVERSION_SERVICE.convert(now, Date.class), "HH:mm:ss.SSS"), is(now.toString()));
	}

	/**
	 * @see DATACMNS-623
	 */
	@Test
	public void convertsDateToInstant() {

		Date now = new Date();
		assertThat(CONVERSION_SERVICE.convert(now, Instant.class), is(now.toInstant()));
	}

	/**
	 * @see DATACMNS-623
	 */
	@Test
	public void convertsInstantToDate() {

		Date now = new Date();
		assertThat(CONVERSION_SERVICE.convert(now.toInstant(), Date.class), is(now));
	}

	@Test
	public void convertsZoneIdToStringAndBack() {

		Map<String, ZoneId> ids = new HashMap<String, ZoneId>();
		ids.put("Europe/Berlin", ZoneId.of("Europe/Berlin"));
		ids.put("+06:00", ZoneId.of("+06:00"));

		for (Entry<String, ZoneId> entry : ids.entrySet()) {
			assertThat(CONVERSION_SERVICE.convert(entry.getValue(), String.class), is(entry.getKey()));
			assertThat(CONVERSION_SERVICE.convert(entry.getKey(), ZoneId.class), is(entry.getValue()));
		}
	}

	@Test
	public void convertsDurationToStringAndBack() {

		Map<String, Duration> ids = new HashMap<String, Duration>();
		ids.put("PT240H", Duration.ofDays(10));
		ids.put("PT2H", Duration.ofHours(2));
		ids.put("PT3M", Duration.ofMinutes(3));
		ids.put("PT4S", Duration.ofSeconds(4));
		ids.put("PT0.005S", Duration.ofMillis(5));
		ids.put("PT0.000000006S", Duration.ofNanos(6));


		for (Entry<String, Duration> entry : ids.entrySet()) {
			assertThat(CONVERSION_SERVICE.convert(entry.getValue(), String.class), is(entry.getKey()));
			assertThat(CONVERSION_SERVICE.convert(entry.getKey(), Duration.class), is(entry.getValue()));
		}
	}

	@Test
	public void convertsPeriodToStringAndBack() {

		Map<String, Period> ids = new HashMap<String, Period>();
		ids.put("P2D", Period.ofDays(2));
		ids.put("P21D", Period.ofWeeks(3));
		ids.put("P4M", Period.ofMonths(4));
		ids.put("P5Y", Period.ofYears(5));

		for (Entry<String, Period> entry : ids.entrySet()) {
			assertThat(CONVERSION_SERVICE.convert(entry.getValue(), String.class), is(entry.getKey()));
			assertThat(CONVERSION_SERVICE.convert(entry.getKey(), Period.class), is(entry.getValue()));
		}
	}
	private static String format(Date date, String format) {
		return new SimpleDateFormat(format).format(date);
	}
}
