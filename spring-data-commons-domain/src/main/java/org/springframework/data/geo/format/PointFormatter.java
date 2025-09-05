/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.geo.format;

import java.text.ParseException;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.data.geo.Point;
import org.springframework.format.Formatter;

/**
 * Converter to parse two comma-separated doubles into a {@link Point}.
 *
 * @author Oliver Gierke
 */
public enum PointFormatter implements Converter<String, Point>, Formatter<Point> {

	INSTANCE;

	public static final ConvertiblePair CONVERTIBLE = new ConvertiblePair(String.class, Point.class);

	private static final String INVALID_FORMAT = "Expected two doubles separated by a comma but got '%s'";

	@NonNull
	@Override
	public Point convert(String source) {

		String[] parts = source.split(",");

		if (parts.length != 2) {
			throw new IllegalArgumentException(String.format(INVALID_FORMAT, source));
		}

		try {

			double latitude = Double.parseDouble(parts[0]);
			double longitude = Double.parseDouble(parts[1]);

			return new Point(longitude, latitude);

		} catch (NumberFormatException o_O) {
			throw new IllegalArgumentException(String.format(INVALID_FORMAT, source), o_O);
		}
	}

	@Override
	public String print(@Nullable Point point, Locale locale) {
		return point == null ? "null" : String.format("%s,%s", point.getY(), point.getX());
	}

	@Override
	public Point parse(String text, Locale locale) throws ParseException {
		return convert(text);
	}
}
