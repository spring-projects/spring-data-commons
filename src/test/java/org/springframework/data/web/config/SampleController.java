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
package org.springframework.data.web.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Date;

import org.springframework.data.web.config.SampleController.SampleDto.Address;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Oliver Gierke
 */
@Controller
public class SampleController {

	@RequestMapping("/proxy")
	public String someMethod(SampleDto sampleDto) {

		assertThat(sampleDto, is(notNullValue()));
		assertThat(sampleDto.getName(), is("Foo"));
		assertThat(sampleDto.getDate(), is(notNullValue()));

		Collection<Address> shippingAddresses = sampleDto.getShippingAddresses();

		assertThat(shippingAddresses, is(hasSize(1)));
		assertThat(shippingAddresses.iterator().next().getZipCode(), is("ZIP"));
		assertThat(shippingAddresses.iterator().next().getCity(), is("City"));

		assertThat(sampleDto.getBillingAddress(), is(notNullValue()));
		assertThat(sampleDto.getBillingAddress().getZipCode(), is("ZIP"));
		assertThat(sampleDto.getBillingAddress().getCity(), is("City"));

		return "view";
	}

	interface SampleDto {

		String getName();

		@DateTimeFormat(iso = ISO.DATE)
		Date getDate();

		Address getBillingAddress();

		Collection<Address> getShippingAddresses();

		interface Address {

			String getZipCode();

			String getCity();
		}
	}
}
