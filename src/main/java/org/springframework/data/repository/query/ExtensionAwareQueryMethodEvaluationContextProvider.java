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
package org.springframework.data.repository.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An {@link QueryMethodEvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} instances.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @since 1.9
 */
public class ExtensionAwareQueryMethodEvaluationContextProvider implements QueryMethodEvaluationContextProvider {

	private final ExtensionAwareEvaluationContextProvider delegate;

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup the {@link EvaluationContextExtension}s from, must not
	 *          be {@literal null}.
	 */
	public ExtensionAwareQueryMethodEvaluationContextProvider(ListableBeanFactory beanFactory) {

		Assert.notNull(beanFactory, "ListableBeanFactory must not be null!");

		this.delegate = new ExtensionAwareEvaluationContextProvider(beanFactory);
	}

	/**
	 * Creates a new {@link ExtensionAwareQueryMethodEvaluationContextProvider} using the given
	 * {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ExtensionAwareQueryMethodEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {

		Assert.notNull(extensions, "EvaluationContextExtensions must not be null!");

		this.delegate = new org.springframework.data.spel.ExtensionAwareEvaluationContextProvider(extensions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[])
	 */
	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues) {

		StandardEvaluationContext evaluationContext = delegate.getEvaluationContext(parameterValues);

		evaluationContext.setVariables(collectVariables(parameters, parameterValues));

		return evaluationContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.QueryMethodEvaluationContextProvider#getEvaluationContext(org.springframework.data.repository.query.Parameters, java.lang.Object[], ExpressionDependencies)
	 */
	@Override
	public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(T parameters, Object[] parameterValues,
			ExpressionDependencies dependencies) {

		StandardEvaluationContext evaluationContext = delegate.getEvaluationContext(parameterValues, dependencies);

		evaluationContext.setVariables(collectVariables(parameters, parameterValues));

		return evaluationContext;
	}

	/**
	 * Exposes variables for all named parameters for the given arguments. Also exposes non-bindable parameters under the
	 * names of their types.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param arguments must not be {@literal null}.
	 * @return
	 */
	static Map<String, Object> collectVariables(Parameters<?, ?> parameters, Object[] arguments) {

		Map<String, Object> variables = new HashMap<>();

		parameters.stream()//
				.filter(Parameter::isSpecialParameter)//
				.forEach(it -> variables.put(//
						StringUtils.uncapitalize(it.getType().getSimpleName()), //
						arguments[it.getIndex()]));

		parameters.stream()//
				.filter(Parameter::isNamedParameter)//
				.forEach(it -> variables.put(//
						it.getName().orElseThrow(() -> new IllegalStateException("Should never occur!")), //
						arguments[it.getIndex()]));

		return variables;
	}

}
