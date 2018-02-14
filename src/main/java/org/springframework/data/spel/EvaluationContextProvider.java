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
package org.springframework.data.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Provides a way to access a centrally defined potentially shared {@link StandardEvaluationContext}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 2.1
 */
public interface EvaluationContextProvider {

	/**
	 * A simple default {@link EvaluationContextProvider} returning a {@link StandardEvaluationContext} with the given
	 * root object.
	 */
	static EvaluationContextProvider DEFAULT = rootObject -> rootObject == null //
			? new StandardEvaluationContext() //
			: new StandardEvaluationContext(rootObject);

	/**
	 * Returns an {@link EvaluationContext} built using the given parameter values.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @return
	 */
	EvaluationContext getEvaluationContext(Object rootObject);
}
