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
package org.springframework.data.geo.format;

import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.format.Formatter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Converter to create {@link Distance} instances from {@link String} representations. The supported format is a decimal
 * followed by whitespace and a metric abbreviation. We currently support the following abbreviations:
 * {@value #SUPPORTED_METRICS}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public enum DistanceFormatter implements Converter<String, Distance>, Formatter<Distance> {

	INSTANCE;

	private static final Map<String, Metric> SUPPORTED_METRICS;
	private static final String INVALID_DISTANCE = "Expected double amount optionally followed by a metrics abbreviation (%s) but got '%s'!";

	static {

		Map<String, Metric> metrics = new LinkedHashMap<>();

		for (Metric metric : Metrics.values()) {
			metrics.put(metric.getAbbreviation(), metric);
			metrics.put(metric.toString().toLowerCase(Locale.US), metric);
		}

		SUPPORTED_METRICS = Collections.unmodifiableMap(metrics);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Nullable
	@Override
	public final Distance convert(String source) {
		return source == null ? null : doConvert(source.trim().toLowerCase(Locale.US));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.format.Printer#print(java.lang.Object, java.util.Locale)
	 */
	@Override
	public String print(Distance distance, Locale locale) {
		return distance == null ? null : String.format("%s%s", distance.getValue(), distance.getUnit().toLowerCase(locale));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.format.Parser#parse(java.lang.String, java.util.Locale)
	 */
	@Override
	public Distance parse(String text, Locale locale) throws ParseException {
		return doConvert(text.trim().toLowerCase(locale));
	}

	/**
	 * Converts the given {@link String} source into a distance. Expects the source to reflect the {@link Metric} as held
	 * in the {@link #SUPPORTED_METRICS} map.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static Distance doConvert(String source) {

		for (Entry<String, Metric> metric : SUPPORTED_METRICS.entrySet()) {
			if (source.endsWith(metric.getKey())) {
				return fromString(source, metric);
			}
		}

		try {
			return new Distance(Double.parseDouble(source));
		} catch (NumberFormatException o_O) {
			throw new IllegalArgumentException(String.format(INVALID_DISTANCE,
					StringUtils.collectionToCommaDelimitedString(SUPPORTED_METRICS.keySet()), source));
		}
	}

	/**
	 * Creates a {@link Distance} from the given source String and the {@link Metric} detected.
	 *
	 * @param source the raw source {@link String}, must not be {@literal null} or empty.
	 * @param metric the {@link Metric} detected keyed by the keyword it was detected for, must not be {@literal null}.
	 * @return
	 */
	private static Distance fromString(String source, Entry<String, Metric> metric) {

		String amountString = source.substring(0, source.indexOf(metric.getKey()));

		try {
			return new Distance(Double.parseDouble(amountString), metric.getValue());
		} catch (NumberFormatException o_O) {
			throw new IllegalArgumentException(String.format(INVALID_DISTANCE,
					StringUtils.collectionToCommaDelimitedString(SUPPORTED_METRICS.keySet()), source));
		}
	}
}
