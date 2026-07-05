/*
 * Copyright 2024-present the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link ValueExpressionParameterValueProvider}.
 *
 * @author Mark Paluch
 */
class ValueExpressionParameterValueProviderUnitTests {

	CachingValueExpressionEvaluatorFactory factory = new CachingValueExpressionEvaluatorFactory(
			new SpelExpressionParser(), StandardEnvironment::new, rootObject -> {

				SpELContext spELContext = new SpELContext(new MapAccessor());
				return spELContext.getEvaluationContext(rootObject);
			});

	ValueExpressionParameterValueProvider<SamplePersistentProperty> provider = new ValueExpressionParameterValueProvider<>(
			factory.create(Map.of("name", "Walter")), DefaultConversionService.getSharedInstance(),
			new ParameterValueProvider<SamplePersistentProperty>() {
				@Override
				public <T> T getParameterValue(Parameter<T, SamplePersistentProperty> parameter) {
					return null;
				}
			});

	@Test // GH-2369
	void considersValueCompatibilityFormat() {

		Annotation[] annotations = CompatibilityConstructor.class.getConstructors()[0].getParameterAnnotations()[0];
		String name = provider
				.getParameterValue(new Parameter<>("name", TypeInformation.of(String.class), annotations, null));

		assertThat(name).isEqualTo("Walter");
	}

	@Test // GH-2369
	void considersValueTemplateFormat() {

		Annotation[] annotations = TemplateConstructor.class.getConstructors()[0].getParameterAnnotations()[0];
		String name = provider
				.getParameterValue(new Parameter<>("name", TypeInformation.of(String.class), annotations, null));

		assertThat(name).isEqualTo("Walter");
	}

	static class CompatibilityConstructor {

		public CompatibilityConstructor(@Value("#root.name") String name) {}

	}

	static class TemplateConstructor {

		public TemplateConstructor(@Value("#{#root.name}") String name) {}

	}

}
