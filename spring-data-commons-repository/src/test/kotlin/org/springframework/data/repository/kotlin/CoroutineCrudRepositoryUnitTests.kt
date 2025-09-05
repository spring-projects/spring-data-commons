/*
 * Copyright 2019-2025 the original author or authors.
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
import io.mockk.verify
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.reactivestreams.Publisher
import org.springframework.data.repository.core.support.DummyReactiveRepositoryFactory
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.data.repository.sample.User
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Unit tests for Coroutine repositories.
 *
 * @author Mark Paluch
 */
class CoroutineCrudRepositoryUnitTests {

	val backingRepository = mockk<ReactiveCrudRepository<User, String>>()
	lateinit var factory: DummyReactiveRepositoryFactory
	lateinit var coRepository: MyCoRepository
	lateinit var invocationListener: RepositoryMethodInvocationListener

	@BeforeEach
	fun before() {
		factory = DummyReactiveRepositoryFactory(backingRepository)
		invocationListener = Mockito.mock(RepositoryMethodInvocationListener::class.java)
		factory.addInvocationListener(invocationListener)
		coRepository = factory.getRepository(MyCoRepository::class.java)
	}

	@Test // DATACMNS-1508, DATACMNS-1764
	fun shouldInvokeFindAll() {

		val sample = User()

		every { backingRepository.findAll() } returns Flux.just(sample)

		val result = runBlocking {
			coRepository.findAll().toList()
		}

		assertThat(result).hasSize(1).containsOnly(sample)
		Mockito.verify(invocationListener).afterInvocation(Mockito.any())
	}

	@Test // DATACMNS-1508
	fun shouldInvokeFindAllById() {

		every { backingRepository.findAllById(any<Publisher<String>>()) } returns Flux.fromArray(arrayOf(User(), User()))

		val result = runBlocking {
			coRepository.findAllById(flowOf("user-1", "user-2")).toList()
		}

		assertThat(result).hasSize(2)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeSaveAllWhenGivenIterable() {

		val sample = listOf(User(), User())

		every { backingRepository.saveAll(any<Iterable<User>>()) } returns Flux.fromIterable(sample)

		val result = runBlocking {
			coRepository.saveAll(sample).toList()
		}

		assertThat(result).containsExactlyElementsOf(sample)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeSaveAllWhenGivenFlow() {

		val u1 = User()
		val u2 = User()
		val sample = flowOf(u1, u2)

		every { backingRepository.saveAll(any<Publisher<User>>()) } returns Flux.fromArray(arrayOf(u1, u2))

		val result = runBlocking {
			coRepository.saveAll(sample).toList()
		}

		assertThat(result).containsExactly(u1, u2)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeFindById() {

		val sample = User()

		every { backingRepository.findById("foo") } returns Mono.just(sample)

		val result = runBlocking {
			coRepository.findById("foo")
		}

		assertThat(result).isNotNull().isEqualTo(sample)
	}

	@Test // DATACMNS-1508
	fun shouldInvokeExistsById() {

		every { backingRepository.existsById("foo") } returns Mono.just(true)

		val result = runBlocking {
			coRepository.existsById("foo")
		}

		assertThat(result).isTrue()
	}

	@Test // DATACMNS-800
	fun shouldInvokeDeleteAllById() {

		every { backingRepository.deleteAllById(listOf("foo", "bar")) } returns Mono.empty()

		runBlocking {
			coRepository.deleteAllById(listOf("foo", "bar"))
		}

		verify { backingRepository.deleteAllById(listOf("foo", "bar")) }
	}

	@Test // DATACMNS-1508
	fun shouldInvokeDeleteAll() {

		every { backingRepository.deleteAll() } returns Mono.empty()

		runBlocking {
			coRepository.deleteAll()
		}

		verify { backingRepository.deleteAll() }
	}

	@Test // DATACMNS-1508
	fun shouldInvokeDeleteAllWhenGivenIterable() {

		val sample = listOf(User(), User())

		every { backingRepository.deleteAll(any<Iterable<User>>()) } returns Mono.empty()

		runBlocking {
			coRepository.deleteAll(sample)
		}

		verify { backingRepository.deleteAll(sample) }
	}

	@Test // DATACMNS-1508
	fun shouldInvokeDeleteAllWhenGivenFlow() {

		val u1 = User()
		val u2 = User()
		val sample = flowOf(u1, u2)

		every { backingRepository.deleteAll(any<Publisher<User>>()) } returns Mono.empty()

		runBlocking {
			coRepository.deleteAll(sample)
		}

		verify { backingRepository.deleteAll(any<Publisher<User>>()) }
	}

	@Test // DATACMNS-1508, DATACMNS-1764
	fun shouldBridgeQueryMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null, any()))).thenReturn(Mono.just(sample))

		val result = runBlocking {
			coRepository.findOne("foo")
		}

		assertThat(result).isNotNull().isEqualTo(sample)
		val captor = ArgumentCaptor.forClass(RepositoryMethodInvocationListener.RepositoryMethodInvocation::class.java)
		Mockito.verify(invocationListener).afterInvocation(captor.capture())
	}

	@Test // DATACMNS-1508, DATACMNS-1764
	fun shouldBridgeFluxQueryMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null, any()))).thenReturn(Flux.just(sample))

		val result = runBlocking {
			coRepository.findOne("foo")
		}

		assertThat(result).isNotNull().isEqualTo(sample)
		val captor = ArgumentCaptor.forClass(RepositoryMethodInvocationListener.RepositoryMethodInvocation::class.java)
		Mockito.verify(invocationListener).afterInvocation(captor.capture())
	}

	@Test // DATACMNS-1508
	fun shouldBridgeRxJavaQueryMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null, any()))).thenReturn(Mono.just(sample))

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

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null, any()))).thenReturn(Flux.just(sample), Flux.empty<User>())

		val result = runBlocking {
			coRepository.findSuspendedMultiple("foo").toList()
		}

		assertThat(result).hasSize(1).containsOnly(sample)

		val emptyResult = runBlocking {
			coRepository.findSuspendedMultiple("foo").toList()
		}

		assertThat(emptyResult).isEmpty()
	}

	@Test // DATACMNS-1802
	fun shouldBridgeSuspendedAsListMethod() {

		val sample = User()

		Mockito.`when`(factory.queryOne.execute(arrayOf("foo", null, any())))
			.thenReturn(Mono.just(listOf(sample)), Flux.just(sample, sample), Mono.empty<User>())

		val result1 = runBlocking {
			coRepository.findSuspendedAsList("foo")
		}

		assertThat(result1).hasSize(1).containsOnly(sample)

		val result2 = runBlocking {
			coRepository.findSuspendedAsList("foo")
		}

		assertThat(result2).hasSize(2).contains(sample)

		val emptyResult = runBlocking {
			coRepository.findSuspendedAsList("foo")
		}

		assertThat(emptyResult).isNull()
	}

	@Test // DATACMNS-1802
	fun shouldDiscardResult() {

		Mockito.`when`(factory.queryOne.execute(any())).thenReturn(Flux.empty<User>())

		val result = runBlocking {
			coRepository.someDelete("foo")
		}

		assertThat(result).isInstanceOf(Unit.javaClass)
	}

	interface MyCoRepository : CoroutineCrudRepository<User, String> {

		suspend fun findOne(id: String): User

		fun findMultiple(id: String): Flow<User>

		suspend fun findSuspendedMultiple(id: String): Flow<User>

		suspend fun findSuspendedAsList(id: String): List<User>

		suspend fun someDelete(id: String)
	}
}
