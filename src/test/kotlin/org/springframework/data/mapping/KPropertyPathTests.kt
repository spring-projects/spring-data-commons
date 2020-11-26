/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for [KPropertyPath] and its extensions.
 *
 * @author Tjeu Kayim
 * @author Yoann de Martino
 * @author Mark Paluch
 */
class KPropertyPathTests {

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

		val property = asString(AnotherEntity::entity / Entity::book / Book::author / Author::name)

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
	fun `Convert triple nested KProperty to property path using toDotPath`() {

		class Entity(val book: Book)
		class AnotherEntity(val entity: Entity)

		val property =
			(AnotherEntity::entity / Entity::book / Book::author / Author::name).toDotPath()

		assertThat(property).isEqualTo("entity.book.author.name")
	}

	@Test // DATACMNS-1835
	fun `Convert nullable KProperty to field name`() {

		class Cat(val name: String?)
		class Owner(val cat: Cat?)

		val property = asString(Owner::cat / Cat::name)
		assertThat(property).isEqualTo("cat.name")
	}

	class Book(val title: String, val author: Author)
	class Author(val name: String)
}
