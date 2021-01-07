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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.mapping.model.ReflectionEntityInstantiator.*;
import static org.springframework.data.util.ClassTypeInformation.from;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ReflectionEntityInstantiatorUnitTests.Outer.Inner;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link ReflectionEntityInstantiator}.
 *
 * @author Oliver Gierke
 * @author Johannes Mockenhaupt
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ReflectionEntityInstantiatorUnitTests<P extends PersistentProperty<P>> {

	@Mock PersistentEntity<?, P> entity;
	@Mock ParameterValueProvider<P> provider;

	@Test
	void instantiatesSimpleObjectCorrectly() {

		doReturn(Object.class).when(entity).getType();
		INSTANCE.createInstance(entity, provider);
	}

	@Test
	void instantiatesArrayCorrectly() {

		doReturn(String[][].class).when(entity).getType();
		INSTANCE.createInstance(entity, provider);
	}

	@Test // DATACMNS-1126
	void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {

		PreferredConstructor<Foo, P> constructor = PreferredConstructorDiscoverer.discover(Foo.class);

		doReturn(constructor).when(entity).getPersistenceConstructor();

		Object instance = INSTANCE.createInstance(entity, provider);

		assertThat(instance).isInstanceOf(Foo.class);
		assertThat(constructor)
				.satisfies(it -> verify(provider, times(1)).getParameterValue(it.getParameters().iterator().next()));
	}

	@Test // DATACMNS-300
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void throwsExceptionOnBeanInstantiationException() {

		doReturn(PersistentEntity.class).when(entity).getType();

		assertThatExceptionOfType(MappingInstantiationException.class)
				.isThrownBy(() -> INSTANCE.createInstance(entity, provider));
	}

	@Test // DATACMNS-134
	void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<>(from(Inner.class));
		assertThat(entity.getPersistenceConstructor()).satisfies(it -> {

			Parameter<Object, P> parameter = it.getParameters().iterator().next();

			Object outer = new Outer();

			when(provider.getParameterValue(parameter)).thenReturn(outer);
			Inner instance = INSTANCE.createInstance(entity, provider);

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

	@Test // DATACMNS-283
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void capturesContextOnInstantiationException() throws Exception {

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<>(from(Sample.class));

		doReturn("FOO").when(provider).getParameterValue(any(Parameter.class));

		Constructor constructor = Sample.class.getConstructor(Long.class, String.class);
		List<Object> parameters = Arrays.asList("FOO", "FOO");

		try {

			INSTANCE.createInstance(entity, provider);
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
