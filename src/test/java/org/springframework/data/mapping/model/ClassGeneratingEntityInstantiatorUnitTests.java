/*
 * Copyright 2014-2021 the original author or authors.
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.util.ClassTypeInformation.from;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ClassGeneratingEntityInstantiator.ObjectInstantiator;
import org.springframework.data.mapping.model.ClassGeneratingEntityInstantiatorUnitTests.Outer.Inner;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link ClassGeneratingEntityInstantiator}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassGeneratingEntityInstantiatorUnitTests<P extends PersistentProperty<P>> {

	ClassGeneratingEntityInstantiator instance = new ClassGeneratingEntityInstantiator();

	@Mock PersistentEntity<?, P> entity;
	@Mock ParameterValueProvider<P> provider;

	@Test
	void instantiatesSimpleObjectCorrectly() {

		doReturn(Object.class).when(entity).getType();

		this.instance.createInstance(entity, provider);
	}

	@Test
	void instantiatesArrayCorrectly() {

		doReturn(String[][].class).when(entity).getType();

		this.instance.createInstance(entity, provider);
	}

	@Test // DATACMNS-1126
	void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {

		PreferredConstructor<Foo, P> constructor = PreferredConstructorDiscoverer.discover(Foo.class);

		doReturn(Foo.class).when(entity).getType();
		doReturn(constructor).when(entity).getPersistenceConstructor();

		assertThat(instance.createInstance(entity, provider)).isInstanceOf(Foo.class);

		assertThat(constructor)
				.satisfies(it -> verify(provider, times(1)).getParameterValue(it.getParameters().iterator().next()));
	}

	@Test // DATACMNS-300, DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void throwsExceptionOnBeanInstantiationException() {

		doReturn(PersistentEntity.class).when(entity).getType();

		assertThatExceptionOfType(MappingInstantiationException.class)
				.isThrownBy(() -> this.instance.createInstance(entity, provider));
	}

	@Test // DATACMNS-134, DATACMNS-578
	void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<>(from(Inner.class));
		assertThat(entity.getPersistenceConstructor()).satisfies(constructor -> {

			Parameter<Object, P> parameter = constructor.getParameters().iterator().next();

			Object outer = new Outer();

			doReturn(outer).when(provider).getParameterValue(parameter);
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

	@Test // DATACMNS-283, DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void capturesContextOnInstantiationException() throws Exception {

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<>(from(Sample.class));

		doReturn("FOO").when(provider).getParameterValue(any(Parameter.class));

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

	@Test // DATACMNS-1175
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void createsInstancesWithRecursionAndSameCtorArgCountCorrectly() {

		PersistentEntity<SampleWithReference, P> outer = new BasicPersistentEntity<>(from(SampleWithReference.class));
		PersistentEntity<Sample, P> inner = new BasicPersistentEntity<>(from(Sample.class));

		doReturn(2L, "FOO").when(provider).getParameterValue(any(Parameter.class));

		ParameterValueProvider<P> recursive = new ParameterValueProvider<P>() {

			@Override
			public <T> T getParameterValue(Parameter<T, P> parameter) {

				if (parameter.getName().equals("id")) {
					return (T) Long.valueOf(1);
				}

				if (parameter.getName().equals("sample")) {
					return (T) instance.createInstance(inner, provider);
				}

				throw new UnsupportedOperationException(parameter.getName());
			}
		};

		SampleWithReference reference = this.instance.createInstance(outer, recursive);

		assertThat(reference.id).isEqualTo(1L);
		assertThat(reference.sample).isNotNull();
		assertThat(reference.sample.id).isEqualTo(2L);
		assertThat(reference.sample.name).isEqualTo("FOO");
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtorDefault() {

		doReturn(ObjCtorDefault.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtorDefault.class))//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2)
				.forEach(i -> assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(ObjCtorDefault.class));
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtorNoArgs() {

		doReturn(ObjCtorNoArgs.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtorNoArgs.class))//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtorNoArgs.class);
			assertThat(((ObjCtorNoArgs) instance).ctorInvoked).isTrue();
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtor1ParamString() {

		doReturn(ObjCtor1ParamString.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtor1ParamString.class))//
				.when(entity).getPersistenceConstructor();
		doReturn("FOO").when(provider).getParameterValue(any());

		IntStream.range(0, 2).forEach(i -> {

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtor1ParamString.class);
			assertThat(((ObjCtor1ParamString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor1ParamString) instance).param1).isEqualTo("FOO");
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtor2ParamStringString() {

		doReturn(ObjCtor2ParamStringString.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtor2ParamStringString.class))//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			when(provider.getParameterValue(any())).thenReturn("FOO", "BAR");

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtor2ParamStringString.class);
			assertThat(((ObjCtor2ParamStringString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor2ParamStringString) instance).param1).isEqualTo("FOO");
			assertThat(((ObjCtor2ParamStringString) instance).param2).isEqualTo("BAR");
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjectCtor1ParamInt() {

		doReturn(ObjectCtor1ParamInt.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjectCtor1ParamInt.class))//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			doReturn(42).when(provider).getParameterValue(any());

			Object instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjectCtor1ParamInt.class);
			assertThat(((ObjectCtor1ParamInt) instance).param1).isEqualTo(42);
		});
	}

	@Test // DATACMNS-1200
	void instantiateObjectCtor1ParamIntWithoutValue() {

		doReturn(ObjectCtor1ParamInt.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjectCtor1ParamInt.class))//
				.when(entity).getPersistenceConstructor();

		assertThatThrownBy(() -> this.instance.createInstance(entity, provider)) //
				.hasCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATACMNS-578, DATACMNS-1126
	@SuppressWarnings("unchecked")
	void instantiateObjectCtor7ParamsString5IntsString() {

		doReturn(ObjectCtor7ParamsString5IntsString.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjectCtor7ParamsString5IntsString.class))//
				.when(entity).getPersistenceConstructor();

		IntStream.range(0, 2).forEach(i -> {

			when(provider.getParameterValue(any(Parameter.class))).thenReturn("A", 1, 2, 3, 4, 5, "B");

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

	@Test // DATACMNS-1373
	void shouldInstantiateProtectedInnerClass() {

		prepareMocks(ProtectedInnerClass.class);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isFalse();
		assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(ProtectedInnerClass.class);
	}

	@Test // DATACMNS-1373
	void shouldInstantiatePackagePrivateInnerClass() {

		prepareMocks(PackagePrivateInnerClass.class);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isFalse();
		assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(PackagePrivateInnerClass.class);
	}

	@Test // DATACMNS-1373
	void shouldNotInstantiatePrivateInnerClass() {

		prepareMocks(PrivateInnerClass.class);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isTrue();
	}

	@Test // DATACMNS-1373
	void shouldInstantiateClassWithPackagePrivateConstructor() {

		prepareMocks(ClassWithPackagePrivateConstructor.class);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isFalse();
		assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(ClassWithPackagePrivateConstructor.class);
	}

	@Test // DATACMNS-1373
	void shouldInstantiateClassInDefaultPackage() throws ClassNotFoundException {

		Class<?> typeInDefaultPackage = Class.forName("TypeInDefaultPackage");
		prepareMocks(typeInDefaultPackage);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isFalse();
		assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(typeInDefaultPackage);
	}

	@Test // DATACMNS-1373
	void shouldNotInstantiateClassWithPrivateConstructor() {

		prepareMocks(ClassWithPrivateConstructor.class);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isTrue();
	}

	@Test // DATACMNS-1422
	void shouldUseReflectionIfFrameworkTypesNotVisible() throws Exception {

		HidingClassLoader classLoader = HidingClassLoader.hide(ObjectInstantiator.class);
		classLoader.excludePackage("org.springframework.data.mapping");

		// require type from different package to meet visibility quirks
		Class<?> entityType = classLoader.loadClass("org.springframework.data.mapping.Person");

		prepareMocks(entityType);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isTrue();
	}

	private void prepareMocks(Class<?> type) {

		doReturn(type).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(type))//
				.when(entity).getPersistenceConstructor();
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

	static class SampleWithReference {

		final Long id;
		final Sample sample;

		public SampleWithReference(Long id, Sample sample) {

			this.id = id;
			this.sample = sample;
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

	protected static class ProtectedInnerClass {}

	static class PackagePrivateInnerClass {}

	private static class PrivateInnerClass {}

	static class ClassWithPrivateConstructor {

		private ClassWithPrivateConstructor() {}
	}

	static class ClassWithPackagePrivateConstructor {

		ClassWithPackagePrivateConstructor() {}
	}

}
