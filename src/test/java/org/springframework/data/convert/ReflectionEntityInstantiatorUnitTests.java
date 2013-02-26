/*
 * Copyright 2012-2013 the original author or authors.
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
import static org.springframework.data.util.ClassTypeInformation.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.convert.ReflectionEntityInstantiatorUnitTests.Outer.Inner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * Unit tests for {@link ReflectionEntityInstantiator}.
 * 
 * @author Oliver Gierke
 * @author Johannes Mockenhaupt
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflectionEntityInstantiatorUnitTests<P extends PersistentProperty<P>> {

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
	 * @see DATACMNS-300
	 */
	@Test(expected = MappingInstantiationException.class)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void throwsExceptionOnBeanInstantiationException() {

		when(entity.getPersistenceConstructor()).thenReturn(null);
		when(entity.getType()).thenReturn((Class) PersistentEntity.class);

		INSTANCE.createInstance(entity, provider);
	}

	/**
	 * @see DATACMNS-134
	 */
	@Test
	public void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(from(Inner.class));
		PreferredConstructor<Inner, P> constructor = entity.getPersistenceConstructor();
		Parameter<Object, P> parameter = constructor.getParameters().iterator().next();

		final Object outer = new Outer();

		when(provider.getParameterValue(parameter)).thenReturn(outer);
		final Inner instance = INSTANCE.createInstance(entity, provider);

		assertThat(instance, is(notNullValue()));

		// Hack to check syntheic field as compiles create different field names (e.g. this$0, this$1)
		ReflectionUtils.doWithFields(Inner.class, new FieldCallback() {
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				if (field.isSynthetic() && field.getName().startsWith("this$")) {
					ReflectionUtils.makeAccessible(field);
					assertThat(ReflectionUtils.getField(field, instance), is(outer));
				}
			}
		});
	}

	/**
	 * @see DATACMNS-283
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void capturesContextOnInstantiationException() throws Exception {

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<Sample, P>(from(Sample.class));

		when(provider.getParameterValue(Mockito.any(Parameter.class))).thenReturn("FOO");

		Constructor constructor = Sample.class.getConstructor(Long.class, String.class);
		List<Object> parameters = Arrays.asList((Object) "FOO", (Object) "FOO");

		try {

			INSTANCE.createInstance(entity, provider);
			fail("Expected MappingInstantiationException!");

		} catch (MappingInstantiationException o_O) {

			assertThat(o_O.getConstructor(), is(constructor));
			assertThat(o_O.getConstructorArguments(), is(parameters));
			assertEquals(Sample.class, o_O.getEntityType());

			assertThat(o_O.getMessage(), containsString(Sample.class.getName()));
			assertThat(o_O.getMessage(), containsString(Long.class.getName()));
			assertThat(o_O.getMessage(), containsString(String.class.getName()));
			assertThat(o_O.getMessage(), containsString("FOO"));
		}
	}

	static class Foo {

		Foo(String foo) {

		}
	}

	static class Outer {

		class Inner {

		}
	}

	static class Sample {

		final Long id;
		final String name;

		public Sample(Long id, String name) {

			this.id = id;
			this.name = name;
		}
	}
}
