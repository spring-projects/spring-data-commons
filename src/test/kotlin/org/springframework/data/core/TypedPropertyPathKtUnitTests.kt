package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
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
	fun verifyTck(actual: TypedPropertyPath<*, *>?, expected: PropertyPath) {
		PropertyPathTck.verify(actual, expected)
	}

	companion object {

		@JvmStatic
		fun propertyPaths(): Stream<ArgumentSet> {

			return Stream.of(
				Arguments.argumentSet(
					"Person.name",
					TypedPropertyPath.of(Person::name),
					PropertyPath.from("name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country",
					TypedPropertyPath.of<Person, Address>(Person::address)
						.then(Address::country),
					PropertyPath.from("address.country", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.address.country.name",
					TypedPropertyPath.of<Person, Address>(Person::address)
						.then<Country>(Address::country).then(Country::name),
					PropertyPath.from("address.country.name", Person::class.java)
				),
				Arguments.argumentSet(
					"Person.emergencyContact.address.country.name",
					TypedPropertyPath.of<Person, Person>(Person::emergencyContact)
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
