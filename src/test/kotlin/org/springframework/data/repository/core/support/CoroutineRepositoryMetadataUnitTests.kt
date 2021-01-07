/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.repository.core.support

import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.mapping.Person
import org.springframework.data.repository.Repository
import org.springframework.data.repository.core.RepositoryMetadata
import kotlin.coroutines.Continuation

/**
 * Coroutine unit tests for [RepositoryMetadata].
 *
 * @author Mark Paluch
 */
class CoroutineRepositoryMetadataUnitTests {

	var metadata: RepositoryMetadata = DefaultRepositoryMetadata(MyCoroutineRepository::class.java)

	@Test // DATACMNS-1508
	fun `should consider Flow return type`() {

		val queryMethod = MyCoroutineRepository::class.java.getDeclaredMethod("suspendedQueryMethod", Continuation::class.java)
		assertThat(metadata.getReturnType(queryMethod).type).isEqualTo(Flow::class.java)
	}

	@Test // DATACMNS-1508
	fun `should consider suspended Flow return`() {

		val queryMethod = MyCoroutineRepository::class.java.getDeclaredMethod("queryMethod")
		assertThat(metadata.getReturnType(queryMethod).type).isEqualTo(Flow::class.java)
	}

	interface MyCoroutineRepository : Repository<Person, String> {

		suspend fun suspendedQueryMethod(): Flow<Person>

		fun queryMethod(): Flow<Person>
	}
}
