/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import lombok.experimental.Wither;

import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;

/**
 * @author Oliver Gierke
 */
public class PersistentPropertyAccessorUnitTests {

	SampleMappingContext context = new SampleMappingContext();

	PersistentPropertyPath<? extends PersistentProperty<?>> path;
	PersistentPropertyAccessor accessor;

	private void setUp(Order order, String path) {

		this.accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		this.path = context.getPersistentPropertyPath(path, Order.class);
	}

	@Test // DATACMNS-1275
	public void looksUpValueForPropertyPath() {

		Order order = new Order(new Customer("Dave"));

		setUp(order, "customer.firstname");

		assertThat(accessor.getProperty(path)).isEqualTo("Dave");
	}

	@Test // DATACMNS-1275
	public void setsPropertyOnNestedPath() {

		Customer customer = new Customer("Dave");
		Order order = new Order(customer);

		setUp(order, "customer.firstname");

		accessor.setProperty(path, "Oliver August");

		assertThat(customer.firstname).isEqualTo("Oliver August");
	}

	@Test // DATACMNS-1275
	public void rejectsEmptyPathToSetValues() {

		setUp(new Order(null), "");

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> accessor.setProperty(path, "Oliver August"));
	}

	@Test // DATACMNS-1275
	public void rejectsIntermediateNullValuesForRead() {

		setUp(new Order(null), "customer.firstname");

		assertThatExceptionOfType(MappingException.class)//
				.isThrownBy(() -> accessor.getProperty(path));
	}

	@Test // DATACMNS-1275
	public void rejectsIntermediateNullValuesForWrite() {

		setUp(new Order(null), "customer.firstname");

		assertThatExceptionOfType(MappingException.class)//
				.isThrownBy(() -> accessor.setProperty(path, "Oliver August"));
	}

	@Test // DATACMNS-1322
	public void correctlyReplacesObjectInstancesWhenSettingPropertyPathOnImmutableObjects() {

		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Outer.class);
		PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath("immutable.value",
				entity.getType());

		NestedImmutable immutable = new NestedImmutable("foo");
		Outer outer = new Outer(immutable);

		PersistentPropertyAccessor accessor = entity.getPropertyAccessor(outer);
		accessor.setProperty(path, "bar");

		Object result = accessor.getBean();

		assertThat(result).isInstanceOfSatisfying(Outer.class, it -> {
			assertThat(it.immutable).isNotSameAs(immutable);
			assertThat(it).isNotSameAs(outer);
		});
	}

	@Test // DATACMNS-1377
	public void shouldConvertToPropertyPathLeafType() {

		Order order = new Order(new Customer("1"));

		PersistentPropertyAccessor<Order> accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		ConvertingPropertyAccessor<Order> convertingAccessor = new ConvertingPropertyAccessor<>(accessor,
				new DefaultConversionService());

		PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath("customer.firstname",
				Order.class);

		convertingAccessor.setProperty(path, 2);

		assertThat(convertingAccessor.getBean().getCustomer().getFirstname()).isEqualTo("2");
	}

	@Value
	static class Order {
		Customer customer;
	}

	@Data
	@AllArgsConstructor
	static class Customer {
		String firstname;
	}

	// DATACMNS-1322

	@Value
	@Wither(AccessLevel.PACKAGE)
	static class NestedImmutable {
		String value;
	}

	@Value
	@Wither(AccessLevel.PACKAGE)
	static class Outer {
		NestedImmutable immutable;
	}
}
