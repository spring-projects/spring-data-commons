/*
 * Copyright 2014-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.ClassGeneratingEntityInstantiator.ObjectInstantiator;
import org.springframework.data.mapping.model.ClassGeneratingEntityInstantiatorUnitTests.Outer.Inner;
import org.springframework.data.test.classloadersupport.HidingClassLoader;
import org.springframework.data.util.TypeInformation;
import org.springframework.test.util.ReflectionTestUtils;
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

	ClassGeneratingEntityInstantiator instance = new ClassGeneratingEntityInstantiator(false);

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
		doReturn(constructor).when(entity).getInstanceCreatorMetadata();

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

		var entity = new BasicPersistentEntity<Inner, P>(TypeInformation.of(Inner.class));
		assertThat(entity.getInstanceCreatorMetadata()).satisfies(constructor -> {

			var parameter = constructor.getParameters().iterator().next();

			Object outer = new Outer();

			doReturn(outer).when(provider).getParameterValue(parameter);
			var instance = this.instance.createInstance(entity, provider);

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

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<>(TypeInformation.of(Sample.class));

		doReturn("FOO").when(provider).getParameterValue(any(Parameter.class));

		List<Object> parameters = Arrays.asList("FOO", "FOO");

		try {

			this.instance.createInstance(entity, provider);
			fail("Expected MappingInstantiationException");

		} catch (MappingInstantiationException o_O) {

			assertThat(o_O.getConstructorArguments()).isEqualTo(parameters);
			assertThat(o_O.getEntityType()).isEqualTo(Sample.class);

			assertThat(o_O.getMessage()).contains(Sample.class.getName());
			assertThat(o_O.getMessage()).contains(Long.class.getName());
			assertThat(o_O.getMessage()).contains(String.class.getName());
			assertThat(o_O.getMessage()).contains("FOO");
		}
	}

	@Test // DATACMNS-1175
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void createsInstancesWithRecursionAndSameCtorArgCountCorrectly() {

		PersistentEntity<SampleWithReference, P> outer = new BasicPersistentEntity<>(
				TypeInformation.of(SampleWithReference.class));
		PersistentEntity<Sample, P> inner = new BasicPersistentEntity<>(TypeInformation.of(Sample.class));

		doReturn(2L, "FOO").when(provider).getParameterValue(any(Parameter.class));

		var recursive = new ParameterValueProvider<P>() {

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

		var reference = this.instance.createInstance(outer, recursive);

		assertThat(reference.id).isEqualTo(1L);
		assertThat(reference.sample).isNotNull();
		assertThat(reference.sample.id).isEqualTo(2L);
		assertThat(reference.sample.name).isEqualTo("FOO");
	}

	@Test // DATACMNS-1175
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void createsInstancesWithFactoryMethodCorrectly() {

		PersistentEntity<WithFactoryMethod, P> entity = new BasicPersistentEntity<>(
				TypeInformation.of(WithFactoryMethod.class));

		doReturn(2L, "FOO").when(provider).getParameterValue(any(Parameter.class));

		var provider = new ParameterValueProvider<P>() {

			@Override
			public <T> T getParameterValue(Parameter<T, P> parameter) {

				if (parameter.getName().equals("id")) {
					return (T) Long.valueOf(1);
				}

				if (parameter.getName().equals("name")) {
					return (T) "Walter";
				}

				throw new UnsupportedOperationException(parameter.getName());
			}
		};

		var result = this.instance.createInstance(entity, provider);

		assertThat(result.id).isEqualTo(1L);
		assertThat(result.name).isEqualTo("Hello Walter");
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtorDefault() {

		doReturn(ObjCtorDefault.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtorDefault.class))//
				.when(entity).getInstanceCreatorMetadata();

		IntStream.range(0, 2)
				.forEach(i -> assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(ObjCtorDefault.class));
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtorNoArgs() {

		doReturn(ObjCtorNoArgs.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtorNoArgs.class))//
				.when(entity).getInstanceCreatorMetadata();

		IntStream.range(0, 2).forEach(i -> {

			var instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtorNoArgs.class);
			assertThat(((ObjCtorNoArgs) instance).ctorInvoked).isTrue();
		});

		var classGeneratingEntityInstantiator = new ClassGeneratingEntityInstantiator();
		classGeneratingEntityInstantiator.createInstance(entity, provider);
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtor1ParamString() {

		doReturn(ObjCtor1ParamString.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtor1ParamString.class))//
				.when(entity).getInstanceCreatorMetadata();
		doReturn("FOO").when(provider).getParameterValue(any());

		IntStream.range(0, 2).forEach(i -> {

			var instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjCtor1ParamString.class);
			assertThat(((ObjCtor1ParamString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor1ParamString) instance).param1).isEqualTo("FOO");
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtor2ParamStringString() {

		doReturn(ObjCtor2ParamStringString.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjCtor2ParamStringString.class))//
				.when(entity).getInstanceCreatorMetadata();

		IntStream.range(0, 2).forEach(i -> {

			when(provider.getParameterValue(any())).thenReturn("FOO", "BAR");

			var instance = this.instance.createInstance(entity, provider);

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
				.when(entity).getInstanceCreatorMetadata();

		IntStream.range(0, 2).forEach(i -> {

			doReturn(42).when(provider).getParameterValue(any());

			var instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjectCtor1ParamInt.class);
			assertThat(((ObjectCtor1ParamInt) instance).param1).isEqualTo(42);
		});
	}

	@Test // DATACMNS-1200
	void instantiateObjectCtor1ParamIntWithoutValue() {

		doReturn(ObjectCtor1ParamInt.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjectCtor1ParamInt.class))//
				.when(entity).getInstanceCreatorMetadata();

		assertThatThrownBy(() -> this.instance.createInstance(entity, provider)) //
				.hasCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATACMNS-578, DATACMNS-1126
	@SuppressWarnings("unchecked")
	void instantiateObjectCtor7ParamsString5IntsString() {

		doReturn(ObjectCtor7ParamsString5IntsString.class).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(ObjectCtor7ParamsString5IntsString.class))//
				.when(entity).getInstanceCreatorMetadata();

		IntStream.range(0, 2).forEach(i -> {

			when(provider.getParameterValue(any(Parameter.class))).thenReturn("A", 1, 2, 3, 4, 5, "B");

			var instance = this.instance.createInstance(entity, provider);

			assertThat(instance).isInstanceOf(ObjectCtor7ParamsString5IntsString.class);

			var toTest = (ObjectCtor7ParamsString5IntsString) instance;

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

		var typeInDefaultPackage = Class.forName("TypeInDefaultPackage");
		prepareMocks(typeInDefaultPackage);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isFalse();
		assertThat(this.instance.createInstance(entity, provider)).isInstanceOf(typeInDefaultPackage);
	}

	@Test // DATACMNS-1373
	void shouldNotInstantiateClassWithPrivateConstructor() {

		prepareMocks(ClassWithPrivateConstructor.class);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isTrue();
	}

	@Test // GH-2446
	void shouldReuseGeneratedClasses() {

		prepareMocks(ProtectedInnerClass.class);

		this.instance.createInstance(entity, provider);

		var instantiator = new ClassGeneratingEntityInstantiator();
		instantiator.createInstance(entity, provider);

		var first = (Map<TypeInformation<?>, EntityInstantiator>) ReflectionTestUtils
				.getField(this.instance, "entityInstantiators");

		var second = (Map<TypeInformation<?>, EntityInstantiator>) ReflectionTestUtils
				.getField(instantiator, "entityInstantiators");

		assertThat(first.get(null)).isNotNull().isNotInstanceOf(Enum.class);
		assertThat(second.get(null)).isNotNull().isNotInstanceOf(Enum.class);
	}

	@Test // DATACMNS-1422
	void shouldUseReflectionIfFrameworkTypesNotVisible() throws Exception {

		var classLoader = HidingClassLoader.hide(ObjectInstantiator.class);
		classLoader.excludePackage("org.springframework.data.mapping");

		// require type from different package to meet visibility quirks
		var entityType = classLoader.loadClass("org.springframework.data.mapping.Person");

		prepareMocks(entityType);

		assertThat(this.instance.shouldUseReflectionEntityInstantiator(entity)).isTrue();
	}

	@Test // GH-2348
	void entityInstantiatorShouldFailForAbstractClass() {

		assertThatExceptionOfType(MappingInstantiationException.class).isThrownBy(() -> this.instance
				.createInstance(new BasicPersistentEntity<>(TypeInformation.of(AbstractDto.class)), provider));
	}

	private void prepareMocks(Class<?> type) {

		doReturn(type).when(entity).getType();
		doReturn(PreferredConstructorDiscoverer.discover(type))//
				.when(entity).getInstanceCreatorMetadata();
	}

	static class Foo {

		Foo(String foo) {

		}
	}

	static class Outer {

		class Inner {

		}
	}

	static class WithFactoryMethod {

		final Long id;
		final String name;

		private WithFactoryMethod(Long id, String name) {

			this.id = id;
			this.name = name;
		}

		@PersistenceCreator
		public static WithFactoryMethod create(Long id, String name) {
			return new WithFactoryMethod(id, "Hello " + name);
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

	public static abstract class AbstractDto {

	}

}
