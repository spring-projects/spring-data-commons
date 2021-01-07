/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.spel.spi;

import java.util.Collections;
import java.util.Map;

import org.springframework.expression.EvaluationContext;
import org.springframework.lang.Nullable;

/**
 * SPI to allow adding a set of properties and function definitions accessible via the root of an
 * {@link EvaluationContext} provided by an
 * {@link org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider}.
 * <p>
 * Extensions can be ordered by following Spring's {@link org.springframework.core.Ordered} conventions.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.9
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 */
public interface EvaluationContextExtension extends ExtensionIdAware {

	/**
	 * Return the properties exposed by the extension.
	 *
	 * @return the properties
	 */
	default Map<String, Object> getProperties() {
		return Collections.emptyMap();
	}

	/**
	 * Return the functions exposed by the extension.
	 *
	 * @return the functions
	 */
	default Map<String, Function> getFunctions() {
		return Collections.emptyMap();
	}

	/**
	 * Return the root object to be exposed by the extension. It's strongly recommended to declare the most concrete type
	 * possible as return type of the implementation method. This will allow us to obtain the necessary metadata once and
	 * not for every evaluation.
	 *
	 * @return the root object to be exposed by the extension.
	 */
	@Nullable
	default Object getRootObject() {
		return null;
	}
}
