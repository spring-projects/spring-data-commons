/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Options to be used for access of properties via {@link PersistentPropertyPathAccessor}. They define how to handle
 * intermediate {@literal null} values contained in a path for set operations as well as how to deal with collection or
 * map properties.
 *
 * @author Oliver Gierke
 * @since 2.2
 * @soundtrack The Intersphere - Linger (The Grand Delusion)
 */
@Wither
@AllArgsConstructor
public class AccessOptions {

	/**
	 * How to handle null values
	 *
	 * @author Oliver Gierke
	 */
	public enum NullHandling {

	/**
	 * Reject {@literal null} values detected when traversing a path to eventually set the leaf property. This will cause
	 * a {@link MappingException} being thrown in that case.
	 */
	REJECT,

	/**
	 * Skip setting the value but log an info message to leave a trace why the value wasn't actually set.
	 */
	LOG,

	/**
	 * Silently skip the attempt to set the value.
	 */
	SKIP;
	}

	public enum Propagation {

		/**
		 * Skip the setting of values when encountering a collection or map value within the path to traverse.
		 */
		SKIP,

		/**
		 * Propagate the setting of values when encountering a collection or map value and set it on all collection or map
		 * members.
		 */
		PROPAGATE;
	}

	public static final AccessOptions DEFAULT = new AccessOptions();

	private final @Getter NullHandling nullHandling;
	private final Propagation collectionPropagation, mapPropagation;

	private AccessOptions() {

		this.nullHandling = NullHandling.REJECT;
		this.collectionPropagation = Propagation.PROPAGATE;
		this.mapPropagation = Propagation.PROPAGATE;
	}

	/**
	 * Returns a new {@link AccessOptions} that will cause paths that contain {@literal null} values to be skipped when
	 * setting a property.
	 *
	 * @return
	 */
	public AccessOptions skipNulls() {
		return withNullHandling(NullHandling.SKIP);
	}

	/**
	 * Shortcut to configure the same {@link Propagation} for both collection and map property path segments.
	 *
	 * @param propagation must not be {@literal null}.
	 * @return
	 */
	public AccessOptions withCollectionAndMapPropagation(Propagation propagation) {

		Assert.notNull(propagation, "Propagation must not be null!");

		return withCollectionPropagation(propagation) //
				.withMapPropagation(propagation);
	}

	/**
	 * Returns whether the given property is supposed to be propagated, i.e. if values for it are supposed to be set at
	 * all.
	 *
	 * @param property can be {@literal null}.
	 * @return
	 */
	public boolean propagate(@Nullable PersistentProperty<?> property) {

		if (property == null) {
			return true;
		}

		if (property.isCollectionLike() && collectionPropagation.equals(Propagation.SKIP)) {
			return false;
		}

		if (property.isMap() && mapPropagation.equals(Propagation.SKIP)) {
			return false;
		}

		return true;
	}
}
