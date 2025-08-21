/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;

import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.util.ObjectUtils;

import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * Unit tests for {@link JsonProjectingMethodInterceptorFactory}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.13
 * @soundtrack Richard Spaven - Assemble (Whole Other*)
 */
class JsonProjectingMethodInterceptorFactoryUnitTests {

	ProjectionFactory projectionFactory;
	Customer customer;

	@BeforeEach
	void setUp() {

		var json = "{\"firstname\" : \"Dave\", "//
				+ "\"address\" : { \"zipCode\" : \"01097\", \"city\" : \"Dresden\" }," //
				+ "\"addresses\" : [ { \"zipCode\" : \"01097\", \"city\" : \"Dresden\" }, { \"zipCode\" : \"69469\", \"city\" : \"Weinheim\" }]"
				+ " }";

		var projectionFactory = new SpelAwareProxyProjectionFactory();

		var objectMapper = new ObjectMapper();
		MappingProvider mappingProvider = new ProjectingJacksonHttpMessageConverter.JacksonMappingProvider(objectMapper);
		JsonProvider jsonProvider = new ProjectingJacksonHttpMessageConverter.JacksonJsonProvider(objectMapper);
		projectionFactory
				.registerMethodInvokerFactory(new JsonProjectingMethodInterceptorFactory(jsonProvider, mappingProvider));

		this.projectionFactory = projectionFactory;
		this.customer = projectionFactory.createProjection(Customer.class, new ByteArrayInputStream(json.getBytes()));
	}

	@Test // DATCMNS-885
	void accessSimpleProperty() {
		assertThat(customer.getFirstname()).isEqualTo("Dave");
	}

	@Test // DATCMNS-885
	void accessPropertyWithExplicitAnnotation() {
		assertThat(customer.getBar()).isEqualTo("Dave");
	}

	@Test // DATCMNS-885
	void accessPropertyWithComplexReturnType() {
		assertThat(customer.getAddress()).isEqualTo(new Address("01097", "Dresden"));
	}

	@Test // DATCMNS-885
	void accessComplexPropertyWithProjection() {
		assertThat(customer.getAddressProjection().getCity()).isEqualTo("Dresden");
	}

	@Test // DATCMNS-885
	void accessPropertyWithNestedJsonPath() {
		assertThat(customer.getNestedZipCode()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessCollectionProperty() {
		assertThat(customer.getAddresses().get(0)).isEqualTo(new Address("01097", "Dresden"));
	}

	@Test // DATCMNS-885
	void accessPropertyOnNestedProjection() {
		assertThat(customer.getAddressProjections().get(0).getZipCode()).isEqualTo("01097");
	}

	@Test // gh-2270
	void nestedProjectionCollectionShouldContainMultipleElements() {
		assertThat(customer.getAddressProjections()).hasSize(2);
		assertThat(customer.getAddressProjections().get(0).getZipCode()).isEqualTo("01097");
		assertThat(customer.getAddressProjections().get(1).getZipCode()).isEqualTo("69469");
	}

	@Test // DATCMNS-885
	void accessPropertyThatUsesJsonPathProjectionInTurn() {
		assertThat(customer.getAnotherAddressProjection().getZipCodeButNotCity()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessCollectionPropertyThatUsesJsonPathProjectionInTurn() {

		var projections = customer.getAnotherAddressProjections();

		assertThat(projections).hasSize(2);
		assertThat(projections.get(0).getZipCodeButNotCity()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessAsCollectionPropertyThatUsesJsonPathProjectionInTurn() {

		var projections = customer.getAnotherAddressProjectionAsCollection();

		assertThat(projections).hasSize(1);
		assertThat(projections.iterator().next().getZipCodeButNotCity()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessNestedPropertyButStayOnRootLevel() {

		var name = customer.getName();

		assertThat(name).isNotNull();
		assertThat(name.getFirstname()).isEqualTo("Dave");
	}

	@Test // DATACMNS-885
	void accessNestedFields() {

		assertThat(customer.getNestedCity()).isEqualTo("Dresden");
		assertThat(customer.getNestedCities()).hasSize(3);
	}

	@Test // DATACMNS-1144
	void returnsNullForNonExistantValue() {
		assertThat(customer.getName().getLastname()).isNull();
	}

	@Test // DATACMNS-1144
	void triesMultipleDeclaredPathsIfNotAvailable() {
		assertThat(customer.getName().getSomeName()).isEqualTo(customer.getName().getFirstname());
	}

	@Test // gh-2270
	void shouldProjectOnArray() {

		var json = "[ { \"creationDate\": 1610111331413, \"changeDate\": 1610111332160, \"person\": { \"caption\": \"Test2 TEST2\", \"firstName\": \"Test2\", \"lastName\": \"Test2\" } }, "
				+ "{ \"creationDate\": 1609775450502, \"changeDate\": 1609775451333, \"person\": { \"caption\": \"Test TEST\", \"firstName\": \"Test\", \"lastName\": \"Test\" } }]";

		var projection = projectionFactory.createProjection(UserPayload.class,
				new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		assertThat(projection.users()).hasSize(2);
	}

	interface Customer {

		String getFirstname();

		@JsonPath("$")
		Name getName();

		Address getAddress();

		List<Address> getAddresses();

		@JsonPath("$.addresses")
		List<AddressProjection> getAddressProjections();

		@JsonPath("$.firstname")
		String getBar();

		@JsonPath("$.address")
		AddressProjection getAddressProjection();

		@JsonPath("$.address.zipCode")
		String getNestedZipCode();

		@JsonPath("$.address")
		AnotherAddressProjection getAnotherAddressProjection();

		@JsonPath("$.addresses")
		List<AnotherAddressProjection> getAnotherAddressProjections();

		@JsonPath("$.address")
		Set<AnotherAddressProjection> getAnotherAddressProjectionAsCollection();

		@JsonPath("$..city")
		String getNestedCity();

		@JsonPath("$..city")
		List<String> getNestedCities();
	}

	interface AddressProjection {

		String getZipCode();

		String getCity();
	}

	interface Name {

		@JsonPath("$.firstname")
		String getFirstname();

		// Not available in the payload
		@JsonPath("$.lastname")
		@Nullable
		String getLastname();

		// First one not available in the payload
		@JsonPath({ "$.lastname", "$.firstname" })
		String getSomeName();
	}

	interface AnotherAddressProjection {

		@JsonPath("$.zipCode")
		String getZipCodeButNotCity();
	}

	static class Address {
		private String zipCode, city;

		public Address() {}

		public Address(String zipCode, String city) {
			this.zipCode = zipCode;
			this.city = city;
		}

		public String getZipCode() {
			return zipCode;
		}

		public void setZipCode(String zipCode) {
			this.zipCode = zipCode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Address address = (Address) o;
			return ObjectUtils.nullSafeEquals(zipCode, address.zipCode) && ObjectUtils.nullSafeEquals(city, address.city);
		}

	}

	@ProjectedPayload
	interface UserPayload {

		@JsonPath("$..person")
		List<Users> users();

		interface Users {

			public String getFirstName();

			public String getLastName();
		}
	}
}
