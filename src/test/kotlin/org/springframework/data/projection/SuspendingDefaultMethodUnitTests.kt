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
package org.springframework.data.projection

import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Unit tests for Kotlin `suspend` default methods invoked through [DefaultMethodInvokingMethodInterceptor].
 *
 * @author Mark Paluch
 */
class SuspendingDefaultMethodUnitTests {

	val projection =
		ProxyProjectionFactory().createProjection(SuspendingProjection::class.java)

	@Test // GH-3578
	fun returnsResultWithoutSuspending() {
		assertThat(runBlocking { projection.immediate() }).isEqualTo("immediate")
	}

	@Test // GH-3578
	fun returnsResultAfterSuspending() {
		assertThat(runBlocking { projection.suspending() }).isEqualTo("suspending")
	}

	@Test // GH-3578
	fun returnsNullAfterSuspending() {
		assertThat(runBlocking { projection.nothing() }).isNull()
	}

	@Test // GH-3578
	fun propagatesExceptionBeforeSuspending() {
		assertThatIllegalStateException().isThrownBy { runBlocking { projection.failing() } }
			.withMessage("failing")
	}

	@Test // GH-3578
	fun resumesCallerCancelledDuringSuspensionOnce() {

		val failures = CopyOnWriteArrayList<Throwable>()
		val resumptions = CopyOnWriteArrayList<Throwable?>()
		val entered = CompletableFuture<Unit>()
		val gate = CompletableFuture<Unit>()

		val executor = Executors.newSingleThreadExecutor()
		try {

			val scope =
				CoroutineScope(executor.asCoroutineDispatcher() + CoroutineExceptionHandler { _, f -> failures += f })
			val job = scope.launch {
				try {
					projection.await(entered, gate)
					resumptions += null
				} catch (ex: CancellationException) {
					resumptions += ex
					throw ex
				}
			}

			entered.get(5, TimeUnit.SECONDS)
			job.cancel()
			runBlocking { job.join() }

			gate.complete(Unit)
			executor.submit {}
				.get() // single thread: runs after the default method completed
		} finally {
			executor.shutdown()
		}

		assertThat(resumptions).singleElement()
			.isInstanceOf(CancellationException::class.java)
		assertThat(failures).isEmpty()
	}

	interface SuspendingProjection {

		suspend fun immediate(): String = "immediate"

		suspend fun suspending(): String {
			yield()
			return "suspending"
		}

		suspend fun nothing(): String? {
			yield()
			return null
		}

		suspend fun failing(): String = throw IllegalStateException("failing")

		// suspends without checking for cancellation
		suspend fun await(
			entered: CompletableFuture<Unit>,
			gate: CompletableFuture<Unit>
		): String {
			suspendCoroutine { continuation ->
				entered.complete(Unit)
				gate.thenRun { continuation.resume(Unit) }
			}
			return "done"
		}

	}

}
