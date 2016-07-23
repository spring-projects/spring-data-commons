/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.web;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpInputMessage;
import org.xmlbeam.annotation.XBRead;

/**
 * Unit tests for {@link XmlBeamHttpMessageConverter}.
 * 
 * @author Oliver Gierke
 * @soundtrack Dr. Kobayashi Maru & The Mothership Connection - Anthem (EPisode One)
 */
@RunWith(MockitoJUnitRunner.class)
public class XmlBeamHttpMessageConverterUnitTests {

	XmlBeamHttpMessageConverter converter = new XmlBeamHttpMessageConverter();

	@Mock HttpInputMessage message;

	/**
	 * @see DATACMNS-885
	 */
	@Test
	public void findsTopLevelElements() throws Exception {

		preparePayload("<user><firstname>Dave</firstname><lastname>Matthews</lastname></user>");

		Customer customer = (Customer) converter.read(Customer.class, message);

		assertThat(customer.getFirstname(), is("Dave"));
		assertThat(customer.getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-885
	 */
	@Test
	public void findsNestedElements() throws Exception {

		preparePayload("<user><username><firstname>Dave</firstname><lastname>Matthews</lastname></username></user>");

		Customer customer = (Customer) converter.read(Customer.class, message);

		assertThat(customer.getFirstname(), is("Dave"));
		assertThat(customer.getLastname(), is("Matthews"));
	}

	private void preparePayload(String payload) throws IOException {
		when(message.getBody()).thenReturn(new ByteArrayInputStream(payload.getBytes()));
	}

	public interface Customer {

		@XBRead("//firstname")
		String getFirstname();

		@XBRead("//lastname")
		String getLastname();
	}
}
