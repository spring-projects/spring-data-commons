/*
 * Copyright 2016-2017 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverterUnitTests.UnannotatedInterface;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.xml.sax.SAXParseException;
import org.xmlbeam.annotation.XBRead;

/**
 * Unit tests for {@link XmlBeamHttpMessageConverter}.
 * 
 * @author Oliver Gierke
 * @soundtrack Dr. Kobayashi Maru & The Mothership Connection - Anthem (EPisode One)
 */
@RunWith(MockitoJUnitRunner.class)
public class XmlBeamHttpMessageConverterUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	XmlBeamHttpMessageConverter converter = new XmlBeamHttpMessageConverter();

	@Mock HttpInputMessage message;

	@Test // DATACMNS-885
	public void findsTopLevelElements() throws Exception {

		preparePayload("<user><firstname>Dave</firstname><lastname>Matthews</lastname></user>");

		Customer customer = (Customer) converter.read(Customer.class, message);

		assertThat(customer.getFirstname(), is("Dave"));
		assertThat(customer.getLastname(), is("Matthews"));
	}

	@Test // DATACMNS-885
	public void findsNestedElements() throws Exception {

		preparePayload("<user><username><firstname>Dave</firstname><lastname>Matthews</lastname></username></user>");

		Customer customer = (Customer) converter.read(Customer.class, message);

		assertThat(customer.getFirstname(), is("Dave"));
		assertThat(customer.getLastname(), is("Matthews"));
	}

	@Test // DATACMNS-885
	public void supportsAnnotatedInterface() {
		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_XML), is(true));
	}

	@Test // DATACMNS-885
	public void supportsXmlBasedMediaType() {
		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_ATOM_XML), is(true));
	}

	@Test // DATACMNS-885
	public void doesNotSupportUnannotatedInterface() {
		assertThat(converter.canRead(UnannotatedInterface.class, MediaType.APPLICATION_XML), is(false));
	}

	@Test // DATACMNS-885
	public void supportsInterfaceAfterLookupForDifferrentMediaType() {

		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_JSON), is(false));
		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_XML), is(true));
	}

	@Test // DATACMNS-1292
	public void doesNotSupportEntityExpansion() throws Exception {

		preparePayload("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" //
				+ "<!DOCTYPE foo [\n" //
				+ "<!ELEMENT foo ANY >\n" //
				+ "<!ENTITY xxe \"Bar\" >]><user><firstname>&xxe;</firstname><lastname>Matthews</lastname></user>");

		exception.expect(HttpMessageNotReadableException.class);
		exception.expectCause(is(Matchers.<Throwable> instanceOf(SAXParseException.class)));

		converter.read(Customer.class, message);
	}

	private void preparePayload(String payload) throws IOException {
		when(message.getBody()).thenReturn(new ByteArrayInputStream(payload.getBytes()));
	}

	@ProjectedPayload
	public interface Customer {

		@XBRead("//firstname")
		String getFirstname();

		@XBRead("//lastname")
		String getLastname();
	}

	public interface UnnannotatedInterface {}
}
