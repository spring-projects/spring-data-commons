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
package org.springframework.data.repository.kotlin

import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata
import org.springframework.data.repository.sample.User
import org.springframework.data.util.ReflectionUtils
import kotlin.coroutines.Continuation

/**
 * Unit tests for [org.springframework.data.repository.core.RepositoryMetadata].
 *
 * @author Mark Paluch
 */
class CoroutineRepositoryMetadataUnitTests {

	@Test // DATACMNS-1689
	fun shouldDetermineCorrectResultType() {

		val metadata = DefaultRepositoryMetadata(MyCoRepository::class.java)
		val method = ReflectionUtils.findRequiredMethod(MyCoRepository::class.java, "findOne", String::class.java, Continuation::class.java);

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User::class.java)
	}

	@Test // DATACMNS-1689
	fun shouldDetermineCorrectOptionalResultType() {

		val metadata = DefaultRepositoryMetadata(MyCoRepository::class.java)
		val method = ReflectionUtils.findRequiredMethod(MyCoRepository::class.java, "findOneOptional", String::class.java, Continuation::class.java);

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User::class.java)
	}

	@Test // DATACMNS-1689
	fun shouldDetermineCorrectFlowResultType() {

		val metadata = DefaultRepositoryMetadata(MyCoRepository::class.java)
		val method = ReflectionUtils.findRequiredMethod(MyCoRepository::class.java, "findMultiple", String::class.java);

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User::class.java)
	}

	@Test // DATACMNS-1689
	fun shouldDetermineCorrectSuspendedFlowResultType() {

		val metadata = DefaultRepositoryMetadata(MyCoRepository::class.java)
		val method = ReflectionUtils.findRequiredMethod(MyCoRepository::class.java, "findMultipleSuspended", String::class.java, Continuation::class.java);

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User::class.java)
	}

	interface MyCoRepository : CoroutineCrudRepository<User, String> {

		suspend fun findOne(id: String): User

		suspend fun findOneOptional(id: String): User?

		fun findMultiple(id: String): Flow<User>

		suspend fun findMultipleSuspended(id: String): Flow<User>
	}
}
