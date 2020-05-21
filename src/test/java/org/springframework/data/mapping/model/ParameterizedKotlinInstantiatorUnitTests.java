/*
 * Copyright 2018-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test to verify correct object instantiation using Kotlin defaulting via
 * {@link KotlinClassGeneratingEntityInstantiator}.
 *
 * @author Mark Paluch
 */
class ParameterizedKotlinInstantiatorUnitTests {

	private static final String VALUE_TO_SET = "THE VALUE";

	@ParameterizedTest // DATACMNS-1402
	@MethodSource("fixtures")
	void shouldCreateInstanceWithSinglePropertySet(Fixture fixture) {
		Object instance = fixture.createInstance(SingleParameterValueProvider::new);
		for (int i = 0; i < fixture.propertyCount; i++) {
			Object value = ReflectionTestUtils.getField(instance, Integer.toString(i));
			if (fixture.index == i) {
				assertThat(value).describedAs("Property " + i + " of " + fixture.entity).isEqualTo(VALUE_TO_SET);
			}
			else {
				assertThat(value).describedAs("Property " + i + " of " + fixture.entity).isEqualTo("");
			}
		}
	}

	@ParameterizedTest // DATACMNS-1402
	@MethodSource("fixtures")
	void shouldCreateInstanceWithAllExceptSinglePropertySet(Fixture fixture) {
		Object instance = fixture.createInstance(AllButParameterValueProvider::new);
		for (int i = 0; i < fixture.propertyCount; i++) {
			Object value = ReflectionTestUtils.getField(instance, Integer.toString(i));
			if (fixture.index == i) {
				assertThat(value).describedAs("Property " + i + " of " + fixture.entity).isEqualTo("");
			}
			else {
				assertThat(value).describedAs("Property " + i + " of " + fixture.entity).isEqualTo(Integer.toString(i));
			}
		}
	}

	static List<Fixture> fixtures() {
		SampleMappingContext context = new SampleMappingContext();

		KotlinClassGeneratingEntityInstantiator generatingInstantiator = new KotlinClassGeneratingEntityInstantiator();
		ReflectionEntityInstantiator reflectionInstantiator = ReflectionEntityInstantiator.INSTANCE;
		List<Fixture> fixtures = new ArrayList<>();
		fixtures.addAll(createFixture(context, With32Args.class, 32, generatingInstantiator));
		fixtures.addAll(createFixture(context, With32Args.class, 32, reflectionInstantiator));
		fixtures.addAll(createFixture(context, With33Args.class, 33, generatingInstantiator));
		fixtures.addAll(createFixture(context, With33Args.class, 33, reflectionInstantiator));

		return fixtures;
	}

	private static List<Fixture> createFixture(SampleMappingContext context, Class<?> entityType, int propertyCount,
			EntityInstantiator entityInstantiator) {
		BasicPersistentEntity<Object, SamplePersistentProperty> persistentEntity = context
				.getPersistentEntity(entityType);
		return IntStream.range(0, propertyCount)
				.mapToObj(index -> new Fixture(persistentEntity, propertyCount, entityInstantiator, index, entityType))
				.collect(Collectors.toList());
	}

	static class Fixture {

		private final BasicPersistentEntity<Object, SamplePersistentProperty> entity;

		private final int propertyCount;

		private final EntityInstantiator entityInstantiator;

		private final int index;

		private final Class<?> entityType;

		Fixture(BasicPersistentEntity<Object, SamplePersistentProperty> entity, int propertyCount,
				EntityInstantiator entityInstantiator, int index, Class<?> entityType) {
			this.entity = entity;
			this.propertyCount = propertyCount;
			this.entityInstantiator = entityInstantiator;
			this.index = index;
			this.entityType = entityType;
		}

		public Object createInstance(
				Function<Fixture, ParameterValueProvider<SamplePersistentProperty>> providerFactory) {
			return this.entityInstantiator.createInstance(this.entity, providerFactory.apply(this));
		}

		@Override
		public String toString() {
			return String.format("Property %d for %s using %s", this.index, this.entityType.getSimpleName(),
					this.entityInstantiator.getClass().getSimpleName());
		}

	}

	/**
	 * Return the value to set for the property to test.
	 */
	class SingleParameterValueProvider implements ParameterValueProvider<SamplePersistentProperty> {

		private final Fixture fixture;

		SingleParameterValueProvider(Fixture fixture) {
			this.fixture = fixture;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getParameterValue(Parameter<T, SamplePersistentProperty> parameter) {
			if (parameter.getName().equals(String.valueOf(this.fixture.index))) {
				return (T) VALUE_TO_SET;
			}
			return null;
		}
	}

	/**
	 * Return the property name as value for all properties except the one to test.
	 */
	class AllButParameterValueProvider implements ParameterValueProvider<SamplePersistentProperty> {

		private final Fixture fixture;

		AllButParameterValueProvider(Fixture fixture) {
			this.fixture = fixture;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getParameterValue(Parameter<T, SamplePersistentProperty> parameter) {
			if (!parameter.getName().equals(String.valueOf(this.fixture.index))) {
				return (T) parameter.getName();
			}
			return null;
		}
	}
}
