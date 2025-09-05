/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactory
import org.springframework.data.repository.sample.User
import java.util.*

/**
 * Unit tests for CrudRepositoryExtensions.
 *
 * @author Sebastien Deleuze
 * @author Mark Paluch
 * @author Subin Kim
 */
class CrudRepositoryExtensionsTests {

    private interface UserRepository : CrudRepository<User, String> {
        override fun findById(id: String): Optional<User>
    }

	var repository = mockk<CrudRepository<User, String>>()

	@Test // DATACMNS-1346
	fun `CrudRepository#findByIdOrNull() extension should call its Java counterpart`() {

		val user = User()

		every { repository.findById("foo") }.returnsMany(Optional.of(user), Optional.empty())

		assertThat(repository.findByIdOrNull("foo")).isEqualTo(user)
		assertThat(repository.findByIdOrNull("foo")).isNull()
		verify(exactly = 2) { repository.findById("foo") }
	}

    @Test // GH-3326
    fun `findByIdOrNull should trigger AOP proxy on overridden method`() {

        val mockTarget = mockk<UserRepository>()
        val user = User()
        every { mockTarget.findById("1") } returns Optional.of(user)

        val factory = ProxyFactory()
        factory.setTarget(mockTarget)
        factory.addInterface(UserRepository::class.java)
        val proxy = factory.proxy as UserRepository

        val result = proxy.findByIdOrNull("1")
        assertThat(result).isEqualTo(user)

        verify(exactly = 1) { mockTarget.findById("1") }
    }
}
