/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.geo;

/**
 * Commonly used {@link Metric}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public enum Metrics implements Metric {

	KILOMETERS(6378.137, "km"), MILES(3963.191, "mi"), NEUTRAL(1, "");

	private final double multiplier;
	private final String abbreviation;

	/**
	 * Creates a new {@link Metrics} using the given multiplier.
	 *
	 * @param multiplier the earth radius at equator, must not be {@literal null}.
	 * @param abbreviation the abbreviation to use for this {@link Metric}, must not be {@literal null}.
	 */
	private Metrics(double multiplier, String abbreviation) {

		this.multiplier = multiplier;
		this.abbreviation = abbreviation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mongodb.core.geo.Metric#getMultiplier()
	 */
	public double getMultiplier() {
		return multiplier;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.geo.Metric#getAbbreviation()
	 */
	@Override
	public String getAbbreviation() {
		return abbreviation;
	}
}
