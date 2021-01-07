/*
 * Copyright 2016-2021 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@ExtendWith(MockitoExtension.class)
class XmlBeamHttpMessageConverterUnitTests {

	XmlBeamHttpMessageConverter converter = new XmlBeamHttpMessageConverter();

	@Mock HttpInputMessage message;

	@Test // DATACMNS-885
	void findsTopLevelElements() throws Exception {

		preparePayload("<user><firstname>Dave</firstname><lastname>Matthews</lastname></user>");

		Customer customer = (Customer) converter.read(Customer.class, message);

		assertThat(customer.getFirstname()).isEqualTo("Dave");
		assertThat(customer.getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-885
	void findsNestedElements() throws Exception {

		preparePayload("<user><username><firstname>Dave</firstname><lastname>Matthews</lastname></username></user>");

		Customer customer = (Customer) converter.read(Customer.class, message);

		assertThat(customer.getFirstname()).isEqualTo("Dave");
		assertThat(customer.getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-885
	void supportsAnnotatedInterface() {
		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_XML)).isTrue();
	}

	@Test // DATACMNS-885
	void supportsXmlBasedMediaType() {
		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_ATOM_XML)).isTrue();
	}

	@Test // DATACMNS-885
	void doesNotSupportUnannotatedInterface() {
		assertThat(converter.canRead(UnannotatedInterface.class, MediaType.APPLICATION_XML)).isFalse();
	}

	@Test // DATACMNS-885
	void supportsInterfaceAfterLookupForDifferrentMediaType() {

		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_JSON)).isFalse();
		assertThat(converter.canRead(Customer.class, MediaType.APPLICATION_XML)).isTrue();
	}

	@Test // DATACMNS-1292
	void doesNotSupportEntityExpansion() throws Exception {

		preparePayload("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" //
				+ "<!DOCTYPE foo [\n" //
				+ "<!ELEMENT foo ANY >\n" //
				+ "<!ENTITY xxe \"Bar\" >]><user><firstname>&xxe;</firstname><lastname>Matthews</lastname></user>");

		assertThatExceptionOfType(HttpMessageNotReadableException.class) //
				.isThrownBy(() -> converter.read(Customer.class, message)) //
				.withCauseInstanceOf(SAXParseException.class);
	}

	private void preparePayload(String payload) throws IOException {
		when(message.getBody()).thenReturn(new ByteArrayInputStream(payload.getBytes()));
	}

	@ProjectedPayload
	interface Customer {

		@XBRead("//firstname")
		String getFirstname();

		@XBRead("//lastname")
		String getLastname();
	}

	interface UnnannotatedInterface {}
}
