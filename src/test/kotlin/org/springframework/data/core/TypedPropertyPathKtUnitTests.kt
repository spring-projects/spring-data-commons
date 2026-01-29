/*
 * Copyright 2025-present the original author or authors.
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Kotlin unit tests for [TypedPropertyPath] and related functionality.
 *
 * @author Mark Paluch
 */
class TypedPropertyPathKtUnitTests {

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
					"Person.name",
					TypedPropertyPath.path(Person::name),
					PropertyPath.from("name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country",
					TypedPropertyPath.path<Person, Address>(Person::address)
						.then(Address::country),
					PropertyPath.from("address.country", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name",
					TypedPropertyPath.path<Person, Address>(Person::address)
						.then<Country>(Address::country).then(Country::name),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.emergencyContact.address.country.name",
					TypedPropertyPath.path<Person, Person>(Person::emergencyContact)
						.then<Address>(Person::address).then<Country>(Address::country)
						.then(Country::name),
					PropertyPath.from(
						"emergencyContact.address.country.name",
						Person::class.java
					)
				)
			)
		}
	}

	@Test // GH-3400
	fun shouldSupportPropertyReference() {

		assertThat(
			TypedPropertyPath.path(Person::address).toDotPath()
		).isEqualTo("address")
	}

	@Test // GH-3400
	fun shouldSupportComposedPropertyReference() {

		val path = TypedPropertyPath.path<Person, Address>(Person::address)
			.then(Address::city);
		assertThat(path.toDotPath()).isEqualTo("address.city")
	}

	@Test // GH-3400
	@Disabled("https://github.com/spring-projects/spring-data-commons/issues/3451")
	fun shouldSupportPropertyLambda() {
		assertThat(TypedPropertyPath.path<Person, Address> { it.address }
			.toDotPath()).isEqualTo("address")
		assertThat(TypedPropertyPath.path<Person, Address> { foo -> foo.address }
			.toDotPath()).isEqualTo("address")
	}

	@Test // GH-3400
	@Disabled()
	fun shouldSupportComposedPropertyLambda() {

		val path = TypedPropertyPath.path<Person, Address> { it.address };
		assertThat(path.then { it.city }.toDotPath()).isEqualTo("address.city")
	}

	@Test // GH-3400
	fun shouldSupportComposedKProperty() {

		val path = TypedPropertyPath.path(Person::address / Address::city);
		assertThat(path.toDotPath()).isEqualTo("address.city")

		val otherPath = TypedPropertyPath.of(Person::address / Address::city);
		assertThat(otherPath.toDotPath()).isEqualTo("address.city")
	}

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
