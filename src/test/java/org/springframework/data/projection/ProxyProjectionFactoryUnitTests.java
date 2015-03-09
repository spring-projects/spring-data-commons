/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.projection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.aop.TargetClassAware;

/**
 * Unit tests for {@link ProxyProjectionFactory}.
 * 
 * @author Oliver Gierke
 */
public class ProxyProjectionFactoryUnitTests {

	ProjectionFactory factory = new ProxyProjectionFactory();

	/**
	 * @see DATACMNS-630
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProjectionType() {
		factory.createProjection(null);
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProjectionTypeWithSource() {
		factory.createProjection(null, new Object());
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProjectionTypeForInputProperties() {
		factory.getInputProperties(null);
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void returnsNullForNullSource() {
		assertThat(factory.createProjection(CustomerExcerpt.class, null), is(nullValue()));
	}

	/**
	 * @see DATAREST-221, DATACMNS-630
	 */
	@Test
	public void createsProjectingProxy() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		customer.address = new Address();
		customer.address.city = "New York";
		customer.address.zipCode = "ZIP";

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class, customer);

		assertThat(excerpt.getFirstname(), is("Dave"));
		assertThat(excerpt.getAddress().getZipCode(), is("ZIP"));
	}

	/**
	 * @see DATAREST-221, DATACMNS-630
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void proxyExposesTargetClassAware() {

		CustomerExcerpt proxy = factory.createProjection(CustomerExcerpt.class);

		assertThat(proxy, is(instanceOf(TargetClassAware.class)));
		assertThat(((TargetClassAware) proxy).getTargetClass(), is(equalTo((Class) HashMap.class)));
	}

	/**
	 * @see DATAREST-221, DATACMNS-630
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonInterfacesAsProjectionTarget() {
		factory.createProjection(Object.class, new Object());
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void createsMapBasedProxyFromSource() {

		HashMap<String, Object> addressSource = new HashMap<String, Object>();
		addressSource.put("zipCode", "ZIP");
		addressSource.put("city", "NewYork");

		Map<String, Object> source = new HashMap<String, Object>();
		source.put("firstname", "Dave");
		source.put("lastname", "Matthews");
		source.put("address", addressSource);

		CustomerExcerpt projection = factory.createProjection(CustomerExcerpt.class, source);

		assertThat(projection.getFirstname(), is("Dave"));

		AddressExcerpt address = projection.getAddress();
		assertThat(address, is(notNullValue()));
		assertThat(address.getZipCode(), is("ZIP"));
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void createsEmptyMapBasedProxy() {

		CustomerProxy proxy = factory.createProjection(CustomerProxy.class);

		assertThat(proxy, is(notNullValue()));

		proxy.setFirstname("Dave");
		assertThat(proxy.getFirstname(), is("Dave"));
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void returnsAllPropertiesAsInputProperties() {

		List<String> result = factory.getInputProperties(CustomerExcerpt.class);

		assertThat(result, hasSize(2));
		assertThat(result, hasItems("firstname", "address"));
	}

	/**
	 * @see DATACMNS-655
	 */
	@Test
	public void invokesDefaultMethodOnProxy() {

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class);

		assertThat(excerpt.sampleDefaultMethod(), is("default"));
	}

	static class Customer {

		public String firstname, lastname;
		public Address address;
	}

	static class Address {

		public String zipCode, city;
	}

	interface CustomerExcerpt {

		String getFirstname();

		AddressExcerpt getAddress();

		default String sampleDefaultMethod() {
			return "default";
		}
	}

	interface AddressExcerpt {

		String getZipCode();
	}

	interface CustomerProxy {

		String getFirstname();

		void setFirstname(String firstname);
	}
}
