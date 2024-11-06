/*
 * Copyright 2012-2024 the original author or authors.
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

import java.lang.reflect.Field;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.data.util.ReflectionUtils.AnnotationFieldFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Inspects the given {@link Class} for fields annotated by {@link CreatedBy}, {@link CreatedDate},
 * {@link LastModifiedBy} , and {@link LastModifiedDate}. Only one field per annotation is stored.
 *
 * @author Ranie Jade Ramiso
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Johannes Englmeier
 * @since 1.5
 */
final class AnnotationAuditingMetadata {

	private static final AnnotationFieldFilter CREATED_BY_FILTER = new AnnotationFieldFilter(CreatedBy.class);
	private static final AnnotationFieldFilter CREATED_DATE_FILTER = new AnnotationFieldFilter(CreatedDate.class);
	private static final AnnotationFieldFilter LAST_MODIFIED_BY_FILTER = new AnnotationFieldFilter(LastModifiedBy.class);
	private static final AnnotationFieldFilter LAST_MODIFIED_DATE_FILTER = new AnnotationFieldFilter(
			LastModifiedDate.class);

	static final List<String> SUPPORTED_DATE_TYPES;

	static {

		List<String> types = new ArrayList<>(Jsr310Converters.getSupportedClasses() //
				.stream() //
				.filter(TemporalAccessor.class::isAssignableFrom) //
				.map(Class::getName) //
				.toList());

		types.add(Date.class.getName());
		types.add(Long.class.getName());
		types.add(long.class.getName());

		SUPPORTED_DATE_TYPES = Collections.unmodifiableList(types);
	}

	private final @Nullable Field createdByField;
	private final @Nullable Field createdDateField;
	private final @Nullable Field lastModifiedByField;
	private final @Nullable Field lastModifiedDateField;

	/**
	 * Creates a new {@link AnnotationAuditingMetadata} instance for the given type.
	 *
	 * @param type must not be {@literal null}.
	 */
	private AnnotationAuditingMetadata(Class<?> type) {

		Assert.notNull(type, "Given type must not be null");

		this.createdByField = ReflectionUtils.findField(type, CREATED_BY_FILTER);
		this.createdDateField = ReflectionUtils.findField(type, CREATED_DATE_FILTER);
		this.lastModifiedByField = ReflectionUtils.findField(type, LAST_MODIFIED_BY_FILTER);
		this.lastModifiedDateField = ReflectionUtils.findField(type, LAST_MODIFIED_DATE_FILTER);

		assertValidDateFieldType(createdDateField);
		assertValidDateFieldType(lastModifiedDateField);
	}

	/**
	 * Checks whether the given field has a type that is a supported date type.
	 */
	private void assertValidDateFieldType(@Nullable Field field) {

		if (field == null) {
			return;
		}

		if (SUPPORTED_DATE_TYPES.contains(field.getType().getName())) {
			return;
		}

		Class<?> type = field.getType();

			if (TemporalAccessor.class.isAssignableFrom(type)) {
				return;
			}

			throw new IllegalStateException(String.format(
					"Found created/modified date field with type %s but only %s as well as java.time types are supported", type,
					SUPPORTED_DATE_TYPES));
	}

	/**
	 * Return a {@link AnnotationAuditingMetadata} for the given {@link Class}.
	 *
	 * @param type the type to inspect, must not be {@literal null}.
	 */
	public static AnnotationAuditingMetadata getMetadata(Class<?> type) {
		return new AnnotationAuditingMetadata(type);
	}

	/**
	 * Returns whether the {@link Class} represented in this instance is auditable or not.
	 */
	public boolean isAuditable() {
		return createdByField != null || createdDateField != null || lastModifiedByField != null
				|| lastModifiedDateField != null;
	}

	/**
	 * Return the field annotated by {@link CreatedBy}.
	 */
	@Nullable
	public Field getCreatedByField() {
		return createdByField;
	}

	/**
	 * Return the field annotated by {@link CreatedDate}.
	 */
	@Nullable
	public Field getCreatedDateField() {
		return createdDateField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedBy}.
	 */
	@Nullable
	public Field getLastModifiedByField() {
		return lastModifiedByField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedDate}.
	 */
	public @Nullable Field getLastModifiedDateField() {
		return lastModifiedDateField;
	}
}
