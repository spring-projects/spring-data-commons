/*
 * Copyright 2018-present the original author or authors.
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
package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Unit tests for [kotlin.reflect.KProperty] extensions.
 *
 * @author Tjeu Kayim
 * @author Yoann de Martino
 * @author Mark Paluch
 * @author Mikhail Polivakha
 */
class KPropertyExtensionsTests {

	@ParameterizedTest
	@MethodSource("propertyPaths")
	fun verifyTck(actual: TypedPropertyPath<*, *>, expected: PropertyPath) {
		PropertyPathTck.verify(actual, expected)
	}

	companion object {

		@JvmStatic
		fun propertyPaths(): Stream<ArgumentSet> {

			return Stream.of(
				Arguments.argumentSet(
					"Person.name (toPath)",
					Person::name.toPropertyPath(),
					PropertyPath.from("name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name (toPath)",
					(Person::address / Address::country / Country::name).toPropertyPath(),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.addresses.country.name (toPath)",
					(Person::addresses / Address::country / Country::name).toPropertyPath(),
					PropertyPath.from("addresses.country.name", Person::class.java)
				)
			)
		}
	}

	@Test // DATACMNS-1835
	fun `Convert normal KProperty to field name`() {

		val property = Book::title.toDotPath()

		assertThat(property).isEqualTo("title")
	}

	@Test // DATACMNS-1835
	fun `Convert nested KProperty to field name`() {

		val property = (Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("author.name")
	}

	@Test // GH-3010
	fun `Convert from Iterable nested KProperty to field name`() {

		val property = (Author::books / Book::title).toDotPath()

		assertThat(property).isEqualTo("books.title")
	}

	@Test // GH-3010
	fun `Convert from Iterable nested Iterable Property to field name`() {

		val property = (Author::books / Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("books.author.name")
	}

	@Test // DATACMNS-1835
	fun `Convert double nested KProperty to field name`() {

		class Entity(val book: Book)

		val property = (Entity::book / Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("book.author.name")
	}

	@Test // DATACMNS-1835
	fun `Convert triple nested KProperty to field name`() {

		class Entity(val book: Book)
		class AnotherEntity(val entity: Entity)

		val property =
			(AnotherEntity::entity / Entity::book / Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("entity.book.author.name")
	}

	@Test // DATACMNS-1835
	fun `Convert triple nested KProperty to property path using toDotPath`() {

		class Entity(val book: Book)
		class AnotherEntity(val entity: Entity)

		val property =
			(AnotherEntity::entity / Entity::book / Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("entity.book.author.name")
	}

	@Test // DATACMNS-1835
	fun `Convert simple KProperty to property path using toDotPath`() {

		class AnotherEntity(val entity: String)

		val property = AnotherEntity::entity.toDotPath()

		assertThat(property).isEqualTo("entity")
	}

	@Test // DATACMNS-1835
	fun `Convert nested KProperty to field name using toDotPath()`() {

		val property = (Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("author.name")
	}

	@Test // DATACMNS-1835
	fun `Convert nullable KProperty to field name`() {

		class Cat(val name: String?)
		class Owner(val cat: Cat?)

		val property = (Owner::cat / Cat::name).toDotPath()
		assertThat(property).isEqualTo("cat.name")
	}

	class Book(val title: String, val author: Author)
	class Author(val name: String, val books: List<Book>)

	class Person {
		var name: String? = null
		var age: Int = 0
		var address: Address? = null
		var addresses: List<Address> = emptyList()
	}

	class Address {
		var city: String? = null
		var street: String? = null
		var country: Country? = null
	}

	data class Country(val name: String)

}
