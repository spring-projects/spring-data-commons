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
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of {@link EvaluationContextProvider} that always creates a new {@link EvaluationContext}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.9
 */
public enum DefaultEvaluationContextProvider implements EvaluationContextProvider {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.EvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[])
	 */
	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues) {

		return ObjectUtils.isEmpty(parameterValues) ? new StandardEvaluationContext() : new StandardEvaluationContext(
				parameterValues);
	}
}
