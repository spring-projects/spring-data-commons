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

import org.junit.jupiter.api.Test;
import org.springframework.data.convert.PropertyValueConverterFactoryUnitTests.Person;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.lang.Nullable;

/**
 * Unit tests for {@link ValueConverterRegistry}.
 *
 * @author Christoph Strobl
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class PropertyValueConverterRegistrarUnitTests {

	@Test // GH-1484
	void buildsRegistryCorrectly() {

		ValueConverterRegistry registry = new PropertyValueConverterRegistrar<>() //
				.registerConverter(Person.class, "name", new ReversingPropertyValueConverter()) //
				.buildRegistry(); //

		assertThat(registry.containsConverterFor(Person.class, "name")).isTrue();
		assertThat(registry.containsConverterFor(Person.class, "not-a-property")).isFalse();
	}

	@Test // GH-1484
	void registersConvertersInRegistryCorrectly() {

		ValueConverterRegistry registry = ValueConverterRegistry.simple();

		new PropertyValueConverterRegistrar<>() //
				.registerConverter(Person.class, "name", new ReversingPropertyValueConverter()) //
				.registerConvertersIn(registry); //

		assertThat(registry.containsConverterFor(Person.class, "name")).isTrue();
		assertThat(registry.containsConverterFor(Person.class, "not-a-property")).isFalse();
	}

	@Test // GH-1484
	void allowsTypeSafeConverterRegistration() {

		PropertyValueConverterRegistrar<SamplePersistentProperty> registrar = new PropertyValueConverterRegistrar<>();
		registrar.registerConverter(Person.class, "name", String.class) //
				.writing(PropertyValueConverterRegistrarUnitTests::reverse) //
				.readingAsIs(); //

		PropertyValueConverter<String, String, ? extends ValueConversionContext<SamplePersistentProperty>> name = registrar
				.buildRegistry().getConverter(Person.class, "name");
		assertThat(name.write("foo", null)).isEqualTo("oof");
		assertThat(name.read("off", null)).isEqualTo("off");
	}

	@Test // GH-1484
	void allowsTypeSafeConverterRegistrationViaRecordedProperty() {

		PropertyValueConverterRegistrar<SamplePersistentProperty> registrar = new PropertyValueConverterRegistrar<>();
		registrar.registerConverter(Person.class, Person::getName) //
				.writing(PropertyValueConverterRegistrarUnitTests::reverse) //
				.readingAsIs();

		PropertyValueConverter<String, String, ? extends ValueConversionContext<SamplePersistentProperty>> name = registrar
				.buildRegistry().getConverter(Person.class, "name");
		assertThat(name.write("foo", null)).isEqualTo("oof");
		assertThat(name.read("мир", null)).isEqualTo("мир");
	}

	static class ReversingPropertyValueConverter
			implements PropertyValueConverter<String, String, ValueConversionContext<?>> {

		@Nullable
		@Override
		public String read(@Nullable String value, ValueConversionContext<?> context) {
			return PropertyValueConverterRegistrarUnitTests.reverse(value);
		}

		@Nullable
		@Override
		public String write(@Nullable String value, ValueConversionContext<?> context) {
			return PropertyValueConverterRegistrarUnitTests.reverse(value);
		}
	}

	@Nullable
	static String reverse(@Nullable String source) {

		if (source == null) {
			return null;
		}

		return new StringBuilder(source).reverse().toString();
	}
}
