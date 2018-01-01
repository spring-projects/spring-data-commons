/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.projection;

import java.beans.PropertyDescriptor;
import java.util.List;

/**
 * Information about a projection type.
 *
 * @author Oliver Gierke
 * @since 1.12
 */
public interface ProjectionInformation {

	/**
	 * Returns the projection type.
	 *
	 * @return will never be {@literal null}.
	 */
	Class<?> getType();

	/**
	 * Returns the properties that will be consumed by the projection type.
	 *
	 * @return will never be {@literal null}.
	 */
	List<PropertyDescriptor> getInputProperties();

	/**
	 * Returns whether supplying values for the properties returned via {@link #getInputProperties()} is sufficient to
	 * create a working proxy instance. This will usually be used to determine whether the projection uses any dynamically
	 * resolved properties.
	 *
	 * @return
	 */
	boolean isClosed();
}
