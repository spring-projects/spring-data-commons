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

import static org.assertj.core.api.Assertions.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
 * @author Oliver Gierke
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
		assertThat(CONVERSION_SERVICE.convert(NOW, LocalDateTime.class).toString())
				.isEqualTo(format(NOW, "yyyy-MM-dd'T'HH:mm:ss.SSS"));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsLocalDateTimeToDate() {

		LocalDateTime now = LocalDateTime.now();
		assertThat(format(CONVERSION_SERVICE.convert(now, Date.class), "yyyy-MM-dd'T'HH:mm:ss.SSS"))
				.isEqualTo(now.toString());
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsDateToLocalDate() {
		assertThat(CONVERSION_SERVICE.convert(NOW, LocalDate.class).toString()).isEqualTo(format(NOW, "yyyy-MM-dd"));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsLocalDateToDate() {

		LocalDate now = LocalDate.now();
		assertThat(format(CONVERSION_SERVICE.convert(now, Date.class), "yyyy-MM-dd")).isEqualTo(now.toString());
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsDateToLocalTime() {
		assertThat(CONVERSION_SERVICE.convert(NOW, LocalTime.class).toString()).isEqualTo(format(NOW, "HH:mm:ss.SSS"));
	}

	/**
	 * @see DATACMNS-606
	 */
	@Test
	public void convertsLocalTimeToDate() {

		LocalTime now = LocalTime.now();
		assertThat(format(CONVERSION_SERVICE.convert(now, Date.class), "HH:mm:ss.SSS")).isEqualTo(now.toString());
	}

	/**
	 * @see DATACMNS-623
	 */
	@Test
	public void convertsDateToInstant() {

		Date now = new Date();
		assertThat(CONVERSION_SERVICE.convert(now, Instant.class)).isEqualTo(now.toInstant());
	}

	/**
	 * @see DATACMNS-623
	 */
	@Test
	public void convertsInstantToDate() {

		Date now = new Date();
		assertThat(CONVERSION_SERVICE.convert(now.toInstant(), Date.class)).isEqualTo(now);
	}

	@Test
	public void convertsZoneIdToStringAndBack() {

		Map<String, ZoneId> ids = new HashMap<String, ZoneId>();
		ids.put("Europe/Berlin", ZoneId.of("Europe/Berlin"));
		ids.put("+06:00", ZoneId.of("+06:00"));

		for (Entry<String, ZoneId> entry : ids.entrySet()) {
			assertThat(CONVERSION_SERVICE.convert(entry.getValue(), String.class)).isEqualTo(entry.getKey());
			assertThat(CONVERSION_SERVICE.convert(entry.getKey(), ZoneId.class)).isEqualTo(entry.getValue());
		}
	}

	private static String format(Date date, String format) {
		return new SimpleDateFormat(format).format(date);
	}
}
