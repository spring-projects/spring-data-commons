/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.spel;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Unit tests for {@link ExtensionAwareEvaluationContextProvider}.
 *
 * @author Mark Paluch
 */
class ExtensionAwareEvaluationContextProviderUnitTests {

	@Test // GH-3304
	void shouldRegisterMultipleProvidersCorrectly() {

		ExtensionAwareEvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(
				List.of(FirstExtension.INSTANCE, SecondExtension.INSTANCE));
		StandardEvaluationContext evaluationContext = provider.getEvaluationContext(new GenericModelRoot());

		assertThat(evaluationContext).isNotNull();
	}

	/**
	 * Extension without exposing a concrete extension type.
	 */
	enum FirstExtension implements EvaluationContextExtension {

		INSTANCE;

		@Override
		public String getExtensionId() {
			return "generic1";
		}

		@Override
		public Object getRootObject() {
			return new GenericModelRoot();
		}
	}

	/**
	 * Extension without exposing a concrete extension type.
	 */
	enum SecondExtension implements EvaluationContextExtension {

		INSTANCE;

		@Override
		public String getExtensionId() {
			return "generic2";
		}

		@Override
		public Object getRootObject() {
			return new GenericModelRoot();
		}
	}

	record GenericModelRoot() {
	}

}
