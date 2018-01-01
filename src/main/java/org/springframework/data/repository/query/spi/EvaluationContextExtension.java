/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.repository.query.spi;

import java.util.Map;

import org.springframework.data.repository.query.ExtensionAwareEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.Nullable;

/**
 * SPI to allow adding a set of properties and function definitions accessible via the root of an
 * {@link EvaluationContext} provided by a {@link ExtensionAwareEvaluationContextProvider}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.9
 */
public interface EvaluationContextExtension {

	/**
	 * Returns the identifier of the extension. The id can be leveraged by users to fully qualify property lookups and
	 * thus overcome ambiguities in case multiple extensions expose properties with the same name.
	 *
	 * @return the extension id, must not be {@literal null}.
	 */
	String getExtensionId();

	/**
	 * Returns the properties exposed by the extension.
	 *
	 * @return the properties
	 */
	Map<String, Object> getProperties();

	/**
	 * Returns the functions exposed by the extension.
	 *
	 * @return the functions
	 */
	Map<String, Function> getFunctions();

	/**
	 * Returns the root object to be exposed by the extension. It's strongly recommended to declare the most concrete type
	 * possible as return type of the implementation method. This will allow us to obtain the necessary metadata once and
	 * not for every evaluation.
	 *
	 * @return
	 */
	@Nullable
	Object getRootObject();
}
