/*
 * Copyright 2012 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link RevisionMetadata} implementation that inspects the given object for fields with the configured annotations
 * and returns the field's values on calls to {@link #getRevisionDate()} and {@link #getRevisionNumber()}.
 *
 * @author Oliver Gierke
 */
public class AnnotationRevisionMetadata<N extends Number & Comparable<N>> implements RevisionMetadata<N> {

	private final Object entity;
	private final N revisionNumber;
	private final LocalDateTime revisionDate;

	/**
	 * Creates a new {@link AnnotationRevisionMetadata} inspecting the given entity for the given annotations. If no
	 * annotations will be provided these values will not be looked up from the entity and return {@literal null}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param revisionNumberAnnotation
	 * @param revisionTimeStampAnnotation
	 */
	public AnnotationRevisionMetadata(final Object entity, Class<? extends Annotation> revisionNumberAnnotation,
			Class<? extends Annotation> revisionTimeStampAnnotation) {

		Assert.notNull(entity);
		this.entity = entity;

		if (revisionNumberAnnotation != null) {
			AnnotationDetectionFieldCallback numberCallback = new AnnotationDetectionFieldCallback(revisionNumberAnnotation);
			ReflectionUtils.doWithFields(entity.getClass(), numberCallback);
			this.revisionNumber = numberCallback.getValue(entity);
		} else {
			this.revisionNumber = null;
		}

		if (revisionTimeStampAnnotation != null) {
			AnnotationDetectionFieldCallback revisionCallback = new AnnotationDetectionFieldCallback(
					revisionTimeStampAnnotation);
			ReflectionUtils.doWithFields(entity.getClass(), revisionCallback);
			this.revisionDate = revisionCallback.getValue(entity);
		} else {
			this.revisionDate = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionMetadata#getRevisionNumber()
	 */
	public Optional<N> getRevisionNumber() {
		return Optional.ofNullable(revisionNumber);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.history.RevisionMetadata#getRevisionDate()
	 */
	public Optional<LocalDateTime> getRevisionDate() {
		return Optional.ofNullable(revisionDate);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.history.RevisionMetadata#getDelegate()
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDelegate() {
		return (T) entity;
	}
}
