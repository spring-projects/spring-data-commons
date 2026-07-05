/*
 * Copyright 2012-present the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * {@link ParameterValueProvider} that can be used to front a {@link ParameterValueProvider} delegate to prefer a SpEL
 * expression evaluation over directly resolving the parameter value with the delegate.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 3.3
 */
public class ValueExpressionParameterValueProvider<P extends PersistentProperty<P>>
		implements ParameterValueProvider<P> {

	private final ValueExpressionEvaluator evaluator;
	private final ConversionService conversionService;
	private final ParameterValueProvider<P> delegate;

	public ValueExpressionParameterValueProvider(ValueExpressionEvaluator evaluator, ConversionService conversionService,
			ParameterValueProvider<P> delegate) {

		Assert.notNull(evaluator, "ValueExpressionEvaluator must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null");
		Assert.notNull(delegate, "Delegate must not be null");

		this.evaluator = evaluator;
		this.conversionService = conversionService;
		this.delegate = delegate;
	}

	@Override
	@Nullable
	public <T> T getParameterValue(Parameter<T, P> parameter) {

		if (!parameter.hasValueExpression()) {
			return delegate.getParameterValue(parameter);
		}

		// retain compatibility where we accepted bare expressions in @Value
		String rawExpressionString = parameter.getRequiredValueExpression();
		String expressionString = rawExpressionString.contains("#{") || rawExpressionString.contains("${")
				? rawExpressionString
				: "#{" + rawExpressionString + "}";

		Object object = evaluator.evaluate(expressionString);
		return object == null ? null : potentiallyConvertExpressionValue(object, parameter);
	}

	/**
	 * Hook to allow to massage the value resulting from the Spel expression evaluation. Default implementation will
	 * leverage the configured {@link ConversionService} to massage the value into the parameter type.
	 *
	 * @param object the value to massage, will never be {@literal null}.
	 * @param parameter the {@link Parameter} we create the value for
	 * @return the converted parameter value.
	 * @since 3.3
	 */
	@Nullable
	protected <T> T potentiallyConvertExpressionValue(Object object, Parameter<T, P> parameter) {
		return conversionService.convert(object, parameter.getRawType());
	}
}
