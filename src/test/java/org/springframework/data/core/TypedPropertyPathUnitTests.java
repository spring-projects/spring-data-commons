/*
 * Copyright 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Unit tests for {@link TypedPropertyPath}.
 *
 * @author Mark Paluch
 */
class TypedPropertyPathUnitTests {

	@Test
	void meetsApiContract() {

		TypedPropertyPath<PersonQuery, Country> typed = PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry);

		PropertyPath path = PropertyPath.from("address.country", PersonQuery.class);

		assertThat(typed.hasNext()).isTrue();
		assertThat(path.hasNext()).isTrue();

		assertThat(typed.next().hasNext()).isFalse();
		assertThat(path.next().hasNext()).isFalse();

		assertThat(typed.getType()).isEqualTo(Address.class);
		assertThat(path.getType()).isEqualTo(Address.class);

		assertThat(typed.getSegment()).isEqualTo("address");
		assertThat(path.getSegment()).isEqualTo("address");

		assertThat(typed.getLeafProperty().getType()).isEqualTo(Country.class);
		assertThat(path.getLeafProperty().getType()).isEqualTo(Country.class);
	}

	@Test
	void resolvesMHSimplePath() {
		assertThat(PropertyPath.of(PersonQuery::getName).toDotPath()).isEqualTo("name");
	}

	@Test
	void resolvesMHComposedPath() {
		assertThat(PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry).toDotPath())
				.isEqualTo("address.country");
	}

	@Test
	void resolvesInitialLambdaGetter() {
		assertThat(PropertyPath.of((PersonQuery person) -> person.getName()).toDotPath()).isEqualTo("name");
	}

	@Test
	void resolvesComposedLambdaGetter() {
		assertThat(PropertyPath.of(PersonQuery::getAddress).then(it -> it.getCity()).toDotPath()).isEqualTo("address.city");
	}

	@Test
	void resolvesComposedLambdaFieldAccess() {
		assertThat(PropertyPath.of(PersonQuery::getAddress).then(it -> it.city).toDotPath()).isEqualTo("address.city");
	}

	@Test
	void resolvesInterfaceMethodReferenceGetter() {
		assertThat(PropertyPath.of(PersonProjection::getName).toDotPath()).isEqualTo("name");
	}

	@Test
	void resolvesInterfaceLambdaGetter() {
		assertThat(PropertyPath.of((PersonProjection person) -> person.getName()).toDotPath()).isEqualTo("name");
	}

	@Test
	void resolvesSuperclassMethodReferenceGetter() {
		assertThat(PropertyPath.of(PersonQuery::getTenant).toDotPath()).isEqualTo("tenant");
	}

	@Test
	void resolvesSuperclassLambdaGetter() {
		assertThat(PropertyPath.of((PersonQuery person) -> person.getTenant()).toDotPath()).isEqualTo("tenant");
	}

	@Test
	void resolvesPrivateMethodReference() {
		assertThat(PropertyPath.of(Address::getSecret).toDotPath()).isEqualTo("secret");
	}

	@Test
	void resolvesPrivateMethodLambda() {
		assertThat(PropertyPath.of((Address address) -> address.getSecret()).toDotPath()).isEqualTo("secret");
	}

	@Test
	void switchingOwningTypeFails() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> {
					return ((SuperClass) person).getTenant();
				}));
	}

	@Test
	void constructorCallsShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> new PersonQuery(person)));
	}

	@Test
	void enumShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of(NotSupported.INSTANCE));
	}

	@Test
	void returningSomethingShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((TypedPropertyPath<Object, Object>) obj -> null));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((TypedPropertyPath<Object, Object>) obj -> 1));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((TypedPropertyPath<Object, Object>) obj -> ""));
	}

	@Test
	void classImplementationShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of(new TypedPropertyPath<Object, Object>() {
					@Override
					public @Nullable Object get(Object obj) {
						return null;
					}
				}));
	}

	@Test
	void constructorMethodReferenceShouldFail() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.<PersonQuery, PersonQuery> of(PersonQuery::new));
	}

	@Test
	void resolvesMRRecordPath() {

		TypedPropertyPath<PersonQuery, String> then = PropertyPath.of(PersonQuery::getAddress).then(Address::getCountry)
				.then(Country::name);

		assertThat(then.toDotPath()).isEqualTo("address.country.name");
	}

	@Test
	void failsResolutionWith$StrangeStuff() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> {
					int a = 1 + 2;
					new Integer(a).toString();
					return person.getName();
				}).toDotPath());
	}

	@Test
	void arithmeticOpsFail() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> {
			PropertyPath.of((PersonQuery person) -> {
				int a = 1 + 2;
				return person.getName();
			});
		});
	}

	@Test
	void failsResolvingCallingLocalMethod() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> PropertyPath.of((PersonQuery person) -> {
					failsResolutionWith$StrangeStuff();
					return person.getName();
				}));
	}

	// Domain entities

	static public class SuperClass {
		private int tenant;

		public int getTenant() {
			return tenant;
		}

		public void setTenant(int tenant) {
			this.tenant = tenant;
		}
	}

	static public class PersonQuery extends SuperClass {

		private String name;
		private Integer age;
		private Address address;

		public PersonQuery(PersonQuery pq) {}

		public PersonQuery() {}

		// Getters
		public String getName() {
			return name;
		}

		public Integer getAge() {
			return age;
		}

		public Address getAddress() {
			return address;
		}
	}

	class Address {

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

	interface PersonProjection {

		String getName();
	}

	static class Criteria {

		public Criteria(String key) {

		}

		public static Criteria where(String key) {
			return new Criteria(key);
		}

		public static Criteria where(PropertyPath propertyPath) {
			return new Criteria(propertyPath.toDotPath());
		}

		public static <T, R> Criteria where(TypedPropertyPath<T, R> propertyPath) {
			return new Criteria(propertyPath.toDotPath());
		}
	}

	enum NotSupported implements TypedPropertyPath<String, String> {

		INSTANCE;

		@Override
		public @Nullable String get(String obj) {
			return "";
		}
	}
}
