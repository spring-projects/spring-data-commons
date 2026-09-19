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
package org.springframework.data.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.sample.User
import kotlin.coroutines.Continuation

/**
 * Unit tests for [KotlinReflectionUtils.isSuspend].
 *
 * @author Greg Taube
 */
class KotlinReflectionUtilsSuspendUnitTests {

	@Test // GH-3544
	fun `detects suspend functions`() {

		val method = SuspendingInterface::class.java.getDeclaredMethod("suspending", Continuation::class.java)

		assertThat(KotlinReflectionUtils.isSuspend(method)).isTrue()
	}

	@Test // GH-3544
	fun `considers Continuation parameter of a regular function as non-suspending`() {

		val method = SuspendingInterface::class.java.getDeclaredMethod("continuationParameter", Continuation::class.java)

		assertThat(KotlinReflectionUtils.isSuspend(method)).isFalse()
	}

	@Test // GH-3544
	fun `considers regular functions as non-suspending`() {

		assertThat(KotlinReflectionUtils.isSuspend(SuspendingInterface::class.java.getDeclaredMethod("regular"))).isFalse()
		assertThat(KotlinReflectionUtils.isSuspend(SuspendingInterface::class.java.getDeclaredMethod("withParameter", User::class.java))).isFalse()
	}

	@Test // GH-3544
	fun `considers Java methods as non-suspending`() {

		assertThat(KotlinReflectionUtils.isSuspend(Object::class.java.getDeclaredMethod("toString"))).isFalse()
	}

	interface SuspendingInterface {

		suspend fun suspending(): User

		fun continuationParameter(continuation: Continuation<User>): User

		fun regular(): User

		fun withParameter(user: User): User
	}
}
