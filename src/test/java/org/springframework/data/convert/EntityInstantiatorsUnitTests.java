/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.convert;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.PersistentEntity;

/**
 * Unit tests for {@link EntityInstantiators}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityInstantiatorsUnitTests {

	@Mock PersistentEntity<?, ?> entity;
	@Mock EntityInstantiator customInstantiator;

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullFallbackInstantiator() {
		new EntityInstantiators((EntityInstantiator) null);
	}

	@Test
	public void usesReflectionEntityInstantiatorAsDefaultFallback() {

		EntityInstantiators instantiators = new EntityInstantiators();
		assertThat(instantiators.getInstantiatorFor(entity),
				instanceOf(ClassGeneratingEntityInstantiator.class));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void returnsCustomInstantiatorForTypeIfRegistered() {

		when(entity.getType()).thenReturn((Class) String.class);

		Map<Class<?>, EntityInstantiator> customInstantiators = Collections.<Class<?>, EntityInstantiator> singletonMap(
				String.class, customInstantiator);

		EntityInstantiators instantiators = new EntityInstantiators(customInstantiators);
		assertThat(instantiators.getInstantiatorFor(entity), is(customInstantiator));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void usesCustomFallbackInstantiatorsIfConfigured() {

		when(entity.getType()).thenReturn((Class) Object.class);

		Map<Class<?>, EntityInstantiator> customInstantiators = Collections.<Class<?>, EntityInstantiator> singletonMap(
				String.class, ReflectionEntityInstantiator.INSTANCE);

		EntityInstantiators instantiators = new EntityInstantiators(customInstantiator, customInstantiators);
		instantiators.getInstantiatorFor(entity);

		assertThat(instantiators.getInstantiatorFor(entity), is(customInstantiator));

		when(entity.getType()).thenReturn((Class) String.class);
		assertThat(instantiators.getInstantiatorFor(entity), is((EntityInstantiator) ReflectionEntityInstantiator.INSTANCE));
	}
}
