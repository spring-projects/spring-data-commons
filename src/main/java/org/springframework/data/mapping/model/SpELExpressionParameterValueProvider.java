/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mapping.model;

import java.util.Optional;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.util.Assert;

/**
 * {@link ParameterValueProvider} that can be used to front a {@link ParameterValueProvider} delegate to prefer a Spel
 * expression evaluation over directly resolving the parameter value with the delegate.
 * 
 * @author Oliver Gierke
 */
public class SpELExpressionParameterValueProvider<P extends PersistentProperty<P>>
		implements ParameterValueProvider<P> {

	private final SpELExpressionEvaluator evaluator;
	private final ParameterValueProvider<P> delegate;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link SpELExpressionParameterValueProvider} using the given {@link SpELExpressionEvaluator},
	 * {@link ConversionService} and {@link ParameterValueProvider} delegate to forward calls to, that resolve parameters
	 * that do not have a Spel expression configured with them.
	 * 
	 * @param evaluator must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 * @param delegate must not be {@literal null}.
	 */
	public SpELExpressionParameterValueProvider(SpELExpressionEvaluator evaluator, ConversionService conversionService,
			ParameterValueProvider<P> delegate) {

		Assert.notNull(evaluator, "SpELExpressionEvaluator must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");
		Assert.notNull(delegate, "ParameterValueProvider delegate must not be null!");

		this.evaluator = evaluator;
		this.conversionService = conversionService;
		this.delegate = delegate;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
	 */
	public <T> Optional<T> getParameterValue(Parameter<T, P> parameter) {

		return parameter.getSpelExpression()//
				.map(it -> Optional.ofNullable(evaluator.evaluate(it))//
						.map(result -> potentiallyConvertSpelValue(result, parameter)))
				.orElseGet(() -> delegate.getParameterValue(parameter));
	}

	/**
	 * Hook to allow to massage the value resulting from the Spel expression evaluation. Default implementation will
	 * leverage the configured {@link ConversionService} to massage the value into the parameter type.
	 * 
	 * @param object the value to massage, will never be {@literal null}.
	 * @param parameter the {@link Parameter} we create the value for
	 * @return
	 */
	protected <T> T potentiallyConvertSpelValue(Object object, Parameter<T, P> parameter) {
		return conversionService.convert(object, parameter.getRawType());
	}
}
