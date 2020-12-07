/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.projection;

import static org.assertj.core.api.Assertions.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.Advised;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ProxyProjectionFactory}.
 *
 * @author Oliver Gierke
 * @author Wim Deblauwe
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Christoph Strobl
 */
class ProxyProjectionFactoryUnitTests {

	ProjectionFactory factory = new ProxyProjectionFactory();

	@Test
	@SuppressWarnings("null")
	// DATACMNS-630
	void rejectsNullProjectionType() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.createProjection(null));
	}

	@Test
	@SuppressWarnings("null")
	// DATACMNS-630
	void rejectsNullProjectionTypeWithSource() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.createProjection(null, new Object()));
	}

	@Test // DATACMNS-630
	void returnsNullForNullSource() {
		assertThat(factory.createNullableProjection(CustomerExcerpt.class, null)).isNull();
	}

	@Test // DATAREST-221, DATACMNS-630
	void createsProjectingProxy() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getAddress().getZipCode()).isEqualTo("ZIP");
	}

	@Test // DATAREST-221, DATACMNS-630
	void proxyExposesTargetClassAware() {

		CustomerExcerpt proxy = factory.createProjection(CustomerExcerpt.class);

		assertThat(proxy).isInstanceOf(TargetClassAware.class);
		assertThat(((TargetClassAware) proxy).getTargetClass()).isEqualTo(HashMap.class);
	}

	@Test // DATAREST-221, DATACMNS-630
	void rejectsNonInterfacesAsProjectionTarget() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.createProjection(Object.class, new Object()));
	}

	@Test // DATACMNS-630
	void createsMapBasedProxyFromSource() {

		HashMap<String, Object> addressSource = new HashMap<>();
		addressSource.put("zipCode", "ZIP");
		addressSource.put("city", "NewYork");

		Map<String, Object> source = new HashMap<>();
		source.put("firstname", "Dave");
		source.put("lastname", "Matthews");
		source.put("address", addressSource);

		CustomerExcerpt projection = factory.createProjection(CustomerExcerpt.class, source);

		assertThat(projection.getFirstname()).isEqualTo("Dave");

		AddressExcerpt address = projection.getAddress();
		assertThat(address).isNotNull();
		assertThat(address.getZipCode()).isEqualTo("ZIP");
	}

	@Test // DATACMNS-630
	void createsEmptyMapBasedProxy() {

		CustomerProxy proxy = factory.createProjection(CustomerProxy.class);

		assertThat(proxy).isNotNull();

		proxy.setFirstname("Dave");
		assertThat(proxy.getFirstname()).isEqualTo("Dave");
	}

	@Test // DATACMNS-630
	void returnsAllPropertiesAsInputProperties() {

		ProjectionInformation projectionInformation = factory.getProjectionInformation(CustomerExcerpt.class);
		List<PropertyDescriptor> result = projectionInformation.getInputProperties();

		assertThat(result).hasSize(6);
	}

	@Test // DATACMNS-655
	void invokesDefaultMethodOnProxy() {

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class);

		Advised advised = (Advised) ReflectionTestUtils.getField(Proxy.getInvocationHandler(excerpt), "advised");
		Advisor[] advisors = advised.getAdvisors();

		assertThat(advisors.length).isGreaterThan(0);
		assertThat(advisors[0].getAdvice()).isInstanceOf(DefaultMethodInvokingMethodInterceptor.class);
	}

	@Test // DATACMNS-648
	void exposesProxyTarget() {

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class);

		assertThat(excerpt).isInstanceOf(TargetAware.class);
		assertThat(((TargetAware) excerpt).getTarget()).isInstanceOf(Map.class);
	}

	@Test // DATACMNS-722
	void doesNotProjectPrimitiveArray() {

		Customer customer = new Customer();
		customer.picture = "binarydata".getBytes();

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getPicture()).isEqualTo(customer.picture);
	}

	@Test // DATACMNS-722
	void projectsNonPrimitiveArray() {

		Address address = new Address();
		address.city = "New York";
		address.zipCode = "ZIP";

		Customer customer = new Customer();
		customer.shippingAddresses = new Address[] { address };

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getShippingAddresses()).hasSize(1);
	}

	@Test // DATACMNS-782
	void convertsPrimitiveValues() {

		Customer customer = new Customer();
		customer.id = 1L;

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getId()).isEqualTo(customer.id.toString());
	}

	@Test // DATACMNS-89
	void exposesProjectionInformationCorrectly() {

		ProjectionInformation information = factory.getProjectionInformation(CustomerExcerpt.class);

		assertThat(information.getType()).isEqualTo(CustomerExcerpt.class);
		assertThat(information.isClosed()).isTrue();
	}

	@Test // DATACMNS-829
	void projectsMapOfStringToObjectCorrectly() {

		Customer customer = new Customer();
		customer.data = Collections.singletonMap("key", null);

		Map<String, Object> data = factory.createProjection(CustomerExcerpt.class, customer).getData();

		assertThat(data).isNotNull();
		assertThat(data.containsKey("key")).isTrue();
		assertThat(data.get("key")).isNull();
	}

	@Test // DATACMNS-1121
	void doesNotCreateWrappingProxyIfTargetImplementsProjectionInterface() {

		Customer customer = new Customer();

		assertThat(factory.createProjection(Contact.class, customer)).isSameAs(customer);
	}

	@Test // DATACMNS-1762
	void supportsOptionalAsReturnTypeIfEmpty() {

		Customer customer = new Customer();
		customer.picture = null;

		CustomerWithOptional excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getPicture()).isEmpty();
	}

	@Test // DATACMNS-1762
	void supportsOptionalAsReturnTypeIfPresent() {

		Customer customer = new Customer();
		customer.picture = new byte[] { 1, 2, 3 };

		CustomerWithOptional excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getPicture()).hasValueSatisfying(bytes -> {
			assertThat(bytes).isEqualTo(new byte[] { 1, 2, 3 });
		});
	}

	@Test // DATACMNS-1762
	void supportsOptionalBackedByOptional() {

		Customer customer = new Customer();
		customer.optional = Optional.of("foo");

		CustomerWithOptional excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getOptional()).hasValue("foo");
	}

	@Test // DATACMNS-1762
	void supportsOptionalWithProjectionAsReturnTypeIfPresent() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		CustomerWithOptionalHavingProjection excerpt = factory.createProjection(CustomerWithOptionalHavingProjection.class,
				customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getAddress()).hasValueSatisfying(addressExcerpt -> {
			assertThat(addressExcerpt.getZipCode()).isEqualTo("ZIP");
		});
	}

	@Test // DATACMNS-1836
	void supportsDateToLocalDateTimeConversion() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.birthdate = new GregorianCalendar(1967, Calendar.JANUARY, 9).getTime();

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		CustomerWithLocalDateTime excerpt = factory.createProjection(CustomerWithLocalDateTime.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getBirthdate()).isEqualTo(LocalDateTime.of(1967, 1, 9, 0, 0));
	}

	@Test // DATACMNS-1836
	void supportsNullableWrapperDateToLocalDateTimeConversion() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.birthdate = new GregorianCalendar(1967, Calendar.JANUARY, 9).getTime();

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		CustomerWithOptional excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getBirthdate()).contains(LocalDateTime.of(1967, 1, 9, 0, 0));
	}

	interface Contact {}

	interface CustomerWithLocalDateTime {

		String getFirstname();

		LocalDateTime getBirthdate();
	}

	static class Address {

		String zipCode, city;
	}

	interface CustomerExcerpt {

		String getId();

		String getFirstname();

		AddressExcerpt getAddress();

		AddressExcerpt[] getShippingAddresses();

		byte[] getPicture();

		Map<String, Object> getData();
	}

	interface AddressExcerpt {

		String getZipCode();
	}

	interface CustomerProxy {

		String getFirstname();

		void setFirstname(String firstname);
	}

	interface CustomerWithOptional {

		String getFirstname();

		Optional<byte[]> getPicture();

		Optional<String> getOptional();

		Optional<LocalDateTime> getBirthdate();
	}

	interface CustomerWithOptionalHavingProjection {

		String getFirstname();

		Optional<AddressExcerpt> getAddress();
	}

	static class Customer implements Contact {

		Long id;
		String firstname, lastname;
		Date birthdate;
		Address address;
		byte[] picture;
		Address[] shippingAddresses;
		Map<String, Object> data;
		Optional<String> optional;
	}
}
