/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Unit tests for {@link MethodParameters}.
 *
 * @author Oliver Gierke
 */
class MethodParametersUnitTests {

	@Test
	void prefersAnnotatedParameterOverDiscovered() throws Exception {

		var method = Sample.class.getMethod("method", String.class, String.class, Object.class);
		var parameters = new MethodParameters(method, Optional.of(new AnnotationAttribute(Qualifier.class)));

		assertThat(parameters.getParameter("param")).isPresent();
		assertThat(parameters.getParameter("foo")).isPresent();
		assertThat(parameters.getParameter("another")).isNotPresent();
	}

	/**
	 * @see #138
	 */
	@Test
	void returnsParametersOfAGivenType() throws Exception {

		var method = Sample.class.getMethod("method", String.class, String.class, Object.class);
		var methodParameters = new MethodParameters(method);

		var objectParameters = methodParameters.getParametersOfType(Object.class);

		assertThat(objectParameters).hasSize(1);
		assertThat(objectParameters.get(0).getParameterIndex()).isEqualTo(2);
	}

	static class Sample {

		public void method(String param, @Qualifier("foo") String another, Object object) {}
	}
}
