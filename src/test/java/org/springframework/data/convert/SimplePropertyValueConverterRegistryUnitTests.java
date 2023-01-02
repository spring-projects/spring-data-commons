/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.Person;

/**
 * Unit tests for {@link SimplePropertyValueConverterRegistry}.
 *
 * @author Christoph Strobl
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class SimplePropertyValueConverterRegistryUnitTests {

	@Test // GH-1484
	void emptyRegistryDoesNotServeConverters() {

		SimplePropertyValueConverterRegistry registry = new SimplePropertyValueConverterRegistry();

		assertThat(registry.isEmpty()).isTrue();
		assertThat(registry.size()).isZero();
		assertThat(registry.containsConverterFor(Person.class, "name")).isFalse();
		assertThat(registry.getConverter(Person.class, "name")).isNull();
	}

	@Test // GH-1484
	void registryCopiesOverConverters() {

		SimplePropertyValueConverterRegistry sourceRegistry = new SimplePropertyValueConverterRegistry();
		sourceRegistry.registerConverter(Person.class, "name", mock(PropertyValueConverter.class));

		SimplePropertyValueConverterRegistry targetRegistry = new SimplePropertyValueConverterRegistry(sourceRegistry);
		assertThat(targetRegistry.size()).isOne();

		sourceRegistry.registerConverter(Address.class, "street", mock(PropertyValueConverter.class));
		assertThat(sourceRegistry.size()).isEqualTo(2);
		assertThat(targetRegistry.size()).isOne();
	}

	@Test // GH-1484
	void registryServesMatchingConverter() {

		SimplePropertyValueConverterRegistry registry = new SimplePropertyValueConverterRegistry();
		registry.registerConverter(Person.class, "name", mock(PropertyValueConverter.class));

		assertThat(registry.isEmpty()).isFalse();
		assertThat(registry.size()).isOne();

		assertThat(registry.containsConverterFor(Person.class, "name")).isTrue();
		assertThat(registry.getConverter(Person.class, "name")).isNotNull();

		assertThat(registry.containsConverterFor(Person.class, "age")).isFalse();
		assertThat(registry.getConverter(Person.class, "age")).isNull();

		assertThat(registry.getConverter(Address.class, "name")).isNull();
	}

	@Test // GH-1484
	void registryMayHoldConvertersForDifferentPropertiesOfSameType() {

		PropertyValueConverter nameConverter = mock(PropertyValueConverter.class);
		PropertyValueConverter ageConverter = mock(PropertyValueConverter.class);

		SimplePropertyValueConverterRegistry registry = new SimplePropertyValueConverterRegistry();
		registry.registerConverter(Person.class, "name", nameConverter);
		registry.registerConverter(Person.class, "age", ageConverter);

		assertThat(registry.getConverter(Person.class, "name")).isSameAs(nameConverter);
		assertThat(registry.getConverter(Person.class, "age")).isSameAs(ageConverter);
	}

	static class Address {}

}
