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
package org.springframework.data.convert;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.convert.ReflectionEntityInstantiator.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.convert.ReflectionEntityInstantiatorUnitTest.Outer.Inner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ReflectionEntityInstantiator}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflectionEntityInstantiatorUnitTest<P extends PersistentProperty<P>> {

	@Mock
	PersistentEntity<?, P> entity;
	@Mock
	ParameterValueProvider<P> provider;
	@Mock
	PreferredConstructor<?, P> constructor;
	@Mock
	Parameter<?, P> parameter;

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiatesSimpleObjectCorrectly() {

		when(entity.getType()).thenReturn((Class) Object.class);
		INSTANCE.createInstance(entity, provider);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiatesArrayCorrectly() {

		when(entity.getType()).thenReturn((Class) String[][].class);
		INSTANCE.createInstance(entity, provider);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<Foo, P>(Foo.class).getConstructor();

		when(entity.getType()).thenReturn((Class) Foo.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		Object instance = INSTANCE.createInstance(entity, provider);

		assertTrue(instance instanceof Foo);
		verify(provider, times(1)).getParameterValue((Parameter) constructor.getParameters().iterator().next());
	}

	/**
	 * @see DATACMNS-134
	 */
	@Test
	public void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(ClassTypeInformation.from(Inner.class));
		PreferredConstructor<Inner, P> constructor = entity.getPersistenceConstructor();
		Parameter<Object, P> parameter = constructor.getParameters().iterator().next();

		Object outer = new Outer();

		when(provider.getParameterValue(parameter)).thenReturn(outer);
		Inner instance = INSTANCE.createInstance(entity, provider);

		assertThat(instance, is(notNullValue()));
		assertThat(ReflectionTestUtils.getField(instance, "this$1"), is(outer));
	}

	static class Foo {

		Foo(String foo) {

		}
	}

	static class Outer {

		class Inner {

		}
	}
}
