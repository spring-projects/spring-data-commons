package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Kotlin unit tests for [TypedPropertyPath] and related functionality.
 *
 * @author Mark Paluch
 */
class TypedPropertyPathKtUnitTests {

	@Test
	fun shouldSupportPropertyReference() {
		assertThat(TypedPropertyPath.of(Person::address).toDotPath()).isEqualTo("address")
	}

	@Test
	fun shouldSupportComposedPropertyReference() {

		val path = TypedPropertyPath.of<Person, Address>(Person::address).then(Address::city);
		assertThat(path.toDotPath()).isEqualTo("address.city")
	}

	@Test
	fun shouldSupportPropertyLambda() {
		assertThat(TypedPropertyPath.of<Person, Address> { it.address }.toDotPath()).isEqualTo("address")
		assertThat(TypedPropertyPath.of<Person, Address> { it -> it.address }.toDotPath()).isEqualTo("address")
	}

	@Test
	fun shouldSupportComposedPropertyLambda() {

		val path = TypedPropertyPath.of<Person, Address> { it.address };
		assertThat(path.then { it.city }.toDotPath()).isEqualTo("address.city")
	}

	class Person {
		var firstname: String? = null
		var lastname: String? = null
		var age: Int = 0
		var address: Address? = null
	}

	class Address {
		var city: String? = null
		var street: String? = null
		var country: Country? = null
	}

	data class Country(val name: String)
}