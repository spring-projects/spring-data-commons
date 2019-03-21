/*
 * Copyright 2008-2019 the original author or authors.
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

import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.data.repository.sample.User

/**
 * Unit tests for CrudRepositoryExtensions.
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class CrudRepositoryExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var repository: CrudRepository<User, String>

	@Test // DATACMNS-1346
	fun `CrudRepository#findByIdOrNull() extension should call its Java counterpart`() {
		repository.findByIdOrNull("foo")
		verify(repository).findById("foo")
	}
}
