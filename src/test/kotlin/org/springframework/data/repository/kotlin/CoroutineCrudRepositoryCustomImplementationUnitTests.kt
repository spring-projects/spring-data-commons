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
package org.springframework.data.repository.kotlin

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.core.RepositoryMetadata
import org.springframework.data.repository.core.support.DummyReactiveRepositoryFactory
import org.springframework.data.repository.core.support.RepositoryComposition
import org.springframework.data.repository.core.support.RepositoryFragment
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.data.repository.sample.User

/**
 * Unit tests for Coroutine repositories.
 *
 * @author Mark Paluch
 */
class CoroutineCrudRepositoryCustomImplementationUnitTests {

	val backingRepository = mockk<ReactiveCrudRepository<User, String>>()
	lateinit var factory: DummyReactiveRepositoryFactory
	lateinit var coRepository: MyCoRepository

	@BeforeEach
	fun before() {
		factory = CustomDummyReactiveRepositoryFactory(backingRepository)
		coRepository = factory.getRepository(MyCoRepository::class.java)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeFindAll() {

		val result = runBlocking {
			coRepository.findOne("foo")
		}

		assertThat(result).isNotNull()
	}

	class CustomDummyReactiveRepositoryFactory(repository: Any) : DummyReactiveRepositoryFactory(repository) {

		override fun getRepositoryFragments(metadata: RepositoryMetadata): RepositoryComposition.RepositoryFragments {
			return super.getRepositoryFragments(metadata).append(RepositoryFragment.implemented(MyCustomCoRepository::class.java, MyCustomCoRepositoryImpl()))
		}
	}

	interface MyCoRepository : CoroutineCrudRepository<User, String>, MyCustomCoRepository

	interface MyCustomCoRepository {

		suspend fun findOne(id: String): User
	}

	class MyCustomCoRepositoryImpl : MyCustomCoRepository {

		override suspend fun findOne(id: String): User {
			return User()
		}
	}
}
