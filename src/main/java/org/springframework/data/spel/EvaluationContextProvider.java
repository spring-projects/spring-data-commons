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
package org.springframework.data.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Provides a way to access a centrally defined potentially shared {@link StandardEvaluationContext}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.1
 */
@FunctionalInterface
public interface EvaluationContextProvider {

	/**
	 * A simple default {@link EvaluationContextProvider} returning a {@link StandardEvaluationContext} with the given
	 * root object.
	 */
	EvaluationContextProvider DEFAULT = rootObject -> rootObject == null //
			? new StandardEvaluationContext() //
			: new StandardEvaluationContext(rootObject);

	/**
	 * Return a {@link EvaluationContext} built using the given parameter values.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @return
	 */
	EvaluationContext getEvaluationContext(Object rootObject);

	/**
	 * Return a tailored {@link EvaluationContext} built using the given parameter values and considering
	 * {@link ExpressionDependencies expression dependencies}. The returned {@link EvaluationContext} may contain a
	 * reduced visibility of methods and properties/fields according to the required {@link ExpressionDependencies
	 * expression dependencies}.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @param dependencies the requested expression dependencies to be available.
	 * @return
	 * @since 2.4
	 */
	default EvaluationContext getEvaluationContext(Object rootObject, ExpressionDependencies dependencies) {
		return getEvaluationContext(rootObject);
	}
}
