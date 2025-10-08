package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

/**
 * Kotlin unit tests for [PropertyReference] and related functionality.
 *
 * @author Mark Paluch
 */
class PropertyReferenceKtUnitTests {

	@Test // GH-3400
	fun shouldSupportPropertyReference() {
		assertThat(PropertyReference.property(Person::address).name).isEqualTo("address")
	}

	@Test // GH-3400
	fun resolutionShouldFailForComposedPropertyPath() {
		assertThatIllegalArgumentException()
			.isThrownBy { PropertyReference.property(Person::address / Address::city) }
	}

	class Person {
		var name: String? = null
		var age: Int = 0
		var address: Address? = null
	}

	class Address {
		var city: String? = null
		var street: String? = null
	}

	data class Country(val name: String)

}
