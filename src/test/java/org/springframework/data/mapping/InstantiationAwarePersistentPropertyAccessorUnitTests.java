/*
 * Copyright 2019-2021 the original author or authors.
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

import lombok.Value;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.InstantiationAwarePropertyAccessor;

/**
 * Unit tests for {@link InstantiationAwarePropertyAccessor}.
 *
 * @author Oliver Drotbohm
 * @author Mark Paluch
 */
class InstantiationAwarePersistentPropertyAccessorUnitTests {

	@Test // DATACMNS-1639
	void shouldCreateNewInstance() {

		EntityInstantiators instantiators = new EntityInstantiators();
		SampleMappingContext context = new SampleMappingContext();

		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Sample.class);

		PersistentPropertyAccessor<Sample> accessor = entity.getPropertyAccessor(new Sample("Dave", "Matthews", 42));

		PersistentPropertyAccessor<Sample> wrapper = new InstantiationAwarePropertyAccessor<>(accessor,
				instantiators);

		wrapper.setProperty(entity.getRequiredPersistentProperty("firstname"), "Oliver August");

		assertThat(wrapper.getBean()).isEqualTo(new Sample("Oliver August", "Matthews", 42));
	}

	@Test // DATACMNS-1768
	void shouldSetMultipleProperties() {

		EntityInstantiators instantiators = new EntityInstantiators();
		SampleMappingContext context = new SampleMappingContext();

		PersistentEntity<Object, SamplePersistentProperty> entity = context.getRequiredPersistentEntity(Sample.class);

		Sample bean = new Sample("Dave", "Matthews", 42);

		PersistentPropertyAccessor<Sample> wrapper = new InstantiationAwarePropertyAccessor<>(bean,
				entity::getPropertyAccessor, instantiators);

		wrapper.setProperty(entity.getRequiredPersistentProperty("firstname"), "Oliver August");
		wrapper.setProperty(entity.getRequiredPersistentProperty("lastname"), "Heisenberg");

		assertThat(wrapper.getBean()).isEqualTo(new Sample("Oliver August", "Heisenberg", 42));
	}

	@Value
	static class Sample {

		String firstname, lastname;
		int age;
	}
}
