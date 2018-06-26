package org.springframework.data.repository

import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.data.repository.sample.User

/**
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class CrudRepositoryExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var repository: CrudRepository<User, String>

	@Test
	fun `CrudRepository#findByIdOrNull() extension should call its Java counterpart`() {
		repository.findByIdOrNull("foo")
		verify(repository).findById("foo")
	}
}
