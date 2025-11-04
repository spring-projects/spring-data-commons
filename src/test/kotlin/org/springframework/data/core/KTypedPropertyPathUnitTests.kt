package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.domain.Sort
import java.util.stream.Stream

/**
 * Unit tests for [KPropertyPath] and related functionality.
 *
 * @author Mark Paluch
 */
class KTypedPropertyPathUnitTests {

	@ParameterizedTest
	@MethodSource("propertyPaths")
	fun verifyTck(actual: TypedPropertyPath<*, *>?, expected: PropertyPath) {
		PropertyPathTck.verify(actual, expected)
	}

	companion object {

		@JvmStatic
		fun propertyPaths(): Stream<ArgumentSet> {

			return Stream.of(
				Arguments.argumentSet(
					"Person.name",
					KTypedPropertyPath.of(Person::name),
					PropertyPath.from("name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country",
					KTypedPropertyPath.of<Person, Country>(Person::address / Address::country),
					PropertyPath.from("address.country", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name",
					KTypedPropertyPath.of<Person, String>(Person::address / Address::country / Country::name),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.emergencyContact.address.country.name",
					KTypedPropertyPath.of<Person, String>(Person::emergencyContact / Person::address / Address::country / Country::name),
					PropertyPath.from(
						"emergencyContact.address.country.name",
						Person::class.java
					)
				)
			)
		}
	}

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
	fun shouldComposeManyPropertyPath() {

		val path = KTypedPropertyPath.of(Author::books).then(Book::title)

		assertThat(path.toDotPath()).isEqualTo("books.title")
	}

	@Test
	fun shouldCreateComposed() {

		val path = KTypedPropertyPath.of<Author, String>(Book::author / Author::name)

		assertThat(path.toDotPath()).isEqualTo("author.name")
	}

	class Book(val title: String, val author: Author)
	class Author(val name: String, val books: List<Book>)


	class Person {
		var name: String? = null
		var age: Int = 0
		var address: Address? = null
		var emergencyContact: Person? = null
	}

	class Address {
		var city: String? = null
		var street: String? = null
		var country: Country? = null
	}

	data class Country(val name: String)
}
