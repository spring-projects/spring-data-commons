/*
 * Copyright 2016-2025 the original author or authors.
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

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;

/**
 * Unit tests for {@link ClassGeneratingPropertyAccessorFactory} covering interface and concrete class entity types.
 *
 * @author John Blum
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ClassGeneratingPropertyAccessorFactoryEntityTypeTests {

	private SampleMappingContext mappingContext = new SampleMappingContext();

	@Test // DATACMNS-853
	void getIdentifierOfInterfaceBasedEntity() {

		Algorithm quickSort = new QuickSort();

		assertThat(getIdentifier(Algorithm.class, quickSort)).isEqualTo(quickSort.getName());
	}

	@Test // DATACMNS-853
	void getIdentifierOfClassBasedEntity() {

		var jonDoe = new Person("JonDoe");

		assertThat(getIdentifier(Person.class, jonDoe)).isEqualTo(jonDoe.name);
	}

	@Test // #2324
	void shouldGeneratePropertyAccessorForKotlinClassWithMultipleCopyMethods() {

		var factory = new ClassGeneratingPropertyAccessorFactory();
		var propertyAccessor = factory.getPropertyAccessor(
				mappingContext.getRequiredPersistentEntity(WithCustomCopyMethod.class),
				new WithCustomCopyMethod("", "", "", 1, LocalDateTime.MAX, LocalDateTime.MAX, ""));

		assertThat(propertyAccessor).isNotNull();
	}

	private Object getIdentifier(Class<?> type, Object object) {

		PersistentEntity<Object, SamplePersistentProperty> entity = mappingContext.getRequiredPersistentEntity(type);

		return entity.getIdentifierAccessor(object).getIdentifier();
	}

	interface Algorithm {

		@Id
		String getName();
	}

	class QuickSort implements Algorithm {

		@Override
		public String getName() {
			return getClass().toString();
		}
	}

	static class Person {

		@Id String name;

		Person(String name) {
			this.name = name;
		}
	}
}
