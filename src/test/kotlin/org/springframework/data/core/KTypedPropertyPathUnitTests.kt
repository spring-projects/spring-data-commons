package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort

/**
 * Unit tests for [KPropertyPath] and related functionality.
 *
 * @author Mark Paluch
 */
class KTypedPropertyPathUnitTests {

	@Test
	fun shouldCreatePropertyPath() {

		val path = KTypedPropertyPath.of(Author::name)

		assertThat(path.toDotPath()).isEqualTo("name")

		Sort.by(Book::author, Book::title);
	}

	@Test
	fun shouldComposePropertyPath() {

		val path = KTypedPropertyPath.of(Book::author).then(Author::name)

		assertThat(path.toDotPath()).isEqualTo("author.name")
	}

	@Test
	fun shouldCreateComposed() {

		val path = KTypedPropertyPath.of<Author, String>(Book::author / Author::name)

		assertThat(path.toDotPath()).isEqualTo("author.name")
	}

	class Book(val title: String, val author: Author)
	class Author(val name: String, val books: List<Book>)

}