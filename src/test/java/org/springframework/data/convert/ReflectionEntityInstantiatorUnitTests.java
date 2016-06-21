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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.convert.ReflectionEntityInstantiator.*;
import static org.springframework.data.util.ClassTypeInformation.*;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

/**
 * Unit tests for {@link ReflectionEntityInstantiator}.
 * 
 * @author Oliver Gierke
 * @author Johannes Mockenhaupt
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflectionEntityInstantiatorUnitTests<P extends PersistentProperty<P>> {

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
		INSTANCE.createInstance(entity, provider);
	}

	@Test
	public void instantiatesArrayCorrectly() {

		doReturn(String[][].class).when(entity).getType();
		INSTANCE.createInstance(entity, provider);
	}

	@Test
	public void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {

		Optional<? extends PreferredConstructor<Foo, P>> constructor = new PreferredConstructorDiscoverer<Foo, P>(Foo.class)
				.getConstructor();

		doReturn(constructor).when(entity).getPersistenceConstructor();
		doReturn(Optional.empty()).when(provider).getParameterValue(any());

		Object instance = INSTANCE.createInstance(entity, provider);

		assertThat(instance).isInstanceOf(Foo.class);
		assertThat(constructor).hasValueSatisfying(it -> {
			verify(provider, times(1)).getParameterValue(it.getParameters().iterator().next());
		});
	}

	/**
	 * @see DATACMNS-300
	 */
	@Test(expected = MappingInstantiationException.class)
	public void throwsExceptionOnBeanInstantiationException() {

		doReturn(Optional.empty()).when(entity).getPersistenceConstructor();
		doReturn(PersistentEntity.class).when(entity).getType();

		INSTANCE.createInstance(entity, provider);
	}

	/**
	 * @see DATACMNS-134
	 */
	@Test
	public void createsInnerClassInstanceCorrectly() {

		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<Inner, P>(from(Inner.class));
		assertThat(entity.getPersistenceConstructor()).hasValueSatisfying(it -> {

			Parameter<Object, P> parameter = it.getParameters().iterator().next();

			Object outer = new Outer();

			when(provider.getParameterValue(parameter)).thenReturn(Optional.of(outer));
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

	/**
	 * @see DATACMNS-283
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void capturesContextOnInstantiationException() throws Exception {

		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<Sample, P>(from(Sample.class));

		doReturn(Optional.of("FOO")).when(provider).getParameterValue(any(Parameter.class));

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
