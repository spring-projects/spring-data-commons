/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Callback interface to lookup values for a given {@link Parameter}.
 *
 * @author Oliver Gierke
 */
public interface ParameterValueProvider<P extends PersistentProperty<P>> {

	/**
	 * Returns the value to be used for the given {@link Parameter} (usually when entity instances are created).
	 *
	 * @param parameter must not be {@literal null}.
	 * @return the property value. Can be {@literal null}.
	 */
	<T> @Nullable T getParameterValue(Parameter<T, P> parameter);
}
