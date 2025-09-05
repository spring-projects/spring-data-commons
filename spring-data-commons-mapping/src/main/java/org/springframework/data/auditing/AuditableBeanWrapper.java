/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.auditing;

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * Interface to abstract the ways setting the auditing information can be implemented.
 *
 * @author Oliver Gierke
 * @since 1.5
 */
public interface AuditableBeanWrapper<T> {

	/**
	 * Set the creator of the object.
	 *
	 * @param value
	 */
	@Nullable
	@Contract("null -> null; !null -> !null")
	Object setCreatedBy(@Nullable Object value);

	/**
	 * Set the date the object was created.
	 *
	 * @param value
	 */
	TemporalAccessor setCreatedDate(TemporalAccessor value);

	/**
	 * Set the last modifier of the object.
	 *
	 * @param value
	 */
	@Nullable
	@Contract("null -> null; !null -> !null")
	Object setLastModifiedBy(@Nullable Object value);

	/**
	 * Returns the date of the last modification date of the backing bean.
	 *
	 * @return the date of the last modification.
	 * @since 1.10
	 */
	Optional<TemporalAccessor> getLastModifiedDate();

	/**
	 * Set the last modification date.
	 *
	 * @param value
	 */
	TemporalAccessor setLastModifiedDate(TemporalAccessor value);

	/**
	 * Returns the underlying bean that potentially has been modified by the setter methods exposed. Client code needs to
	 * make sure to call this method to see all the changes applied via this {@link AuditableBeanWrapper}.
	 *
	 * @return will never be {@literal null}.
	 * @since 2.1
	 */
	T getBean();
}
