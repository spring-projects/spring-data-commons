package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
					"Person.name",
					KTypedPropertyPath.of(Person::name),
					PropertyPath.from("name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.name (toPath)",
					Person::name.toPath(),
					PropertyPath.from("name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country",
					KTypedPropertyPath.of(Person::address / Address::country),
					PropertyPath.from("address.country", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name",
					KTypedPropertyPath.of(Person::address / Address::country / Country::name),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name (toPath)",
					(Person::address / Address::country / Country::name).toPath(),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.emergencyContact.address.country.name",
					KTypedPropertyPath.of(Person::emergencyContact / Person::address / Address::country / Country::name),
					PropertyPath.from(
						"emergencyContact.address.country.name",
						Person::class.java
					)
				)
			)
		}
	}

	@Test // GH-3400
	fun shouldCreatePropertyPath() {

		val path = KTypedPropertyPath.of(Person::name)

		assertThat(path.toDotPath()).isEqualTo("name")
	}

	@Test // GH-3400
	fun shouldComposePropertyPath() {

		val path = KTypedPropertyPath.of(Person::address).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
	}

	@Test // GH-3400
	fun shouldComposeManyPropertyPath() {

		val path = KTypedPropertyPath.of(Person::addresses).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("addresses.city")
	}

	@Test // GH-3400
	fun shouldCreateComposed() {

		assertThat(
			PropertyPath.of(Person::address / Address::city).toDotPath()
		).isEqualTo("address.city")

		val path = KTypedPropertyPath.of(Person::address / Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
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
