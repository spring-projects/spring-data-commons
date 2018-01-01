/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.auditing.config;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Default implementation for {@link AuditingConfiguration}.
 *
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public class AnnotationAuditingConfiguration implements AuditingConfiguration {

	private static final String MISSING_ANNOTATION_ATTRIBUTES = "Couldn't find annotation attributes for %s in %s!";

	private final AnnotationAttributes attributes;

	/**
	 * Creates a new instance of {@link AnnotationAuditingConfiguration} for the given {@link AnnotationMetadata} and
	 * annotation type.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	public AnnotationAuditingConfiguration(AnnotationMetadata metadata, Class<? extends Annotation> annotation) {

		Assert.notNull(metadata, "AnnotationMetadata must not be null!");
		Assert.notNull(annotation, "Annotation must not be null!");

		Map<String, Object> attributesSource = metadata.getAnnotationAttributes(annotation.getName());

		if (attributesSource == null) {
			throw new IllegalArgumentException(String.format(MISSING_ANNOTATION_ATTRIBUTES, annotation, metadata));
		}

		this.attributes = new AnnotationAttributes(attributesSource);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingConfiguration#getAuditorAwareRef()
	 */
	@Override
	public String getAuditorAwareRef() {
		return attributes.getString("auditorAwareRef");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingConfiguration#isSetDates()
	 */
	@Override
	public boolean isSetDates() {
		return attributes.getBoolean("setDates");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingConfiguration#getDateTimeProviderRef()
	 */
	@Override
	public String getDateTimeProviderRef() {
		return attributes.getString("dateTimeProviderRef");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.config.AuditingConfiguration#isModifyOnCreate()
	 */
	@Override
	public boolean isModifyOnCreate() {
		return attributes.getBoolean("modifyOnCreate");
	}
}
