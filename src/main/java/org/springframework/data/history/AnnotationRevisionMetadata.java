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

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link RevisionMetadata} implementation that inspects the given object for fields with the configured annotations
 * and returns the field's values on calls to {@link #getRevisionDate()}, {@link #getRevisionInstant()} and
 * {@link #getRevisionNumber()}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class AnnotationRevisionMetadata<N extends Number & Comparable<N>> implements RevisionMetadata<N> {

	private final Object entity;
	private final Lazy<Optional<N>> revisionNumber;
	private final Lazy<Optional<Object>> revisionDate;
	private final RevisionType revisionType;

	/**
	 * Creates a new {@link AnnotationRevisionMetadata} inspecting the given entity for the given annotations. If no
	 * annotations will be provided these values will not be looked up from the entity and return {@literal null}. The
	 * revisionType will be set to {@literal unknown}
	 *
	 * @param entity must not be {@literal null}.
	 * @param revisionNumberAnnotation must not be {@literal null}.
	 * @param revisionTimeStampAnnotation must not be {@literal null}.
	 */
	public AnnotationRevisionMetadata(Object entity, Class<? extends Annotation> revisionNumberAnnotation,
			Class<? extends Annotation> revisionTimeStampAnnotation) {

		this(entity, revisionNumberAnnotation, revisionTimeStampAnnotation, RevisionType.UNKNOWN);
	}

	/**
	 * Creates a new {@link AnnotationRevisionMetadata} inspecting the given entity for the given annotations. If no
	 * annotations will be provided these values will not be looked up from the entity and return {@literal null}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param revisionNumberAnnotation must not be {@literal null}.
	 * @param revisionTimeStampAnnotation must not be {@literal null}.
	 * @param revisionType must not be {@literal null}.
	 * @since 2.2.0
	 */
	public AnnotationRevisionMetadata(Object entity, Class<? extends Annotation> revisionNumberAnnotation,
			Class<? extends Annotation> revisionTimeStampAnnotation, RevisionType revisionType) {

		Assert.notNull(entity, "Entity must not be null!");
		Assert.notNull(revisionNumberAnnotation, "Revision number annotation must not be null!");
		Assert.notNull(revisionTimeStampAnnotation, "Revision time stamp annotation must not be null!");
		Assert.notNull(revisionType, "Revision Type must not be null!");

		this.entity = entity;
		this.revisionNumber = detectAnnotation(entity, revisionNumberAnnotation);
		this.revisionDate = detectAnnotation(entity, revisionTimeStampAnnotation);
		this.revisionType = revisionType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionMetadata#getRevisionNumber()
	 */
	public Optional<N> getRevisionNumber() {
		return revisionNumber.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionDate()
	 */
	public Optional<Instant> getRevisionInstant() {
		return revisionDate.get().map(AnnotationRevisionMetadata::convertToInstant);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionDate()
	 */
	public RevisionType getRevisionType() {
		return revisionType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionMetadata#getDelegate()
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDelegate() {
		return (T) entity;
	}

	private static <T> Lazy<Optional<T>> detectAnnotation(Object entity, Class<? extends Annotation> annotationType) {

		return Lazy.of(() -> {

			AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(annotationType);
			ReflectionUtils.doWithFields(entity.getClass(), callback);
			return Optional.ofNullable(callback.getValue(entity));
		});
	}

	private static Instant convertToInstant(Object timestamp) {

		if (timestamp instanceof Instant) {
			return (Instant) timestamp;
		}

		if (timestamp instanceof LocalDateTime) {
			return ((LocalDateTime) timestamp).atZone(ZoneOffset.systemDefault()).toInstant();
		}

		if (timestamp instanceof Long) {
			return Instant.ofEpochMilli((Long) timestamp);
		}

		if (Date.class.isInstance(timestamp)) {
			return Date.class.cast(timestamp).toInstant();
		}

		throw new IllegalArgumentException(String.format("Can't convert %s to Instant!", timestamp));
	}
}
