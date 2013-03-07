/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.data.auditing.DateTimeProvider;

/**
 * Configuration information for auditing.
 * 
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 */
public interface AnnotationAuditingConfiguration {

	/**
	 * @return References a bean of type AuditorAware to represent the current principal.
	 */
	String getAuditorAwareRef();

	/**
	 * @return Configures whether the creation and modification dates are set and defaults to {@literal true}.
	 */
	boolean isSetDates();

	/**
	 * @return Configures whether the entity shall be marked as modified on creation and defaults to {@literal true}.
	 */
	boolean isModifyOnCreate();

	/**
	 * @return Configures a {@link DateTimeProvider} that allows customizing which DateTime shall be used for setting
	 *         creation and modification dates.
	 */
	String getDateTimeProviderRef();
}
