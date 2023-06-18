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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.Predicates;

/**
 * Unit tests for {@link PropertyValueConversionService}.
 *
 * @author Mark Paluch
 */
class PropertyValueConversionServiceUnitTests {

	SampleMappingContext mappingContext = new SampleMappingContext();

	PropertyValueConversions conversions = PropertyValueConversions.simple(it -> {
		it.registerConverter(Person.class, "firstName", String.class).writing(w -> "Writing " + w)
				.reading(r -> "Reading " + r);
	});
	PropertyValueConversionService service = createConversionService(conversions);

	@Test // GH-2557
	void shouldReportConverter() {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Person.class);

		assertThat(service.hasConverter(entity.getRequiredPersistentProperty("firstName"))).isTrue();
		assertThat(service.hasConverter(entity.getRequiredPersistentProperty("lastName"))).isFalse();
	}

	@Test // GH-2557
	void conversionWithoutConverterShouldFail() {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Person.class);

		SamplePersistentProperty property = entity.getRequiredPersistentProperty("lastName");
		assertThatIllegalArgumentException().isThrownBy(() -> service.read("foo", property, () -> property));
		assertThatIllegalArgumentException().isThrownBy(() -> service.write("foo", property, () -> property));
	}

	@Test // GH-2557
	void readShouldUseReadConverter() {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Person.class);

		SamplePersistentProperty property = entity.getRequiredPersistentProperty("firstName");
		assertThat(service.read("Walter", property, () -> property)).isEqualTo("Reading Walter");
		assertThat(service.read(null, property, () -> property)).isEqualTo("Reading null");
	}

	@Test // GH-2557
	void readShouldUseWriteConverter() {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Person.class);

		SamplePersistentProperty property = entity.getRequiredPersistentProperty("firstName");
		assertThat(service.write("Walter", property, () -> property)).isEqualTo("Writing Walter");
		assertThat(service.write(null, property, () -> property)).isEqualTo("Writing null");
	}

	@Test // GH-2557
	void readShouldUseNullConvertersConverter() {

		PropertyValueConversions conversions = PropertyValueConversions.simple(it -> {
			it.registerConverter(Person.class, "firstName", WithNullConverters.INSTANCE);
		});

		PropertyValueConversionService service = createConversionService(conversions);

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(Person.class);

		SamplePersistentProperty property = entity.getRequiredPersistentProperty("firstName");

		assertThat(service.read(null, property, () -> property)).isEqualTo("readNull");
		assertThat(service.write(null, property, () -> property)).isEqualTo("writeNull");
	}

	private static PropertyValueConversionService createConversionService(PropertyValueConversions conversions) {

		CustomConversions.ConverterConfiguration configuration = new CustomConversions.ConverterConfiguration(
				CustomConversions.StoreConversions.NONE, Collections.emptyList(), Predicates.isTrue(), conversions);

		return new PropertyValueConversionService(new CustomConversions(configuration));
	}

	enum WithNullConverters implements PropertyValueConverter<String, String, ValueConversionContext<?>> {
		INSTANCE;

		@Override
		public String read(String value, ValueConversionContext<?> context) {
			return value;
		}

		@Override
		public String readNull(ValueConversionContext<?> context) {
			return "readNull";
		}

		@Override
		public String write(String value, ValueConversionContext<?> context) {
			return value;
		}

		@Override
		public String writeNull(ValueConversionContext<?> context) {
			return "writeNull";
		}
	}

}
