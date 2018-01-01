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

import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;

/**
 * Value object capturing some arbitrary object plus a distance.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
@Value
public class GeoResult<T> implements Serializable {

	private static final long serialVersionUID = 1637452570977581370L;

	@NonNull T content;
	@NonNull Distance distance;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("GeoResult [content: %s, distance: %s, ]", content.toString(), distance.toString());
	}
}
