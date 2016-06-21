/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Unit tests for {@link ConvertingPropertyAccessor}.
 * 
 * @author Oliver Gierke
 */
public class ConvertingPropertyAccessorUnitTests {

	static final ConversionService CONVERSION_SERVICE = new DefaultFormattingConversionService();

	/**
	 * @see DATACMNS-596
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPropertyAccessorDelegate() {
		new ConvertingPropertyAccessor(null, CONVERSION_SERVICE);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullConversionService() {
		new ConvertingPropertyAccessor(new BeanWrapper<>(new Object()), null);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void returnsBeanFromDelegate() {

		Object entity = new Entity();
		assertThat(getAccessor(entity, CONVERSION_SERVICE).getBean()).isEqualTo(entity);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void convertsPropertyValueToExpectedType() {

		Entity entity = new Entity();
		entity.id = 1L;

		assertThat(getIdProperty()).hasValueSatisfying(it -> {
			assertThat(getAccessor(entity, CONVERSION_SERVICE).getProperty(it, String.class)).hasValue("1");
		});
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void doesNotInvokeConversionForNullValues() {

		ConversionService conversionService = mock(ConversionService.class);

		assertThat(getIdProperty()).hasValueSatisfying(it -> {
			assertThat(getAccessor(new Entity(), conversionService).getProperty(it, Number.class)).isNotPresent();
			verify(conversionService, times(0)).convert(1L, Number.class);
		});
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void doesNotInvokeConversionIfTypeAlreadyMatches() {

		Entity entity = new Entity();
		entity.id = 1L;

		ConversionService conversionService = mock(ConversionService.class);

		assertThat(getIdProperty()).hasValueSatisfying(it -> {
			assertThat(getAccessor(entity, conversionService).getProperty(it, Number.class)).hasValue(1L);
			verify(conversionService, times(0)).convert(1L, Number.class);
		});
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void convertsValueOnSetIfTypesDontMatch() {

		Entity entity = new Entity();

		assertThat(getIdProperty()).hasValueSatisfying(property -> {
			getAccessor(entity, CONVERSION_SERVICE).setProperty(property, Optional.of("1"));
			assertThat(entity.id).isEqualTo(1L);
		});
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void doesNotInvokeConversionIfTypeAlreadyMatchesOnSet() {

		assertThat(getIdProperty()).hasValueSatisfying(it -> {
			getAccessor(new Entity(), mock(ConversionService.class)).setProperty(it, Optional.of(1L));
			verify(mock(ConversionService.class), times(0)).convert(1L, Long.class);
		});
	}

	private static ConvertingPropertyAccessor getAccessor(Object entity, ConversionService conversionService) {

		PersistentPropertyAccessor wrapper = new BeanWrapper<>(entity);
		return new ConvertingPropertyAccessor(wrapper, conversionService);
	}

	private static Optional<SamplePersistentProperty> getIdProperty() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Entity.class);
		return entity.getPersistentProperty("id");
	}

	static class Entity {
		Long id;
	}
}
