/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mapping.PersistentEntity;

/**
 * Unit tests for {@link EntityInstantiators}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class EntityInstantiatorsUnitTests {

	@Mock PersistentEntity<?, ?> entity;
	@Mock EntityInstantiator customInstantiator;

	@Test
	void rejectsNullFallbackInstantiator() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EntityInstantiators((EntityInstantiator) null));
	}

	@Test
	void usesReflectionEntityInstantiatorAsDefaultFallback() {

		EntityInstantiators instantiators = new EntityInstantiators();
		assertThat(instantiators.getInstantiatorFor(entity)).isInstanceOf(ClassGeneratingEntityInstantiator.class);
	}

	@Test
	void returnsCustomInstantiatorForTypeIfRegistered() {

		doReturn(String.class).when(entity).getType();

		Map<Class<?>, EntityInstantiator> customInstantiators = Collections.singletonMap(String.class, customInstantiator);

		EntityInstantiators instantiators = new EntityInstantiators(customInstantiators);
		assertThat(instantiators.getInstantiatorFor(entity)).isEqualTo(customInstantiator);
	}

	@Test
	void usesCustomFallbackInstantiatorsIfConfigured() {

		doReturn(Object.class).when(entity).getType();

		Map<Class<?>, EntityInstantiator> customInstantiators = Collections.singletonMap(String.class,
				ReflectionEntityInstantiator.INSTANCE);

		EntityInstantiators instantiators = new EntityInstantiators(customInstantiator, customInstantiators);
		instantiators.getInstantiatorFor(entity);

		assertThat(instantiators.getInstantiatorFor(entity)).isEqualTo(customInstantiator);

		doReturn(String.class).when(entity).getType();
		assertThat(instantiators.getInstantiatorFor(entity)).isEqualTo(ReflectionEntityInstantiator.INSTANCE);
	}
}
