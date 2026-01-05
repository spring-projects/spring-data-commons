/*
 * Copyright 2023-present the original author or authors.
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

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError;

import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jmolecules.ddd.types.Association;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;

/**
 * Kotlin-specific unit tests for {@link ClassGeneratingPropertyAccessorFactory} and
 * {@link BeanWrapperPropertyAccessorFactory}
 *
 * @author John Blum
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class KotlinPropertyAccessorFactoryTests {

	private EntityInstantiators instantiators = new EntityInstantiators();
	private SampleMappingContext mappingContext = new SampleMappingContext();

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void shouldGeneratePropertyAccessorForTypeWithValueClass(PersistentPropertyAccessorFactory factory) {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(WithMyValueClass.class);

		Object instance = createInstance(entity, parameter -> "foo");

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);
		var persistentProperty = entity.getRequiredPersistentProperty("id");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(persistentProperty)).isEqualTo("foo");

		if (factory instanceof BeanWrapperPropertyAccessorFactory) {

			// Sigh. Reflection requires a wrapped value while copy accepts the inlined type.
			assertThatExceptionOfType(IllegalArgumentException.class)
					.isThrownBy(() -> propertyAccessor.setProperty(persistentProperty, "bar"));
			return;
		}

		propertyAccessor.setProperty(persistentProperty, "bar");
		assertThat(propertyAccessor.getProperty(persistentProperty)).isEqualTo("bar");
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void shouldGeneratePropertyAccessorForTypeWithNullableValueClass(PersistentPropertyAccessorFactory factory)
			throws ReflectiveOperationException {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(WithNestedMyNullableValueClass.class);

		Object instance = createInstance(entity, parameter -> null);

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);

		var expectedDefaultValue = BeanUtils
				.instantiateClass(MyNullableValueClass.class.getDeclaredConstructor(String.class), "id");
		var barValue = BeanUtils.instantiateClass(MyNullableValueClass.class.getDeclaredConstructor(String.class), "bar");
		var property = entity.getRequiredPersistentProperty("baz");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(property)).isEqualTo(expectedDefaultValue);

		propertyAccessor.setProperty(property, barValue);
		assertThat(propertyAccessor.getProperty(property)).isEqualTo(barValue);
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void shouldGeneratePropertyAccessorForDataClassWithNullableValueClass(PersistentPropertyAccessorFactory factory)
			throws ReflectiveOperationException {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(DataClassWithNullableValueClass.class);

		Object instance = createInstance(entity, parameter -> null);

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);

		var expectedDefaultValue = BeanUtils
				.instantiateClass(MyNullableValueClass.class.getDeclaredConstructor(String.class), "id");
		var barValue = BeanUtils.instantiateClass(MyNullableValueClass.class.getDeclaredConstructor(String.class), "bar");
		var fullyNullable = entity.getRequiredPersistentProperty("fullyNullable");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(fullyNullable)).isEqualTo(expectedDefaultValue);

		if (factory instanceof BeanWrapperPropertyAccessorFactory) {

			// see https://youtrack.jetbrains.com/issue/KT-57357
			assertThatExceptionOfType(KotlinReflectionInternalError.class)
					.isThrownBy(() -> propertyAccessor.setProperty(fullyNullable, barValue))
					.withMessageContaining("This callable does not support a default call");
			return;
		}

		propertyAccessor.setProperty(fullyNullable, barValue);
		assertThat(propertyAccessor.getProperty(fullyNullable)).isEqualTo(barValue);

		var innerNullable = entity.getRequiredPersistentProperty("innerNullable");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(innerNullable)).isEqualTo("id");

		propertyAccessor.setProperty(innerNullable, "bar");
		assertThat(propertyAccessor.getProperty(innerNullable)).isEqualTo("bar");
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void nestedNullablePropertiesShouldBeSetCorrectly(PersistentPropertyAccessorFactory factory) {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(DataClassWithNestedNullableValueClass.class);

		Object instance = createInstance(entity, parameter -> null);

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);
		var nullableNestedNullable = entity.getRequiredPersistentProperty("nullableNestedNullable");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(nullableNestedNullable)).isNull();

		KClass<MyNestedNullableValueClass> nested = JvmClassMappingKt.getKotlinClass(MyNestedNullableValueClass.class);
		KClass<MyNullableValueClass> nullable = JvmClassMappingKt.getKotlinClass(MyNullableValueClass.class);

		MyNullableValueClass inner = nullable.getConstructors().iterator().next().call("new-value");
		MyNestedNullableValueClass outer = nested.getConstructors().iterator().next().call(inner);

		if (factory instanceof BeanWrapperPropertyAccessorFactory) {

			// see https://youtrack.jetbrains.com/issue/KT-57357
			assertThatExceptionOfType(KotlinReflectionInternalError.class)
					.isThrownBy(() -> propertyAccessor.setProperty(nullableNestedNullable, outer))
					.withMessageContaining("This callable does not support a default call");
			return;
		}

		propertyAccessor.setProperty(nullableNestedNullable, outer);
		assertThat(propertyAccessor.getProperty(nullableNestedNullable)).isInstanceOf(MyNestedNullableValueClass.class)
				.hasToString("MyNestedNullableValueClass(id=MyNullableValueClass(id=new-value))");

		var nestedNullable = entity.getRequiredPersistentProperty("nestedNullable");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(nestedNullable)).isNull();

		propertyAccessor.setProperty(nestedNullable, "inner");
		assertThat(propertyAccessor.getProperty(nestedNullable)).isEqualTo("inner");
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void genericInlineClassesShouldWork(PersistentPropertyAccessorFactory factory) {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(WithGenericValue.class);

		KClass<MyGenericValue> genericClass = JvmClassMappingKt.getKotlinClass(MyGenericValue.class);
		MyGenericValue<?> inner = genericClass.getConstructors().iterator().next().call("initial-value");
		MyGenericValue<?> outer = genericClass.getConstructors().iterator().next().call(inner);

		MyGenericValue<?> newInner = genericClass.getConstructors().iterator().next().call("new-value");
		MyGenericValue<?> newOuter = genericClass.getConstructors().iterator().next().call(newInner);

		Object instance = createInstance(entity, parameter -> parameter.getName().equals("recursive") ? outer : "aaa");

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);
		var string = entity.getRequiredPersistentProperty("string");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(string)).isEqualTo("aaa");

		if (factory instanceof BeanWrapperPropertyAccessorFactory) {

			// see https://youtrack.jetbrains.com/issue/KT-57357
			assertThatExceptionOfType(KotlinReflectionInternalError.class)
					.isThrownBy(() -> propertyAccessor.setProperty(string, "string"))
					.withMessageContaining("This callable does not support a default call");
			return;
		}

		propertyAccessor.setProperty(string, "string");
		assertThat(propertyAccessor.getProperty(string)).isEqualTo("string");

		var charseq = entity.getRequiredPersistentProperty("charseq");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(charseq)).isEqualTo("aaa");
		propertyAccessor.setProperty(charseq, "charseq");
		assertThat(propertyAccessor.getProperty(charseq)).isEqualTo("charseq");

		var recursive = entity.getRequiredPersistentProperty("recursive");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(recursive)).isEqualTo(outer);
		propertyAccessor.setProperty(recursive, newOuter);

		// huh? why is that?
		assertThat(propertyAccessor.getProperty(recursive)).isEqualTo(newInner);
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void genericNullableInlineClassesShouldWork(PersistentPropertyAccessorFactory factory) {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(WithGenericNullableValue.class);

		KClass<MyGenericValue> genericClass = JvmClassMappingKt.getKotlinClass(MyGenericValue.class);
		MyGenericValue<?> inner = genericClass.getConstructors().iterator().next().call("initial-value");
		MyGenericValue<?> outer = genericClass.getConstructors().iterator().next().call(inner);

		MyGenericValue<?> newInner = genericClass.getConstructors().iterator().next().call("new-value");
		MyGenericValue<?> newOuter = genericClass.getConstructors().iterator().next().call(newInner);

		Object instance = createInstance(entity, parameter -> outer);

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);
		var recursive = entity.getRequiredPersistentProperty("recursive");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(recursive)).isEqualTo(outer);
		propertyAccessor.setProperty(recursive, newOuter);
		assertThat(propertyAccessor.getProperty(recursive)).isEqualTo(newOuter);
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-1947
	void shouldUnwrapValueTypeIfNecessary(PersistentPropertyAccessorFactory factory) throws Exception {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(MyEntity.class);

		Constructor<?> declaredConstructor = MyValueClass.class.getDeclaredConstructor(String.class);

		Object instance = createInstance(entity, parameter -> {

			String name = parameter.getName();

			return switch (name) {
				case "id" -> 1L;
				case "name" -> "foo";
				default -> "bar";
			};

		});

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);
		var createdBy = entity.getRequiredPersistentProperty("createdBy");

		propertyAccessor.setProperty(createdBy, BeanUtils.instantiateClass(declaredConstructor, "baz"));
	}

	@MethodSource("factories")
	@ParameterizedTest // GH-3131
	void shouldApplyCopyForSinglePropertyClass(PersistentPropertyAccessorFactory factory) {

		BasicPersistentEntity<Object, SamplePersistentProperty> entity = mappingContext
				.getRequiredPersistentEntity(DataClassWithAssociation.class);

		var foo = Association.forAggregate(new DataClassAggregate(new DataClassId("foo")));
		var bar = Association.forAggregate(new DataClassAggregate(new DataClassId("bar")));
		Object instance = createInstance(entity, parameter -> foo);

		var propertyAccessor = factory.getPropertyAccessor(entity, instance);
		var persistentProperty = entity.getRequiredPersistentProperty("assoc");

		assertThat(propertyAccessor).isNotNull();
		assertThat(propertyAccessor.getProperty(persistentProperty)).isEqualTo(foo);

		propertyAccessor.setProperty(persistentProperty, bar);
		assertThat(propertyAccessor.getProperty(persistentProperty)).isEqualTo(bar);
	}

	private Object createInstance(BasicPersistentEntity<?, SamplePersistentProperty> entity,
			Function<Parameter<?, ?>, Object> parameterProvider) {
		return instantiators.getInstantiatorFor(entity).createInstance(entity,
				new ParameterValueProvider<SamplePersistentProperty>() {
					@Override
					public <T> T getParameterValue(Parameter<T, SamplePersistentProperty> parameter) {
						return (T) parameterProvider.apply(parameter);
					}
				});
	}

	static Stream<PersistentPropertyAccessorFactory> factories() {
		return Stream.of(new ClassGeneratingPropertyAccessorFactory(), BeanWrapperPropertyAccessorFactory.INSTANCE);
	}

}
