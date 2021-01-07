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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Unit tests for {@link ConvertingPropertyAccessor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ConvertingPropertyAccessorUnitTests {

	static final ConversionService CONVERSION_SERVICE = new DefaultFormattingConversionService();

	@Test // DATACMNS-596
	public void rejectsNullPropertyAccessorDelegate() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConvertingPropertyAccessor(null, CONVERSION_SERVICE));
	}

	@Test // DATACMNS-596
	public void rejectsNullConversionService() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ConvertingPropertyAccessor(new BeanWrapper<>(new Object()), null));
	}

	@Test // DATACMNS-596
	public void returnsBeanFromDelegate() {

		Object entity = new Entity();
		assertThat(getAccessor(entity, CONVERSION_SERVICE).getBean()).isEqualTo(entity);
	}

	@Test // DATACMNS-596
	public void convertsPropertyValueToExpectedType() {

		Entity entity = new Entity();
		entity.id = 1L;

		assertThat(getIdProperty()).satisfies(
				it -> assertThat(getAccessor(entity, CONVERSION_SERVICE).getProperty(it, String.class)).isEqualTo("1"));
	}

	@Test // DATACMNS-596
	public void doesNotInvokeConversionForNullValues() {

		ConversionService conversionService = mock(ConversionService.class);

		assertThat(getIdProperty()).satisfies(it -> {
			assertThat(getAccessor(new Entity(), conversionService).getProperty(it, Number.class)).isNull();
			verify(conversionService, times(0)).convert(1L, Number.class);
		});
	}

	@Test // DATACMNS-596
	public void doesNotInvokeConversionIfTypeAlreadyMatches() {

		Entity entity = new Entity();
		entity.id = 1L;

		ConversionService conversionService = mock(ConversionService.class);

		assertThat(getIdProperty()).satisfies(it -> {
			assertThat(getAccessor(entity, conversionService).getProperty(it, Number.class)).isEqualTo(1L);
			verify(conversionService, times(0)).convert(1L, Number.class);
		});
	}

	@Test // DATACMNS-596
	public void convertsValueOnSetIfTypesDontMatch() {

		Entity entity = new Entity();

		assertThat(getIdProperty()).satisfies(property -> {
			getAccessor(entity, CONVERSION_SERVICE).setProperty(property, "1");
			assertThat(entity.id).isEqualTo(1L);
		});
	}

	@Test // DATACMNS-596
	public void doesNotInvokeConversionIfTypeAlreadyMatchesOnSet() {

		assertThat(getIdProperty()).satisfies(it -> {
			getAccessor(new Entity(), mock(ConversionService.class)).setProperty(it, 1L);
			verify(mock(ConversionService.class), times(0)).convert(1L, Long.class);
		});
	}

	@Test // DATACMNS-1377
	public void shouldConvertToPropertyPathLeafType() {

		Order order = new Order(new Customer("1"));

		SampleMappingContext context = new SampleMappingContext();

		PersistentPropertyAccessor<Order> accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		ConvertingPropertyAccessor<Order> convertingAccessor = new ConvertingPropertyAccessor<>(accessor,
				new DefaultConversionService());

		PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath("customer.firstname",
				Order.class);

		convertingAccessor.setProperty(path, 2);

		assertThat(convertingAccessor.getBean().getCustomer().getFirstname()).isEqualTo("2");
	}

	private static ConvertingPropertyAccessor getAccessor(Object entity, ConversionService conversionService) {

		PersistentPropertyAccessor wrapper = new BeanWrapper<>(entity);
		return new ConvertingPropertyAccessor(wrapper, conversionService);
	}

	private static SamplePersistentProperty getIdProperty() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Entity.class);
		return entity.getPersistentProperty("id");
	}

	static class Entity {
		Long id;
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
}
