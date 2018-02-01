/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.history;

import static org.springframework.data.util.Optionals.*;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.lang.Nullable;

/**
 * Wrapper to contain {@link RevisionMetadata} as well as the revisioned entity.
 *
 * @author Oliver Gierke
 * @author Philipp Huegelmeyer
 * @author Christoph Strobl
 * @author Jens Schauder
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Revision<N extends Number & Comparable<N>, T> implements Comparable<Revision<N, ?>> {

	/**
	 * The {@link RevisionMetadata} for the current {@link Revision}.
	 */
	@NonNull RevisionMetadata<N> metadata;

	/**
	 * The underlying entity.
	 */
	@NonNull T entity;

	/**
	 * Creates a new {@link Revision} for the given {@link RevisionMetadata} and entity.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	public static <N extends Number & Comparable<N>, T> Revision<N, T> of(RevisionMetadata<N> metadata, T entity) {
		return new Revision<>(metadata, entity);
	}

	/**
	 * Returns the revision number of the revision.
	 *
	 * @return the revision number.
	 */
	public Optional<N> getRevisionNumber() {
		return metadata.getRevisionNumber();
	}

	/**
	 * Returns the revision number of the revision, immediately failing on absence.
	 *
	 * @return the revision number.
	 */
	public N getRequiredRevisionNumber() {
		return metadata.getRequiredRevisionNumber();
	}

	/**
	 * Returns the revision date of the revision.
	 *
	 * @return Guaranteed to be not {@literal null}.
	 * @deprecated Use {@link #getRevisionInstant()} instead.
	 */
	@Deprecated
	public Optional<LocalDateTime> getRevisionDate() {
		return metadata.getRevisionDate();
	}

	/**
	 * Returns the timestamp of the revision.
	 *
	 * @return Guaranteed to be not {@literal null}.
	 */
	public Optional<Instant> getRevisionInstant() {
		return metadata.getRevisionInstant();
	}

	/**
	 * Returns the revision date of the revision, immediately failing on absence.
	 *
	 * @return the revision date.
	 * @deprecated Use {@link #getRequiredRevisionInstant()} instead.
	 */
	@Deprecated
	public LocalDateTime getRequiredRevisionDate() {
		return metadata.getRequiredRevisionDate();
	}

	/**
	 * Returns the timestamp of the revision, immediately failing on absence.
	 *
	 * @return the revision {@link Instant}. May be {@literal null}.
	 */
	public Instant getRequiredRevisionInstant() {
		return metadata.getRequiredRevisionInstant();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(@Nullable Revision<N, ?> that) {

		if (that == null) {
			return 1;
		}

		return mapIfAllPresent(getRevisionNumber(), that.getRevisionNumber(), //
				(left, right) -> left.compareTo(right)).orElse(-1);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format("Revision %s of entity %s - Revision metadata %s",
				getRevisionNumber().map(Object::toString).orElse("<unknown>"), entity, metadata);
	}
}
