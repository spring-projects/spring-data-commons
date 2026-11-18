/*
 * Copyright 2026-present the original author or authors.
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

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.data.repository.core.RepositoryMethodContext
import org.springframework.data.repository.core.support.DummyReactiveRepositoryFactory
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.data.repository.sample.User
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * Unit tests for Kotlin `suspend` default methods on coroutine repositories.
 *
 * @author Mark Paluch
 */
class CoroutineCrudRepositoryDefaultMethodUnitTests {

	val factory =
		DummyReactiveRepositoryFactory(mockk<ReactiveCrudRepository<User, String>>())


	@Test // GH-3578
	fun exposesMethodContextToDefaultMethod() {

		factory.setExposeMetadata(true)
		val repository = factory.getRepository(MetadataRepository::class.java)

		assertThat(runBlocking { repository.repositoryInterface() }).isEqualTo(
			MetadataRepository::class.java
		)

		assertThat(runBlocking { repository.immediate() }).isEqualTo("immediate")
		assertThat(runBlocking { repository.transactional() }).isEqualTo("transactional")
	}

	@Test // GH-3578
	fun shouldSupportTransactionalInterceptors() {

		val transactionManager = mockk<ReactiveTransactionManager> {
			every { getReactiveTransaction(any()) } returns Mono.just(mockk(relaxed = true))
			every { commit(any()) } returns Mono.empty()
			every { rollback(any()) } returns Mono.empty()
		}

		val beanFactory = DefaultListableBeanFactory()
		beanFactory.registerSingleton("transactionManager", transactionManager)
		val factoryBean = object :
			TransactionalRepositoryFactoryBeanSupport<MetadataRepository, User, String>(
				MetadataRepository::class.java
			) {
			override fun doCreateRepositoryFactory() = factory
		}
		factoryBean.setBeanFactory(beanFactory)
		factoryBean.setExposeMetadata(true)
		factoryBean.afterPropertiesSet()

		val repository = factoryBean.getObject()

		assertThat(runBlocking { repository.immediate() }).isEqualTo("immediate")
		verify { transactionManager wasNot Called }

		assertThat(runBlocking { repository.transactional() }).isEqualTo("transactional")
		verifySequence {
			transactionManager.getReactiveTransaction(any())
			transactionManager.commit(any())
		}
	}

	interface MetadataRepository : CoroutineCrudRepository<User, String> {

		suspend fun repositoryInterface(): Class<*> =
			RepositoryMethodContext.getContext().metadata.repositoryInterface

		suspend fun immediate(): String = "immediate"

		@Transactional
		suspend fun transactional(): String = "transactional"
	}

}
