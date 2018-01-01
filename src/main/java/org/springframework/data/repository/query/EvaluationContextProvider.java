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
package org.springframework.data.repository.query;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Provides a way to access a centrally defined potentially shared {@link StandardEvaluationContext}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.9
 */
public interface EvaluationContextProvider {

	/**
	 * Returns an {@link EvaluationContext} built using the given {@link Parameters} and parameter values.
	 *
	 * @param parameters the {@link Parameters} instance obtained from the query method the context is built for.
	 * @param parameterValues the values for the parameters.
	 * @return
	 */
	<T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues);
}
