/*
 * Copyright 2014 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.util.ClassTypeInformation.*;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

	@Before
	public void setUp() {
		doReturn(Optional.empty()).when(entity).getPersistenceConstructor();
	}

	@Test
	public void instantiatesSimpleObjectCorrectly() {

		doReturn(Object.class).when(entity).getType();

		this.instance.createInstance(entity, provider);
	}

	@Test
	public void instantiatesArrayCorrectly() {

		doReturn(String[][].class).when(entity).getType();

		this.instance.createInstance(entity, provider);
	}

	@Test
	public void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {

		Optional<? extends PreferredConstructor<Foo, P>> constructor = new PreferredConstructorDiscoverer<Foo, P>(Foo.class)
				.getConstructor();

		doReturn(Foo.class).when(entity).getType();
		doReturn(constructor).when(entity).getPersistenceConstructor();

		assertThat(instance.createInstance(entity, provider)).isInstanceOf(Foo.class);

		assertThat(constructor).hasValueSatisfying(it -> {
			verify(provider, times(1)).getParameterValue(it.getParameters().iterator().next());
		});
	}

	/**
	 * @see DATACMNS-300, DATACMNS-578
	 */
	@Test(expected = MappingInstantiationException.class)
	public void throwsExceptionOnBeanInstantiationException() {

		doReturn(Optional.empty()).when(entity).getPersistenceConstructor();
		doReturn(PersistentEntity.class).when(entity).getType();

		this.instance.createInstance(entity, provider);
	}

	/**
	 * @see DATACMNS-134, DATACMNS-578
	 */
	@Test
	public void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(from(Inner.class));
		assertThat(entity.getPersistenceConstructor()).hasValueSatisfying(constructor -> {

			Parameter<Object, P> parameter = constructor.getParameters().iterator().next();

			Object outer = new Outer();

			doReturn(Optional.of(outer)).when(provider).getParameterValue(parameter);
			Inner instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isNotNull();

			// Hack to check synthetic field as compiles create different field names (e.g. this$0, this$1)
			ReflectionUtils.doWithFields(Inner.class, field -> {
				if (field.isSynthetic() && field.getName().startsWith("this$")) {
					ReflectionUtils.makeAccessible(field);
					assertThat(ReflectionUtils.getField(field, instance)).isEqualTo(outer);
				}
			});
		});
	}

	/**
	 * @see DATACMNS-283, DATACMNS-578
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void capturesContextOnInstantiationException() throws Exception {

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<>(from(Sample.class));

		doReturn(Optional.of("FOO")).when(provider).getParameterValue(any(Parameter.class));

		Constructor constructor = Sample.class.getConstructor(Long.class, String.class);
		List<Object> parameters = Arrays.asList("FOO", "FOO");

		try {

			this.instance.createInstance(entity, provider);
			fail("Expected MappingInstantiationException!");

		} catch (MappingInstantiationException o_O) {

			assertThat(o_O.getConstructor()).hasValue(constructor);
			assertThat(o_O.getConstructorArguments()).isEqualTo(parameters);
			assertThat(o_O.getEntityType()).hasValue(Sample.class);

			assertThat(o_O.getMessage()).contains(Sample.class.getName());
			assertThat(o_O.getMessage()).contains(Long.class.getName());
			assertThat(o_O.getMessage()).contains(String.class.getName());
			assertThat(o_O.getMessage()).contains("FOO");
		}
	}

	/**
	 * @see DATACMNS-578
	 */
	@Test
	public void instantiateObjCtorDefault() {

		doReturn(ObjCtorDefault.class).when(entity).getType();
		doReturn(new PreferredConstructorDiscoverer<>(ObjCtorDefault.class).getConstructor())//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {
			assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(ObjCtorDefault.class);
		});
	}

	/**
	 * @see DATACMNS-578
	 */
	@Test
	public void instantiateObjCtorNoArgs() {

		doReturn(ObjCtorNoArgs.class).when(entity).getType();
		doReturn(new PreferredConstructorDiscoverer<>(ObjCtorNoArgs.class).getConstructor())//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtorNoArgs.class);
			assertThat(((ObjCtorNoArgs) instance).ctorInvoked).isTrue();
		});
	}

	/**
	 * @see DATACMNS-578
	 */
	@Test
	public void instantiateObjCtor1ParamString() {

		doReturn(ObjCtor1ParamString.class).when(entity).getType();
		doReturn(new PreferredConstructorDiscoverer<>(ObjCtor1ParamString.class).getConstructor())//
				.when(entity).getPersistenceConstructor();
		doReturn(Optional.of("FOO")).when(provider).getParameterValue(any());

		IntStream.range(0, 2).forEach(i -> {

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtor1ParamString.class);
			assertThat(((ObjCtor1ParamString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor1ParamString) instance).param1).isEqualTo("FOO");
		});
	}

	/**
	 * @see DATACMNS-578
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void instantiateObjCtor2ParamStringString() {

		doReturn(ObjCtor2ParamStringString.class).when(entity).getType();
		doReturn(new PreferredConstructorDiscoverer<>(ObjCtor2ParamStringString.class).getConstructor())//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			when(provider.getParameterValue(any())).thenReturn(Optional.of("FOO"), Optional.of("BAR"));

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtor2ParamStringString.class);
			assertThat(((ObjCtor2ParamStringString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor2ParamStringString) instance).param1).isEqualTo("FOO");
			assertThat(((ObjCtor2ParamStringString) instance).param2).isEqualTo("BAR");
		});
	}

	/**
	 * @see DATACMNS-578
	 */
	@Test
	public void instantiateObjectCtor1ParamInt() {

		doReturn(ObjectCtor1ParamInt.class).when(entity).getType();
		doReturn(new PreferredConstructorDiscoverer<>(ObjectCtor1ParamInt.class).getConstructor())//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			doReturn(Optional.of(42)).when(provider).getParameterValue(any());

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjectCtor1ParamInt.class);
			assertThat(((ObjectCtor1ParamInt) instance).param1).isEqualTo(42);
		});
	}

	/**
	 * @see DATACMNS-578
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void instantiateObjectCtor7ParamsString5IntsString() {

		doReturn(ObjectCtor7ParamsString5IntsString.class).when(entity).getType();
		doReturn(new PreferredConstructorDiscoverer<>(ObjectCtor7ParamsString5IntsString.class).getConstructor())//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			when(provider.getParameterValue(any(Parameter.class))).thenReturn(Optional.of("A"), Optional.of(1),
					Optional.of(2), Optional.of(3), Optional.of(4), Optional.of(5), Optional.of("B"));

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjectCtor7ParamsString5IntsString.class);

			ObjectCtor7ParamsString5IntsString toTest = (ObjectCtor7ParamsString5IntsString) instance;

			assertThat(toTest.param1).isEqualTo("A");
			assertThat(toTest.param2).isEqualTo(1);
			assertThat(toTest.param3).isEqualTo(2);
			assertThat(toTest.param4).isEqualTo(3);
			assertThat(toTest.param5).isEqualTo(4);
			assertThat(toTest.param6).isEqualTo(5);
			assertThat(toTest.param7).isEqualTo("B");
		});
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

		public ObjectCtor7ParamsString5IntsString(String param1, int param2, int param3, int param4, int param5, int param6,
				String param7) {
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
