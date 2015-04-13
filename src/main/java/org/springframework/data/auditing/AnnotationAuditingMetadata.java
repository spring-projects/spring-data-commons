/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.auditing;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.data.util.ReflectionUtils.AnnotationFieldFilter;
import org.springframework.util.Assert;

/**
 * Inspects the given {@link Class} for fields annotated by {@link CreatedBy}, {@link CreatedDate},
 * {@link LastModifiedBy} , and {@link LastModifiedDate}. Only one field per annotation is stored.
 * 
 * @author Ranie Jade Ramiso
 * @author Oliver Gierke
 * @since 1.5
 */
final class AnnotationAuditingMetadata {

	private static final AnnotationFieldFilter CREATED_BY_FILTER = new AnnotationFieldFilter(CreatedBy.class);
	private static final AnnotationFieldFilter CREATED_DATE_FILTER = new AnnotationFieldFilter(CreatedDate.class);
	private static final AnnotationFieldFilter LAST_MODIFIED_BY_FILTER = new AnnotationFieldFilter(LastModifiedBy.class);
	private static final AnnotationFieldFilter LAST_MODIFIED_DATE_FILTER = new AnnotationFieldFilter(
			LastModifiedDate.class);

	private static final Map<Class<?>, AnnotationAuditingMetadata> METADATA_CACHE = new ConcurrentHashMap<Class<?>, AnnotationAuditingMetadata>();

	public static final boolean IS_JDK_8 = org.springframework.util.ClassUtils.isPresent("java.time.Clock",
			AnnotationAuditingMetadata.class.getClassLoader());

	static final List<String> SUPPORTED_DATE_TYPES;

	static {

		List<String> types = new ArrayList<String>(5);
		types.add("org.joda.time.DateTime");
		types.add("org.joda.time.LocalDateTime");
		types.add(Date.class.getName());
		types.add(Long.class.getName());
		types.add(long.class.getName());

		SUPPORTED_DATE_TYPES = Collections.unmodifiableList(types);
	}

	private final Field createdByField;
	private final Field createdDateField;
	private final Field lastModifiedByField;
	private final Field lastModifiedDateField;

	/**
	 * Creates a new {@link AnnotationAuditingMetadata} instance for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 */
	private AnnotationAuditingMetadata(Class<?> type) {

		Assert.notNull(type, "Given type must not be null!");

		this.createdByField = ReflectionUtils.findField(type, CREATED_BY_FILTER);
		this.createdDateField = ReflectionUtils.findField(type, CREATED_DATE_FILTER);
		this.lastModifiedByField = ReflectionUtils.findField(type, LAST_MODIFIED_BY_FILTER);
		this.lastModifiedDateField = ReflectionUtils.findField(type, LAST_MODIFIED_DATE_FILTER);

		assertValidDateFieldType(createdDateField);
		assertValidDateFieldType(lastModifiedDateField);
	}

	/**
	 * Checks whether the given field has a type that is a supported date type.
	 * 
	 * @param field can be {@literal null}.
	 */
	private void assertValidDateFieldType(Field field) {

		if (field == null || SUPPORTED_DATE_TYPES.contains(field.getType().getName())) {
			return;
		}

		Class<?> type = field.getType();

		if (Jsr310Converters.supports(type) || ThreeTenBackPortConverters.supports(type)) {
			return;
		}

		throw new IllegalStateException(String.format(
				"Found created/modified date field with type %s but only %s as well as java.time types are supported!", type,
				SUPPORTED_DATE_TYPES));
	}

	/**
	 * Return a {@link AnnotationAuditingMetadata} for the given {@link Class}.
	 * 
	 * @param type the type to inspect, must not be {@literal null}.
	 */
	public static AnnotationAuditingMetadata getMetadata(Class<?> type) {

		if (METADATA_CACHE.containsKey(type)) {
			return METADATA_CACHE.get(type);
		}

		AnnotationAuditingMetadata metadata = new AnnotationAuditingMetadata(type);
		METADATA_CACHE.put(type, metadata);
		return metadata;
	}

	/**
	 * Returns whether the {@link Class} represented in this instance is auditable or not.
	 */
	public boolean isAuditable() {
		if (createdByField == null && createdDateField == null && lastModifiedByField == null
				&& lastModifiedDateField == null) {
			return false;
		}
		return true;
	}

	/**
	 * Return the field annotated by {@link CreatedBy}, or {@literal null}.
	 */
	public Field getCreatedByField() {
		return createdByField;
	}

	/**
	 * Return the field annotated by {@link CreatedDate}, or {@literal null}.
	 */
	public Field getCreatedDateField() {
		return createdDateField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedBy}, or {@literal null}.
	 */
	public Field getLastModifiedByField() {
		return lastModifiedByField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedDate}, or {@literal null}.
	 */
	public Field getLastModifiedDateField() {
		return lastModifiedDateField;
	}
}
