/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.MappingContextEvent;

/**
 * Unit tests for {@link MappingContextEvent}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingContextEventUnitTests<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> {

	@Mock
	E entity;

	@Mock
	MappingContext<?, ?> mappingContext, otherMappingContext;

	@Test
	public void returnsPersistentEntityHandedToTheEvent() {

		MappingContextEvent<E, P> event = new MappingContextEvent<E, P>(mappingContext, entity);
		assertThat(event.getPersistentEntity(), is(entity));
	}

	@Test
	public void usesMappingContextAsEventSource() {

		MappingContextEvent<E, P> event = new MappingContextEvent<E, P>(mappingContext, entity);
		assertThat(event.getSource(), is((Object) mappingContext));
	}

	@Test
	public void detectsEmittingMappingContextCorrectly() {

		MappingContextEvent<E, P> event = new MappingContextEvent<E, P>(mappingContext, entity);
		assertThat(event.wasEmittedBy(mappingContext), is(true));
		assertThat(event.wasEmittedBy(otherMappingContext), is(false));
	}
}
