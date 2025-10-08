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
package org.springframework.data.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Unit tests for {@link TypedPropertyPath}.
 *
 * @author Mark Paluch
 */
class TypedPropertyPathUnitTests {

	@ParameterizedTest // GH-3400
	@MethodSource("propertyPaths")
	void verifyTck(TypedPropertyPath<?, ?> actual, PropertyPath expected) {
		PropertyPathTck.verify(actual, expected);
	}

	static Stream<Arguments.ArgumentSet> propertyPaths() {

		return Stream.of(
				Arguments.argumentSet("PersonQuery.name", PropertyPath.of(PersonQuery::getName),
						PropertyPath.from("name", PersonQuery.class)),
				Arguments.argumentSet("PersonQuery.address.country",
						PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry),
						PropertyPath.from("address.country", PersonQuery.class)),
				Arguments.argumentSet("PersonQuery.address.country.name",
						PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry).then(Country::name),
						PropertyPath.from("address.country.name", PersonQuery.class)),
				Arguments.argumentSet(
						"PersonQuery.emergencyContact.address.country.name", PropertyPath.of(PersonQuery::getEmergencyContact)
								.then(PersonQuery::getAddress).then(Address::getCountry).then(Country::name),
						PropertyPath.from("emergencyContact.address.country.name", PersonQuery.class)));
	}

	@Test // GH-3400
	void resolvesMHSimplePath() {
		assertThat(PropertyPath.of(PersonQuery::getName).toDotPath()).isEqualTo("name");
	}

	@Test // GH-3400
	void resolvesMHComposedPath() {
		assertThat(PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry).toDotPath())
				.isEqualTo("address.country");
	}

	@Test // GH-3400
	void resolvesCollectionPath() {
		assertThat(PropertyPath.ofMany(PersonQuery::getAddresses).then(Address::getCity).toDotPath())
				.isEqualTo("addresses.city");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesInitialLambdaGetter() {
		assertThat(PropertyPath.of((PersonQuery person) -> person.getName()).toDotPath()).isEqualTo("name");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesComposedLambdaGetter() {
		assertThat(PropertyPath.of(PersonQuery::getAddress).then(it -> it.getCity()).toDotPath()).isEqualTo("address.city");
	}

	@Test // GH-3400
	void resolvesComposedLambdaFieldAccess() {
		assertThat(PropertyPath.of(PersonQuery::getAddress).then(it -> it.city).toDotPath()).isEqualTo("address.city");
	}

	@Test // GH-3400
	void resolvesInterfaceMethodReferenceGetter() {
		assertThat(PropertyPath.of(PersonProjection::getName).toDotPath()).isEqualTo("name");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesInterfaceLambdaGetter() {
		assertThat(PropertyPath.of((PersonProjection person) -> person.getName()).toDotPath()).isEqualTo("name");
	}

	@Test // GH-3400
	void resolvesSuperclassMethodReferenceGetter() {
		assertThat(PropertyPath.of(PersonQuery::getTenant).toDotPath()).isEqualTo("tenant");
	}

	@Test // GH-3400
	void resolvesSuperclassLambdaGetter() {
		assertThat(PropertyPath.of((PersonQuery person) -> person.getTenant()).toDotPath()).isEqualTo("tenant");
	}

	@Test // GH-3400
	void resolvesPrivateMethodReference() {
		assertThat(PropertyPath.of(Secret::getSecret).toDotPath()).isEqualTo("secret");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesPrivateMethodLambda() {
		assertThat(PropertyPath.of((Secret secret) -> secret.getSecret()).toDotPath()).isEqualTo("secret");
	}

	@Test // GH-3400
	void switchingOwningTypeFails() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> {
					return ((SuperClass) person).getTenant();
				}));
	}

	@Test // GH-3400
	void constructorCallsShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> new PersonQuery(person)));
	}

	@Test // GH-3400
	void enumShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> TypedPropertyPath.of(NotSupported.INSTANCE));
	}

	@Test // GH-3400
	void returningSomethingShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> TypedPropertyPath.of((TypedPropertyPath<Object, Object>) obj -> null));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> TypedPropertyPath.of((TypedPropertyPath<Object, Object>) obj -> 1));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> TypedPropertyPath.of((TypedPropertyPath<Object, Object>) obj -> ""));
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2Lambda")
	void classImplementationShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> TypedPropertyPath.of(new TypedPropertyPath<Object, Object>() {
					@Override
					public @Nullable Object get(Object obj) {
						return null;
					}
				}));
	}

	@Test // GH-3400
	void constructorMethodReferenceShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.<PersonQuery, PersonQuery> of(PersonQuery::new));
	}

	@Test // GH-3400
	void resolvesMRRecordPath() {

		TypedPropertyPath<PersonQuery, String> then = PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry)
				.then(Country::name);

		assertThat(then.toDotPath()).isEqualTo("address.country.name");
	}

	@Test // GH-3400
	void failsResolutionWith$StrangeStuff() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> {
					int a = 1 + 2;
					new Integer(a).toString();
					return person.getName();
				}).toDotPath());
	}

	@Test // GH-3400
	void arithmeticOpsFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> {
			PropertyPath.of((PersonQuery person) -> {
				int a = 1 + 2;
				return person.getName();
			});
		});
	}

	@Test // GH-3400
	void failsResolvingCallingLocalMethod() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> {
					failsResolutionWith$StrangeStuff();
					return person.getName();
				}));
	}

	@Nested
	class NestedTestClass {

		@Test // GH-3400
		@SuppressWarnings("Convert2MethodRef")
		void resolvesInterfaceLambdaGetter() {
			assertThat(PropertyPath.of((PersonProjection person) -> person.getName()).toDotPath()).isEqualTo("name");
		}

		@Test // GH-3400
		void resolvesSuperclassMethodReferenceGetter() {
			assertThat(PropertyPath.of(PersonQuery::getTenant).toDotPath()).isEqualTo("tenant");
		}

	}

	// Domain entities

	static class SuperClass {

		private int tenant;

		public int getTenant() {
			return tenant;
		}

		public void setTenant(int tenant) {
			this.tenant = tenant;
		}
	}

	static class PersonQuery extends SuperClass {

		private String name;
		private @Nullable Integer age;
		private PersonQuery emergencyContact;
		private Address address;
		private List<Address> addresses;

		public PersonQuery(PersonQuery pq) {}

		public PersonQuery() {}

		// Getters
		public String getName() {
			return name;
		}

		public @Nullable Integer getAge() {
			return age;
		}

		public PersonQuery getEmergencyContact() {
			return emergencyContact;
		}

		public void setEmergencyContact(PersonQuery emergencyContact) {
			this.emergencyContact = emergencyContact;
		}

		public Address getAddress() {
			return address;
		}

		public List<Address> getAddresses() {
			return addresses;
		}

		public void setAddresses(List<Address> addresses) {
			this.addresses = addresses;
		}
	}

	static class Address {

		String street;
		String city;
		private Country country;
		private String secret;

		// Getters
		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}

		public Country getCountry() {
			return country;
		}

		private String getSecret() {
			return secret;
		}

		private void setSecret(String secret) {
			this.secret = secret;
		}
	}

	record Country(String name, String code) {

	}

	static class Secret {

		private String secret;

		private String getSecret() {
			return secret;
		}

	}

	interface PersonProjection {

		String getName();
	}

	enum NotSupported implements TypedPropertyPath<String, String> {

		INSTANCE;

		@Override
		public @Nullable String get(String obj) {
			return "";
		}
	}
}
