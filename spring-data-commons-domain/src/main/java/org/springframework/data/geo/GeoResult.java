/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.geo;

import java.io.Serial;
import java.io.Serializable;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object capturing some arbitrary object plus a distance.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public final class GeoResult<T> implements Serializable {

	private static final @Serial long serialVersionUID = 1637452570977581370L;

	private final T content;
	private final Distance distance;

	public GeoResult(T content, Distance distance) {

		Assert.notNull(content, "Content must not be null");
		Assert.notNull(distance, "Distance must not be null");

		this.content = content;
		this.distance = distance;
	}

	public T getContent() {
		return this.content;
	}

	public Distance getDistance() {
		return this.distance;
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof GeoResult<?> geoResult)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(content, geoResult.content)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(distance, geoResult.distance);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(content, distance);
	}

	@Override
	public String toString() {
		return String.format("GeoResult [content: %s, distance: %s]", content, distance);
	}
}
