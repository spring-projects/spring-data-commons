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

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Unit tests for [KPropertyPath] and related functionality.
 *
 * @author Mark Paluch
 */
class KTypedPropertyPathUnitTests {

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
					"Person.address.country",
					(Person::address / Address::country).toPropertyPath(),
					PropertyPath.from("address.country", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name",
					(Person::address / Address::country / Country::name).toPropertyPath(),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name (toPath)",
					(Person::address / Address::country / Country::name).toPropertyPath(),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.emergencyContact.address.country.name",
					(Person::emergencyContact / Person::address / Address::country / Country::name).toPropertyPath(),
					PropertyPath.from(
						"emergencyContact.address.country.name",
						Person::class.java
					)
				)
			)
		}
	}

	class Person {
		var name: String? = null
		var age: Int = 0
		var address: Address? = null
		var addresses: List<Address> = emptyList()
		var emergencyContact: Person? = null
	}

	class Address {
		var city: String? = null
		var street: String? = null
		var country: Country? = null
	}

	data class Country(val name: String)
}
