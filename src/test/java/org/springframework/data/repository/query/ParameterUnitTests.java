/*
 * Copyright 2017-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

/**
 * Unit tests for {@link Parameter}.
 *
 * @author Jens Schauder
 */
class ParameterUnitTests {

	@Test // DATAJPA-1185
	void classParameterWithSameTypeParameterAsReturnedListIsDynamicProjectionParameter() throws Exception {

		Parameter parameter = new Parameter(getMethodParameter("dynamicProjectionWithList"));

		assertThat(parameter.isDynamicProjectionParameter()).isTrue();
	}

	@Test // DATAJPA-1185
	void classParameterWithSameTypeParameterAsReturnedStreamIsDynamicProjectionParameter() throws Exception {

		Parameter parameter = new Parameter(getMethodParameter("dynamicProjectionWithStream"));

		assertThat(parameter.isDynamicProjectionParameter()).isTrue();
	}

	@NotNull
	private MethodParameter getMethodParameter(String methodName) throws NoSuchMethodException {
		return new MethodParameter( //
				this.getClass().getDeclaredMethod( //
						methodName, //
						Class.class //
				), //
				0);
	}

	<T> List<T> dynamicProjectionWithList(Class<T> type) {
		return Collections.emptyList();
	}

	<T> Stream<T> dynamicProjectionWithStream(Class<T> type) {
		return Stream.empty();
	}
}
