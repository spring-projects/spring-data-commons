/*
 * Copyright 2019 the original author or authors.
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

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.data.repository.core.support.DummyReactiveRepositoryFactory
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.data.repository.sample.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import rx.Observable
import rx.Single

/**
 * Unit tests for Coroutine repositories.
 *
 * @author Mark Paluch
 */
class CoCrudRepositoryUnitTests {

	val backingRepository = mockk<ReactiveCrudRepository<User, String>>()
	lateinit var factory: DummyReactiveRepositoryFactory;
	lateinit var coRepository: MyCoRepository;

	@Before
	fun before() {
		factory = DummyReactiveRepositoryFactory(backingRepository)
		coRepository = factory.getRepository(MyCoRepository::class.java)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeFindAll() {

		val sample = User()

		every { backingRepository.findAll() }.returns(Flux.just(sample))

		val result = runBlocking {
			coRepository.findAll().toList()
		}

		assertThat(result).hasSize(1).containsOnly(sample)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeFindById() {

		val sample = User()

		every { backingRepository.findById("foo") }.returns(Mono.just(sample))

		val result = runBlocking {
			coRepository.findById("foo")
		}

		assertThat(result).isNotNull().isEqualTo(sample)
	}

	@Test // DATACMNS-1508
	fun shouldBridgeQueryMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null))).thenReturn(Mono.just(sample))

		val result = runBlocking {
			coRepository.findOne("foo")
		}

		assertThat(result).isNotNull().isEqualTo(sample)
	}

	@Test // DATACMNS-1508
	fun shouldBridgeRxJavaQueryMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null))).thenReturn(Single.just(sample))

		val result = runBlocking {
			coRepository.findOne("foo")
		}

		assertThat(result).isNotNull().isEqualTo(sample)
	}

	@Test // DATACMNS-1508
	fun shouldBridgeFlowMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo"))).thenReturn(Flux.just(sample), Flux.empty<User>())

		val result = runBlocking {
			coRepository.findMultiple("foo").toList()
		}

		assertThat(result).hasSize(1).containsOnly(sample)

		val emptyResult = runBlocking {
			coRepository.findMultiple("foo").toList()
		}

		assertThat(emptyResult).isEmpty()
	}

	@Test // DATACMNS-1508
	fun shouldBridgeRxJavaToFlowMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo"))).thenReturn(Observable.just(sample))

		val result = runBlocking {
			coRepository.findMultiple("foo").toList()
		}

		assertThat(result).hasSize(1).containsOnly(sample)
	}

	@Test // DATACMNS-1508
	fun shouldBridgeSuspendedFlowMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null))).thenReturn(Flux.just(sample), Flux.empty<User>())

		val result = runBlocking {
			coRepository.findSuspendedMultiple("foo").toList()
		}

		assertThat(result).hasSize(1).containsOnly(sample)

		val emptyResult = runBlocking {
			coRepository.findSuspendedMultiple("foo").toList()
		}

		assertThat(emptyResult).isEmpty()
	}

	interface MyCoRepository : CoCrudRepository<User, String> {

		suspend fun findOne(id: String): User

		fun findMultiple(id: String): Flow<User>

		suspend fun findSuspendedMultiple(id: String): Flow<User>
	}
}
