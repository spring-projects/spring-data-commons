/*
 * Copyright 2015 the original author or authors.
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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Unit tests for {@link SpelAwareProxyProjectionFactory}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class SpelAwareProxyProjectionFactoryUnitTests {

	private SpelAwareProxyProjectionFactory factory;

	@Before
	public void setup() {
		factory = new SpelAwareProxyProjectionFactory();
	}

	/**
	 * @see DATAREST-221, DATACMNS-630
	 */
	@Test
	public void exposesSpelInvokingMethod() {

		Customer customer = new Customer();
		customer.firstname = "Dave";
		customer.lastname = "Matthews";

		CustomerExcerpt excerpt = factory.createProjection(CustomerExcerpt.class, customer);
		assertThat(excerpt.getFullName(), is("Dave Matthews"));
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void excludesAtValueAnnotatedMethodsForInputProperties() {

		List<String> properties = factory.getInputProperties(CustomerExcerpt.class);

		assertThat(properties, hasSize(1));
		assertThat(properties, hasItem("firstname"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void considersProjectionUsingAtValueNotClosed() {

		ProjectionInformation information = factory.getProjectionInformation(CustomerExcerpt.class);

		assertThat(information.isClosed(), is(false));
	}

	static class Customer {

		public String firstname, lastname;
	}

	interface CustomerExcerpt {

		@Value("#{target.firstname + ' ' + target.lastname}")
		String getFullName();

		String getFirstname();
	}
}
