/*
 * Copyright 2014-2025 the original author or authors.
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

import example.NoNullableMarkedInterface;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Value;
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

		var customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		var excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getAddress().getZipCode()).isEqualTo("ZIP");
	}

	@Test // DATAREST-221, DATACMNS-630
	void proxyExposesTargetClassAware() {

		var proxy = factory.createProjection(CustomerExcerpt.class);

		assertThat(proxy).isInstanceOf(TargetClassAware.class);
		assertThat(((TargetClassAware) proxy).getTargetClass()).isEqualTo(HashMap.class);
	}

	@Test // DATAREST-221, DATACMNS-630
	void rejectsNonInterfacesAsProjectionTarget() {
		assertThatIllegalArgumentException().isThrownBy(() -> factory.createProjection(Object.class, new Object()));
	}

	@Test // DATACMNS-630
	void createsMapBasedProxyFromSource() {

		var addressSource = new HashMap<String, Object>();
		addressSource.put("zipCode", "ZIP");
		addressSource.put("city", "NewYork");

		Map<String, Object> source = new HashMap<>();
		source.put("firstname", "Dave");
		source.put("lastname", "Matthews");
		source.put("address", addressSource);

		var projection = factory.createProjection(CustomerExcerpt.class, source);

		assertThat(projection.getFirstname()).isEqualTo("Dave");

		var address = projection.getAddress();
		assertThat(address).isNotNull();
		assertThat(address.getZipCode()).isEqualTo("ZIP");
	}

	@Test // DATACMNS-630
	void createsEmptyMapBasedProxy() {

		var proxy = factory.createProjection(CustomerProxy.class);

		assertThat(proxy).isNotNull();

		proxy.setFirstname("Dave");
		assertThat(proxy.getFirstname()).isEqualTo("Dave");
	}

	@Test // DATACMNS-630
	void returnsAllPropertiesAsInputProperties() {

		var projectionInformation = factory.getProjectionInformation(CustomerExcerpt.class);
		var result = projectionInformation.getInputProperties();

		assertThat(result).hasSize(6);
		assertThat(projectionInformation.hasInputProperties()).isTrue();
		assertThat(projectionInformation.isClosed()).isTrue();
	}

	@Test // DATACMNS-630
	void identifiersOpenProjectionCorrectly() {

		var projectionInformation = factory.getProjectionInformation(OpenProjection.class);
		var result = projectionInformation.getInputProperties();

		assertThat(result).isEmpty();
		assertThat(projectionInformation.hasInputProperties()).isFalse();
		assertThat(projectionInformation.isClosed()).isFalse();
	}

	@Test // DATACMNS-655, GH-2831
	void invokesDefaultMethodOnProxy() {

		var excerpt = factory.createProjection(CustomerExcerptWithDefaultMethod.class);

		var advised = (Advised) ReflectionTestUtils.getField(Proxy.getInvocationHandler(excerpt), "advised");
		var advisors = advised.getAdvisors();

		assertThat(advisors.length).isGreaterThan(0);
		assertThat(advisors[0].getAdvice()).isInstanceOf(DefaultMethodInvokingMethodInterceptor.class);
	}

	@Test // GH-2831
	void doesNotRegisterDefaultMethodInvokingMethodInterceptor() {

		var excerpt = factory.createProjection(CustomerExcerpt.class);

		var advised = (Advised) ReflectionTestUtils.getField(Proxy.getInvocationHandler(excerpt), "advised");
		var advisors = advised.getAdvisors();

		assertThat(advisors.length).isGreaterThan(0);

		for (Advisor advisor : advisors) {
			assertThat(advisor).isNotInstanceOf(DefaultMethodInvokingMethodInterceptor.class);
		}
	}

	@Test // DATACMNS-648
	void exposesProxyTarget() {

		var excerpt = factory.createProjection(CustomerExcerpt.class);

		assertThat(excerpt).isInstanceOf(TargetAware.class);
		assertThat(((TargetAware) excerpt).getTarget()).isInstanceOf(Map.class);
	}

	@Test // DATACMNS-722
	void doesNotProjectPrimitiveArray() {

		var customer = new Customer();
		customer.picture = "binarydata".getBytes();

		var excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getPicture()).isEqualTo(customer.picture);
	}

	@Test // DATACMNS-722
	void projectsNonPrimitiveArray() {

		var address = new Address();
		address.city = "New York";
		address.zipCode = "ZIP";

		var customer = new Customer();
		customer.shippingAddresses = new Address[] { address };

		var excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getShippingAddresses()).hasSize(1);
	}

	@Test // DATACMNS-782
	void convertsPrimitiveValues() {

		var customer = new Customer();
		customer.id = 1L;

		var excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getId()).isEqualTo(customer.id.toString());
	}

	@Test // DATACMNS-89
	void exposesProjectionInformationCorrectly() {

		var information = factory.getProjectionInformation(CustomerExcerpt.class);

		assertThat(information.getType()).isEqualTo(CustomerExcerpt.class);
		assertThat(information.isClosed()).isTrue();
	}

	@Test // DATACMNS-829
	void projectsMapOfStringToObjectCorrectly() {

		var customer = new Customer();
		customer.data = Collections.singletonMap("key", null);

		var data = factory.createProjection(CustomerExcerpt.class, customer).getData();

		assertThat(data).isNotNull();
		assertThat(data.containsKey("key")).isTrue();
		assertThat(data.get("key")).isNull();
	}

	@Test // DATACMNS-1121
	void doesNotCreateWrappingProxyIfTargetImplementsProjectionInterface() {

		var customer = new Customer();

		assertThat(factory.createProjection(Contact.class, customer)).isSameAs(customer);
	}

	@Test // DATACMNS-1762
	void supportsOptionalAsReturnTypeIfEmpty() {

		var customer = new Customer();
		customer.picture = null;

		var excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getPicture()).isEmpty();
	}

	@Test // DATACMNS-1762
	void supportsOptionalAsReturnTypeIfPresent() {

		var customer = new Customer();
		customer.picture = new byte[] { 1, 2, 3 };

		var excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getPicture()).hasValueSatisfying(bytes -> {
			assertThat(bytes).isEqualTo(new byte[] { 1, 2, 3 });
		});
	}

	@Test // DATACMNS-1762
	void supportsOptionalBackedByOptional() {

		var customer = new Customer();
		customer.optional = Optional.of("foo");

		var excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getOptional()).hasValue("foo");
	}

	@Test // DATACMNS-1762
	void supportsOptionalWithProjectionAsReturnTypeIfPresent() {

		var customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		var excerpt = factory.createProjection(CustomerWithOptionalHavingProjection.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getAddress()).hasValueSatisfying(addressExcerpt -> {
			assertThat(addressExcerpt.getZipCode()).isEqualTo("ZIP");
		});
	}

	@Test // DATACMNS-1836
	void supportsDateToLocalDateTimeConversion() {

		var customer = new Customer();
		customer.firstname = "Dave";
		customer.birthdate = new GregorianCalendar(1967, Calendar.JANUARY, 9).getTime();

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		var excerpt = factory.createProjection(CustomerWithLocalDateTime.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getBirthdate()).isEqualTo(LocalDateTime.of(1967, 1, 9, 0, 0));
	}

	@Test // DATACMNS-1836
	void supportsNullableWrapperDateToLocalDateTimeConversion() {

		var customer = new Customer();
		customer.firstname = "Dave";
		customer.birthdate = new GregorianCalendar(1967, Calendar.JANUARY, 9).getTime();

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		var excerpt = factory.createProjection(CustomerWithOptional.class, customer);

		assertThat(excerpt.getFirstname()).isEqualTo("Dave");
		assertThat(excerpt.getBirthdate()).contains(LocalDateTime.of(1967, 1, 9, 0, 0));
	}

	@Test // GH-3242
	@Disabled(" ReflectJvmMapping.getKotlinFunction(method) returns null for Person.getAge()")
	void projectionFactoryConsidersKotlinNullabilityConstraints() {

		var source = new HashMap<String, Object>(2);
		source.put("name", null);
		source.put("age", null);

		Person projection = factory.createProjection(Person.class, source);

		assertThatNoException().isThrownBy(projection::getAge);
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(projection::getName);
	}

	@Test // GH-3242
	void projectionFactoryConsidersNullabilityAnnotations() {

		var source = new HashMap<String, Object>(2);
		source.put("firstname", null);
		source.put("lastname", null);

		CustomerProjectionWithNullables projection = factory.createProjection(CustomerProjectionWithNullables.class, source);

		assertThatNoException().isThrownBy(projection::getFirstname);
		assertThatExceptionOfType(NullPointerException.class).isThrownBy(projection::getLastname);
	}

	@Test // GH-3242
	void projectionFactoryIgnoresNullabilityAnnotationsOnUnmanagedPackage() {

		var source = new HashMap<String, Object>(2);
		source.put("firstname", null);
		source.put("lastname", null);

		NoNullableMarkedInterface projection = factory.createProjection(NoNullableMarkedInterface.class, source);

		assertThatNoException().isThrownBy(projection::getFirstname);
		assertThatNoException().isThrownBy(projection::getLastname);
	}

	interface Contact {}

	interface CustomerWithLocalDateTime {

		String getFirstname();

		LocalDateTime getBirthdate();
	}

	interface CustomerProjectionWithNullables {

		@Nullable
		String getFirstname();
		String getLastname();
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

	interface OpenProjection {

		@Value("#{@greetingsFrom.groot(target.firstname)}")
		String hello();
	}

	interface CustomerExcerptWithDefaultMethod extends CustomerExcerpt {

		default String getFirstnameAndId() {
			return getFirstname() + " " + getId();
		}
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
