/*
 * Copyright 2012-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.AbstractPersistentPropertyUnitTests.SamplePersistentProperty;

/**
 * Unit tests for {@link SpELExpressionParameterValueProvider}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpelExpressionParameterProviderUnitTests {

	@Mock SpELExpressionEvaluator evaluator;
	@Mock ParameterValueProvider<SamplePersistentProperty> delegate;
	@Mock ConversionService conversionService;

	private SpELExpressionParameterValueProvider<SamplePersistentProperty> provider;

	private Parameter<Object, SamplePersistentProperty> parameter;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		provider = new SpELExpressionParameterValueProvider<>(evaluator, conversionService, delegate);

		parameter = mock(Parameter.class);
		when(parameter.hasSpelExpression()).thenReturn(true);
		when(parameter.getRawType()).thenReturn(Object.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void delegatesIfParameterDoesNotHaveASpELExpression() {

		Parameter<Object, SamplePersistentProperty> parameter = mock(Parameter.class);
		when(parameter.hasSpelExpression()).thenReturn(false);

		provider.getParameterValue(parameter);
		verify(delegate, times(1)).getParameterValue(parameter);
		verify(evaluator, times(0)).evaluate("expression");
	}

	@Test
	void evaluatesSpELExpression() {

		when(parameter.getSpelExpression()).thenReturn("expression");

		provider.getParameterValue(parameter);
		verify(delegate, times(0)).getParameterValue(parameter);
		verify(evaluator, times(1)).evaluate("expression");
	}

	@Test
	void handsSpELValueToConversionService() {

		doReturn("source").when(parameter).getSpelExpression();
		doReturn("value").when(evaluator).evaluate(any());

		provider.getParameterValue(parameter);

		verify(delegate, times(0)).getParameterValue(parameter);
		verify(conversionService, times(1)).convert("value", Object.class);
	}

	@Test
	void doesNotConvertNullValue() {

		doReturn("source").when(parameter).getSpelExpression();
		doReturn(null).when(evaluator).evaluate(any());

		provider.getParameterValue(parameter);

		verify(delegate, times(0)).getParameterValue(parameter);
		verify(conversionService, times(0)).convert("value", Object.class);
	}

	@Test
	void returnsMassagedObjectOnOverride() {

		provider = new SpELExpressionParameterValueProvider<SamplePersistentProperty>(evaluator, conversionService,
				delegate) {

			@Override
			@SuppressWarnings("unchecked")
			protected <T> T potentiallyConvertSpelValue(Object object, Parameter<T, SamplePersistentProperty> parameter) {
				return (T) "FOO";
			}
		};

		doReturn("source").when(parameter).getSpelExpression();
		doReturn("value").when(evaluator).evaluate(any());

		assertThat(provider.getParameterValue(parameter)).isEqualTo("FOO");

		verify(delegate, times(0)).getParameterValue(parameter);
	}
}
