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
package org.springframework.data.mapping.model;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProviderUnitTests.Outer.Inner;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link PersistentEntityParameterValueProvider}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistentEntityParameterValueProviderUnitTests<P extends PersistentProperty<P>> {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	PropertyValueProvider<P> propertyValueProvider;
	@Mock
	P property;

	/**
	 * @see DATACMNS-134
	 */
	@Test
	public void usesParentObjectAsImplicitFirstConstructorArgument() {

		Object outer = new Outer();

		PersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(ClassTypeInformation.from(Inner.class)) {
			@Override
			public P getPersistentProperty(String name) {
				return property;
			}
		};
		PreferredConstructor<Inner, P> constructor = entity.getPersistenceConstructor();
		Iterator<Parameter<Object, P>> iterator = constructor.getParameters().iterator();

		ParameterValueProvider<P> provider = new PersistentEntityParameterValueProvider<P>(entity, propertyValueProvider,
				outer);
		assertThat(provider.getParameterValue(iterator.next()), is(outer));
		assertThat(provider.getParameterValue(iterator.next()), is(nullValue()));
		assertThat(iterator.hasNext(), is(false));
	}

	@Test
	public void rejectsPropertyIfNameDoesNotMatch() {

		PersistentEntity<Entity, P> entity = new BasicPersistentEntity<Entity, P>(ClassTypeInformation.from(Entity.class));
		ParameterValueProvider<P> provider = new PersistentEntityParameterValueProvider<P>(entity, propertyValueProvider,
				property);

		PreferredConstructor<Entity, P> constructor = entity.getPersistenceConstructor();

		exception.expect(MappingException.class);
		exception.expectMessage("bar");
		exception.expectMessage(Entity.class.getName());

		provider.getParameterValue(constructor.getParameters().iterator().next());
	}

	static class Outer {

		class Inner {

			Object myObject;

			Inner(Object myObject) {

			}
		}
	}

	static class Entity {

		String foo;

		public Entity(String bar) {

		}
	}
}
