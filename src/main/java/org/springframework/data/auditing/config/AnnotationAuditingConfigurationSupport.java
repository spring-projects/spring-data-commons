/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Default implementation for {@link AnnotationAuditingConfiguration}.
 * @author Ranie Jade Ramiso
 */
public class AnnotationAuditingConfigurationSupport implements AnnotationAuditingConfiguration {
	protected AnnotationAttributes attributes;

	public AnnotationAuditingConfigurationSupport(AnnotationMetadata metadata, Class<? extends Annotation> annotation) {
		attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(annotation.getName()));
	}

	public String getAuditorAwareRef() {
		return attributes.getString("auditorAwareRef");
	}

	public boolean isSetDates() {
		return attributes.getBoolean("setDates");
	}

	public String getDateTimeProviderRef() {
		return attributes.getString("dateTimeProviderRef");
	}

	public boolean isModifyOnCreate() {
		return attributes.getBoolean("modifyOnCreate");
	}
}
