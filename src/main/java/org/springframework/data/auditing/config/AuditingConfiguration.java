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

import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

/**
 * Configuration information for auditing.
 *
 * @author Ranie Jade Ramiso
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public interface AuditingConfiguration {

	/**
	 * Returns the bean name of the {@link AuditorAware} instance to be used..
	 *
	 * @return
	 */
	String getAuditorAwareRef();

	/**
	 * Returns whether the creation and modification dates shall be set. Defaults to {@literal true}.
	 *
	 * @return
	 */
	boolean isSetDates();

	/**
	 * Returns whether the entity shall be marked as modified on creation. Defaults to {@literal true}.
	 *
	 * @return
	 */
	boolean isModifyOnCreate();

	/**
	 * Returns the bean name of the {@link DateTimeProvider} to be used.
	 *
	 * @return
	 */
	String getDateTimeProviderRef();
}
