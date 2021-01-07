/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.history;

import java.time.Instant;
import java.util.Optional;

/**
 * Metadata about a revision.
 *
 * @author Philipp Huegelmeyer
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public interface RevisionMetadata<N extends Number & Comparable<N>> {

	/**
	 * Returns the revision number of the revision.
	 *
	 * @return will never be {@literal null}.
	 */
	Optional<N> getRevisionNumber();

	/**
	 * Returns the revision number of the revision, immediately failing on absence.
	 *
	 * @return will never be {@literal null}.
	 * @throws IllegalStateException if no revision number is available.
	 */
	default N getRequiredRevisionNumber() {

		return getRevisionNumber().orElseThrow(
				() -> new IllegalStateException(String.format("No revision number found on %s!", this.<Object> getDelegate())));
	}

	/**
	 * Returns the timestamp of the revision.
	 *
	 * @return will never be {@literal null}.
	 */
	Optional<Instant> getRevisionInstant();

	/**
	 * Returns the time stamp of the revision, immediately failing on absence.
	 *
	 * @return will never be {@literal null}.
	 * @throws IllegalStateException if no revision date is available.
	 */
	default Instant getRequiredRevisionInstant() {

		return getRevisionInstant().orElseThrow(
				() -> new IllegalStateException(String.format("No revision date found on %s!", this.<Object> getDelegate())));
	}

	/**
	 * Returns the underlying revision metadata which might provider more detailed implementation specific information.
	 *
	 * @return
	 */
	<T> T getDelegate();

	/**
	 * Returns the {@link RevisionType} of this change. If the {@link RevisionType} cannot be determined, this method
	 * returns {@link RevisionType#UNKNOWN}.
	 *
	 * @return will never be {@literal null}.
	 * @since 2.2.0
	 */
	default RevisionType getRevisionType() {
		return RevisionType.UNKNOWN;
	}

	/**
	 * The type of a {@link Revision}.
	 *
	 * @author Jens Schauder
	 * @since 2.2.0
	 */
	enum RevisionType {

		/** Fallback if the type of a revision cannot be determined. */
		UNKNOWN,

		/** Creation of an instance */
		INSERT,

		/** Change of an instance */
		UPDATE,

		/** Deletion of an instance */
		DELETE
	}
}
