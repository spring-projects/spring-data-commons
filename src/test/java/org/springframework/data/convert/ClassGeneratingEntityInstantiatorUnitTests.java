/*
 * Copyright 2014-2017 the original author or authors.
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
import org.springframework.data.convert.ClassGeneratingEntityInstantiatorUnitTests.Outer.Inner;
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
 * Unit tests for {@link ClassGeneratingEntityInstantiator}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ClassGeneratingEntityInstantiatorUnitTests<P extends PersistentProperty<P>> {

	ClassGeneratingEntityInstantiator instance = new ClassGeneratingEntityInstantiator();

	@Mock PersistentEntity<?, P> entity;
	@Mock ParameterValueProvider<P> provider;
	@Mock PreferredConstructor<?, P> constructor;
	@Mock Parameter<?, P> parameter;

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiatesSimpleObjectCorrectly() {

		when(entity.getType()).thenReturn((Class) Object.class);
		this.instance.createInstance(entity, provider);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiatesArrayCorrectly() {

		when(entity.getType()).thenReturn((Class) String[][].class);
		this.instance.createInstance(entity, provider);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<Foo, P>(Foo.class).getConstructor();

		when(entity.getType()).thenReturn((Class) Foo.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		Object instance = this.instance.createInstance(entity, provider);

		assertTrue(instance instanceof Foo);
		verify(provider, times(1)).getParameterValue((Parameter) constructor.getParameters().iterator().next());
	}

	@Test(expected = MappingInstantiationException.class) // DATACMNS-300, DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void throwsExceptionOnBeanInstantiationException() {

		when(entity.getPersistenceConstructor()).thenReturn(null);
		when(entity.getType()).thenReturn((Class) PersistentEntity.class);

		this.instance.createInstance(entity, provider);
	}

	@Test // DATACMNS-134, DATACMNS-578
	public void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(from(Inner.class));
		PreferredConstructor<Inner, P> constructor = entity.getPersistenceConstructor();
		Parameter<Object, P> parameter = constructor.getParameters().iterator().next();

		final Object outer = new Outer();

		when(provider.getParameterValue(parameter)).thenReturn(outer);
		final Inner instance = this.instance.createInstance(entity, provider);

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

	@Test // DATACMNS-283, DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void capturesContextOnInstantiationException() throws Exception {

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<Sample, P>(from(Sample.class));

		when(provider.getParameterValue(Mockito.any(Parameter.class))).thenReturn("FOO");

		Constructor constructor = Sample.class.getConstructor(Long.class, String.class);
		List<Object> parameters = Arrays.asList((Object) "FOO", (Object) "FOO");

		try {

			this.instance.createInstance(entity, provider);
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

	@Test // DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiateObjCtorDefault() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<ObjCtorDefault, P>(ObjCtorDefault.class)
				.getConstructor();

		when(entity.getType()).thenReturn((Class) ObjCtorDefault.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		for (int i = 0; i < 2; i++) {
			Object instance = this.instance.createInstance(entity, provider);
			assertTrue(instance instanceof ObjCtorDefault);
		}
	}

	@Test // DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiateObjCtorNoArgs() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<ObjCtorNoArgs, P>(ObjCtorNoArgs.class)
				.getConstructor();

		when(entity.getType()).thenReturn((Class) ObjCtorNoArgs.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		for (int i = 0; i < 2; i++) {
			Object instance = this.instance.createInstance(entity, provider);
			assertTrue(instance instanceof ObjCtorNoArgs);
			assertTrue(((ObjCtorNoArgs) instance).ctorInvoked);
		}
	}

	@Test // DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiateObjCtor1ParamString() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<ObjCtor1ParamString, P>(
				ObjCtor1ParamString.class).getConstructor();

		when(entity.getType()).thenReturn((Class) ObjCtor1ParamString.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		when(provider.getParameterValue(Mockito.any(Parameter.class))).thenReturn("FOO");

		for (int i = 0; i < 2; i++) {
			Object instance = this.instance.createInstance(entity, provider);
			assertTrue(instance instanceof ObjCtor1ParamString);
			assertTrue(((ObjCtor1ParamString) instance).ctorInvoked);
			assertThat(((ObjCtor1ParamString) instance).param1, is("FOO"));
		}
	}

	@Test // DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiateObjCtor2ParamStringString() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<ObjCtor2ParamStringString, P>(
				ObjCtor2ParamStringString.class).getConstructor();

		when(entity.getType()).thenReturn((Class) ObjCtor2ParamStringString.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		for (int i = 0; i < 2; i++) {
			when(provider.getParameterValue(Mockito.any(Parameter.class))).thenReturn("FOO").thenReturn("BAR");

			Object instance = this.instance.createInstance(entity, provider);
			assertTrue(instance instanceof ObjCtor2ParamStringString);
			assertTrue(((ObjCtor2ParamStringString) instance).ctorInvoked);
			assertThat(((ObjCtor2ParamStringString) instance).param1, is("FOO"));
			assertThat(((ObjCtor2ParamStringString) instance).param2, is("BAR"));
		}
	}

	@Test // DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiateObjectCtor1ParamInt() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<ObjectCtor1ParamInt, P>(
				ObjectCtor1ParamInt.class).getConstructor();

		when(entity.getType()).thenReturn((Class) ObjectCtor1ParamInt.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		for (int i = 0; i < 2; i++) {

			when(provider.getParameterValue(Mockito.any(Parameter.class))).thenReturn(42);

			Object instance = this.instance.createInstance(entity, provider);
			assertTrue(instance instanceof ObjectCtor1ParamInt);
			assertTrue("matches", ((ObjectCtor1ParamInt) instance).param1 == 42);
		}
	}

	@Test // DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void instantiateObjectCtor7ParamsString5IntsString() {

		PreferredConstructor constructor = new PreferredConstructorDiscoverer<ObjectCtor7ParamsString5IntsString, P>(
				ObjectCtor7ParamsString5IntsString.class).getConstructor();

		when(entity.getType()).thenReturn((Class) ObjectCtor7ParamsString5IntsString.class);
		when(entity.getPersistenceConstructor()).thenReturn(constructor);

		for (int i = 0; i < 2; i++) {
			when(provider.getParameterValue(Mockito.any(Parameter.class))).thenReturn("A").thenReturn(1).thenReturn(2)
					.thenReturn(3).thenReturn(4).thenReturn(5).thenReturn("B");

			Object instance = this.instance.createInstance(entity, provider);
			assertTrue(instance instanceof ObjectCtor7ParamsString5IntsString);
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param1, is("A"));
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param2, is(1));
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param3, is(2));
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param4, is(3));
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param5, is(4));
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param6, is(5));
			assertThat(((ObjectCtor7ParamsString5IntsString) instance).param7, is("B"));
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

	/**
	 * @author Thomas Darimont
	 */
	public static class ObjCtorDefault {}

	/**
	 * @author Thomas Darimont
	 */
	public static class ObjCtorNoArgs {

		public boolean ctorInvoked;

		public ObjCtorNoArgs() {
			ctorInvoked = true;
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class ObjCtor1ParamString {

		public boolean ctorInvoked;
		public String param1;

		public ObjCtor1ParamString(String param1) {
			this.param1 = param1;
			this.ctorInvoked = true;
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class ObjCtor2ParamStringString {

		public boolean ctorInvoked;
		public String param1;
		public String param2;

		public ObjCtor2ParamStringString(String param1, String param2) {
			this.ctorInvoked = true;
			this.param1 = param1;
			this.param2 = param2;
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class ObjectCtor1ParamInt {

		public int param1;

		public ObjectCtor1ParamInt(int param1) {
			this.param1 = param1;
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class ObjectCtor7ParamsString5IntsString {

		public String param1;
		public int param2;
		public int param3;
		public int param4;
		public int param5;
		public int param6;
		public String param7;

		public ObjectCtor7ParamsString5IntsString(String param1, int param2, int param3, int param4, int param5,
				int param6, String param7) {
			this.param1 = param1;
			this.param2 = param2;
			this.param3 = param3;
			this.param4 = param4;
			this.param5 = param5;
			this.param6 = param6;
			this.param7 = param7;
		}
	}
}
