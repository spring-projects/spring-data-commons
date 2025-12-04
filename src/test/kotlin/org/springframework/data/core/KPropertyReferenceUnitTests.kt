package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

/**
 * Unit tests for [KPropertyReference] and related functionality.
 *
 * @author Mark Paluch
 */
class KPropertyReferenceUnitTests {

	@Test // GH-3400
	fun shouldCreatePropertyReference() {

		val path = KPropertyReference.of(Person::name)

		assertThat(path.name).isEqualTo("name")
	}

	@Test // GH-3400
	fun shouldComposePropertyPath() {

		val path = KPropertyReference.of(Person::address).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("address.city")
	}

	@Test // GH-3400
	fun shouldComposeManyPropertyPath() {

		val path = KPropertyReference.of(Person::addresses).then(Address::city)

		assertThat(path.toDotPath()).isEqualTo("addresses.city")
	}

	@Test // GH-3400
	fun composedReferenceCreationShouldFail() {
		assertThatIllegalArgumentException().isThrownBy {
			PropertyReference.property(
				Person::address / Address::city
			)
		}
		assertThatIllegalArgumentException().isThrownBy { KPropertyReference.of(Person::address / Address::city) }
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
