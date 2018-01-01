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
package org.springframework.data.geo;

import org.springframework.util.Assert;

/**
 * Value object to create custom {@link Metric}s on the fly.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class CustomMetric implements Metric {

	private static final long serialVersionUID = -2972074177454114228L;

	private final double multiplier;
	private final String abbreviation;

	/**
	 * Creates a custom {@link Metric} using the given multiplier.
	 *
	 * @param multiplier
	 */
	public CustomMetric(double multiplier) {

		this(multiplier, "");
	}

	/**
	 * Creates a custom {@link Metric} using the given multiplier and abbreviation.
	 *
	 * @param multiplier
	 * @param abbreviation must not be {@literal null}.
	 */
	public CustomMetric(double multiplier, String abbreviation) {

		Assert.notNull(abbreviation, "Abbreviation must not be null!");

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
