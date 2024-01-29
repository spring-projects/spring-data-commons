/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentProperty;

/**
 * {@link ParameterValueProvider} that can be used to front a {@link ParameterValueProvider} delegate to prefer a SpEL
 * expression evaluation over directly resolving the parameter value with the delegate.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @deprecated since 3.3, use {@link ValueExpressionParameterValueProvider} instead.
 */
@Deprecated(since = "3.3")
public class SpELExpressionParameterValueProvider<P extends PersistentProperty<P>>
		extends ValueExpressionParameterValueProvider<P> implements ParameterValueProvider<P> {

	public SpELExpressionParameterValueProvider(SpELExpressionEvaluator evaluator, ConversionService conversionService,
			ParameterValueProvider<P> delegate) {

		super(evaluator, conversionService, delegate);
	}

}
