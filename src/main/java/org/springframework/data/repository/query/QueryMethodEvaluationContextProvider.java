/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.repository.query;

import java.util.function.Supplier;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * Provides a way to access a centrally defined potentially shared {@link StandardEvaluationContext}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.1
 */
public interface QueryMethodEvaluationContextProvider {

	QueryMethodEvaluationContextProvider DEFAULT = ExtensionAwareQueryMethodEvaluationContextProviderFactory.create();

	/**
	 * Returns an {@link EvaluationContext} built using the given {@link ParameterContext}.
	 *
	 * @param parameterContext
	 * @return
	 */
	<T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(ParameterContext<T> parameterContext);

	/**
	 * Interface declaring methods object obtain {@link Parameters} and their actual values for a query method invocation.
	 * 
	 * @author Mark Paluch
	 * @since 2.1
	 */
	interface ParameterContext<T extends Parameters<?, ?>> {

		/**
		 * @return the {@link Parameters}.
		 */
		T getParameters();

		/**
		 * @return the actual parameter values.
		 */
		Object[] getParameterValues();

		/**
		 * Creates a new {@link ParameterContext} given {@link Parameters} and their {@code parameterValues}.
		 *
		 * @param parameters the {@link Parameters} instance obtained from the query method the context is built for.
		 * @param parameterValues must not be {@literal null}.
		 * @return
		 */
		static <T extends Parameters<?, ?>> ParameterContext<T> of(T parameters, Object[] parameterValues) {

			Assert.notNull(parameters, "Parameters must not be null!");
			Assert.notNull(parameterValues, "Parameter values must not be null!");

			return new DefaultParameterContext<>(parameters, parameterValues);
		}

		/**
		 * Creates a new {@link ParameterContext} given {@link Parameters} and a supplier for {@code parameterValues}.
		 *
		 * @param parameters the {@link Parameters} instance obtained from the query method the context is built for.
		 * @param parameterValues must not be {@literal null}.
		 * @return
		 */
		static <T extends Parameters<?, ?>> ParameterContext<T> of(T parameters, Supplier<Object[]> parameterValues) {

			Assert.notNull(parameters, "Parameters must not be null!");
			Assert.notNull(parameterValues, "Parameter values supplier must not be null!");

			return new DefaultParameterContext<>(parameters, parameterValues);
		}
	}
}
