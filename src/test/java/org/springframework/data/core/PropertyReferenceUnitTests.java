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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Unit tests for {@link PropertyReference}.
 *
 * @author Mark Paluch
 */
class PropertyReferenceUnitTests {

	@Test // GH-3400
	void resolvesMHSimplePath() {
		assertThat(PropertyReference.of(PersonQuery::getName).getName()).isEqualTo("name");
	}

	@Test // GH-3400
	void resolvesMHComposedPath() {
		assertThat(PropertyReference.of(PersonQuery::getAddress).then(Address::getCountry).toDotPath())
				.isEqualTo("address.country");
	}

	@Test // GH-3400
	void resolvesCollectionPath() {
		assertThat(PropertyReference.ofMany(PersonQuery::getAddresses).then(Address::getCity).toDotPath())
				.isEqualTo("addresses.city");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesInitialLambdaGetter() {
		assertThat(PropertyReference.of((PersonQuery person) -> person.getName()).getName()).isEqualTo("name");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesComposedLambdaGetter() {
		assertThat(PropertyReference.of(PersonQuery::getAddress).then(it -> it.getCity()).toDotPath())
				.isEqualTo("address.city");
	}

	@Test // GH-3400
	void resolvesComposedLambdaFieldAccess() {
		assertThat(PropertyReference.of(PersonQuery::getAddress).then(it -> it.city).toDotPath()).isEqualTo("address.city");
	}

	@Test // GH-3400
	void resolvesInterfaceMethodReferenceGetter() {
		assertThat(PropertyReference.of(PersonProjection::getName).getName()).isEqualTo("name");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesInterfaceLambdaGetter() {
		assertThat(PropertyReference.of((PersonProjection person) -> person.getName()).getName()).isEqualTo("name");
	}

	@Test // GH-3400
	void resolvesSuperclassMethodReferenceGetter() {
		assertThat(PropertyReference.of(PersonQuery::getTenant).getName()).isEqualTo("tenant");
	}

	@Test // GH-3400
	void resolvesSuperclassLambdaGetter() {
		assertThat(PropertyReference.of((PersonQuery person) -> person.getTenant()).getName()).isEqualTo("tenant");
	}

	@Test // GH-3400
	void resolvesPrivateMethodReference() {
		assertThat(PropertyReference.of(Secret::getSecret).getName()).isEqualTo("secret");
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2MethodRef")
	void resolvesPrivateMethodLambda() {
		assertThat(PropertyReference.of((Secret secret) -> secret.getSecret()).getName()).isEqualTo("secret");
	}

	@Test // GH-3400
	void switchingOwningTypeFails() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PersonQuery person) -> {
					return ((SuperClass) person).getTenant();
				}));
	}

	@Test // GH-3400
	void constructorCallsShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PersonQuery person) -> new PersonQuery(person)));
	}

	@Test // GH-3400
	void enumShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of(NotSupported.INSTANCE));
	}

	@Test // GH-3400
	void returningSomethingShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PropertyReference<Object, Object>) obj -> null));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PropertyReference<Object, Object>) obj -> 1));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PropertyReference<Object, Object>) obj -> ""));
	}

	@Test // GH-3400
	@SuppressWarnings("Convert2Lambda")
	void classImplementationShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of(new PropertyReference<Object, Object>() {
					@Override
					public @Nullable Object get(Object obj) {
						return null;
					}
				}));
	}

	@Test // GH-3400
	void constructorMethodReferenceShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.<PersonQuery, PersonQuery> of(PersonQuery::new));
	}

	@Test // GH-3400
	void failsResolutionWith$StrangeStuff() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PersonQuery person) -> {
					int a = 1 + 2;
					new Integer(a).toString();
					return person.getName();
				}).getName());
	}

	@Test // GH-3400
	void arithmeticOpsFail() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> {
			PropertyReference.of((PersonQuery person) -> {
				int a = 1 + 2;
				return person.getName();
			});
		});
	}

	@Test // GH-3400
	void failsResolvingCallingLocalMethod() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyReference.of((PersonQuery person) -> {
					failsResolutionWith$StrangeStuff();
					return person.getName();
				}));
	}

	@Nested
	class NestedTestClass {

		@Test // GH-3400
		@SuppressWarnings("Convert2MethodRef")
		void resolvesInterfaceLambdaGetter() {
			assertThat(PropertyReference.of((PersonProjection person) -> person.getName()).getName()).isEqualTo("name");
		}

		@Test // GH-3400
		void resolvesSuperclassMethodReferenceGetter() {
			assertThat(PropertyReference.of(PersonQuery::getTenant).getName()).isEqualTo("tenant");
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

	enum NotSupported implements PropertyReference<String, String> {

		INSTANCE;

		@Override
		public @Nullable String get(String obj) {
			return "";
		}
	}
}
