/*
 * Copyright 2018-present the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;

/**
 * Unit tests for {@link PersistentPropertyAccessor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class PersistentPropertyAccessorUnitTests {

	SampleMappingContext context = new SampleMappingContext();

	PersistentPropertyPath<? extends PersistentProperty<?>> path;
	PersistentPropertyAccessor accessor;

	private void setUp(Order order, String path) {

		this.accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		this.path = context.getPersistentPropertyPath(path, Order.class);
	}

	@Test // DATACMNS-1377
	public void shouldConvertToPropertyPathLeafType() {

		var order = new Order(new Customer("1"));

		var accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		var convertingAccessor = new ConvertingPropertyAccessor<Order>(accessor, new DefaultConversionService());

		var path = context.getPersistentPropertyPath("customer.firstname", Order.class);

		convertingAccessor.setProperty(path, 2);

		assertThat(convertingAccessor.getBean().customer().getFirstname()).isEqualTo("2");
	}

	record Order(Customer customer) {
	}

	static class Customer {
		String firstname;

		public Customer(String firstname) {
			this.firstname = firstname;
		}

		public String getFirstname() {
			return this.firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

	}

	// DATACMNS-1322

	static final class NestedImmutable {
		private final String value;

		public NestedImmutable(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		NestedImmutable withValue(String value) {
			return this.value == value ? this : new NestedImmutable(value);
		}
	}

	static final class Outer {
		private final NestedImmutable immutable;

		public Outer(NestedImmutable immutable) {
			this.immutable = immutable;
		}

		public NestedImmutable getImmutable() {
			return this.immutable;
		}

		Outer withImmutable(NestedImmutable immutable) {
			return this.immutable == immutable ? this : new Outer(immutable);
		}
	}
}
