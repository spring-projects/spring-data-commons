/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;

/**
 * Integration tests for {@link EntityInstantiator}.
 *
 * @author Mark Paluch
 */
public class EntityInstantiatorIntegrationTests {

	SampleMappingContext context = new SampleMappingContext();
	EntityInstantiators instantiators = new EntityInstantiators();

	@Test // GH-2942
	void shouldDefaultTransientProperties() {

		WithTransientProperty instance = createInstance(WithTransientProperty.class);

		assertThat(instance.foo).isEqualTo(null);
		assertThat(instance.bar).isEqualTo(0);
	}

	@Test // GH-2942
	void shouldDefaultTransientRecordProperties() {

		RecordWithTransientProperty instance = createInstance(RecordWithTransientProperty.class);

		assertThat(instance.foo).isEqualTo(null);
		assertThat(instance.bar).isEqualTo(0);
	}

	@Test // GH-2942
	void shouldDefaultTransientKotlinProperty() {

		DataClassWithTransientProperties instance = createInstance(DataClassWithTransientProperties.class);

		// Kotlin defaulting
		assertThat(instance.getFoo()).isEqualTo("foo");

		// Our defaulting
		assertThat(instance.getBar()).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	private <E> E createInstance(Class<E> entityType) {

		var entity = context.getRequiredPersistentEntity(entityType);
		var instantiator = instantiators.getInstantiatorFor(entity);

		return (E) instantiator.createInstance(entity,
				new PersistentEntityParameterValueProvider<>(entity, new PropertyValueProvider<SamplePersistentProperty>() {
					@Override
					public <T> T getPropertyValue(SamplePersistentProperty property) {
						return null;
					}
				}, null));
	}

	static class WithTransientProperty {

		@Transient String foo;
		@Transient int bar;

		public WithTransientProperty(String foo, int bar) {

		}
	}

	record RecordWithTransientProperty(@Transient String foo, @Transient int bar) {

	}

}
