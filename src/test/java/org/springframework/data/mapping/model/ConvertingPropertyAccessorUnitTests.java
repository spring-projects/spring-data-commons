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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
		new ConvertingPropertyAccessor(new BeanWrapper<Object>(new Object()), null);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void returnsBeanFromDelegate() {

		Object entity = new Entity();
		assertThat(getAccessor(entity, CONVERSION_SERVICE).getBean(), is(entity));
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void convertsPropertyValueToExpectedType() {

		Entity entity = new Entity();
		entity.id = 1L;

		ConvertingPropertyAccessor accessor = getAccessor(entity, CONVERSION_SERVICE);

		assertThat(accessor.getProperty(getIdProperty(), String.class), is("1"));
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void doesNotInvokeConversionForNullValues() {

		ConversionService conversionService = mock(ConversionService.class);
		ConvertingPropertyAccessor accessor = getAccessor(new Entity(), conversionService);

		assertThat(accessor.getProperty(getIdProperty(), Number.class), is(nullValue()));
		verify(conversionService, times(0)).convert(1L, Number.class);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void doesNotInvokeConversionIfTypeAlreadyMatches() {

		Entity entity = new Entity();
		entity.id = 1L;

		ConversionService conversionService = mock(ConversionService.class);
		ConvertingPropertyAccessor accessor = getAccessor(entity, conversionService);

		assertThat(accessor.getProperty(getIdProperty(), Number.class), is((Number) 1L));
		verify(conversionService, times(0)).convert(1L, Number.class);
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void convertsValueOnSetIfTypesDontMatch() {

		Entity entity = new Entity();
		ConvertingPropertyAccessor accessor = getAccessor(entity, CONVERSION_SERVICE);

		accessor.setProperty(getIdProperty(), "1");

		assertThat(entity.id, is(1L));
	}

	/**
	 * @see DATACMNS-596
	 */
	@Test
	public void doesNotInvokeConversionIfTypeAlreadyMatchesOnSet() {

		ConvertingPropertyAccessor accessor = getAccessor(new Entity(), mock(ConversionService.class));

		accessor.setProperty(getIdProperty(), 1L);
		verify(mock(ConversionService.class), times(0)).convert(1L, Long.class);
	}

	private static ConvertingPropertyAccessor getAccessor(Object entity, ConversionService conversionService) {

		PersistentPropertyAccessor wrapper = new BeanWrapper<Object>(entity);
		return new ConvertingPropertyAccessor(wrapper, conversionService);
	}

	private static SamplePersistentProperty getIdProperty() {

		SampleMappingContext mappingContext = new SampleMappingContext();
		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext.getPersistentEntity(Entity.class);
		return entity.getPersistentProperty("id");
	}

	static class Entity {
		Long id;
	}
}
