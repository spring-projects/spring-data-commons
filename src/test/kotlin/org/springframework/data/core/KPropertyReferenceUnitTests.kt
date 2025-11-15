package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KPropertyReference] and related functionality.
 *
 * @author Mark Paluch
 */
class KPropertyReferenceUnitTests {

	@Test
	fun shouldCreatePropertyReference() {

		val path = KPropertyReference.of(Person::name)

		assertThat(path.name).isEqualTo("name")
	}

	@Test
	fun shouldComposePropertyPath() {

		val path = KPropertyReference.of(Person::address).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
	}

	@Test
	fun shouldComposeManyPropertyPath() {

		val path = KPropertyReference.of(Person::addresses).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("addresses.city")
	}

	@Test
	fun shouldCreateComposed() {

		val path = KPropertyReference.of(Person::address / Address::city)

		assertThat(path.name).isEqualTo("city")
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
