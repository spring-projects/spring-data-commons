/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.repository.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import kotlin.reflect.jvm.javaMethod

/**
 * Unit tests for [Parameter].
 *
 * @author Mark Paluch
 */
class ParameterUnitTests {

	@Test // DATACMNS-1508
	fun `should consider Continuation a special parameter`() {

		val methodParameter = MethodParameter(MyCoroutineRepository::hello.javaMethod, 0)
		methodParameter.initParameterNameDiscovery(DefaultParameterNameDiscoverer())
		val parameter = Parameter(methodParameter)

		assertThat(parameter.name).isEmpty()
		assertThat(parameter.isBindable).isFalse()
	}

	interface MyCoroutineRepository {
		suspend fun hello(): Any
	}
}
