/*
 * Copyright 2018-2021 the original author or authors.
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.data.mapping.PersistentEntity;
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
@RunWith(Parameterized.class)
public class ParameterizedKotlinInstantiatorUnitTests {

	private final String valueToSet = "THE VALUE";
	private final PersistentEntity<Object, SamplePersistentProperty> entity;
	private final int propertyCount;
	private final int propertyUnderTestIndex;
	private final String propertyUnderTestName;
	private final EntityInstantiator entityInstantiator;

	public ParameterizedKotlinInstantiatorUnitTests(PersistentEntity<Object, SamplePersistentProperty> entity,
			int propertyCount, int propertyUnderTestIndex, String propertyUnderTestName,
			EntityInstantiator entityInstantiator, String label) {
		this.entity = entity;
		this.propertyCount = propertyCount;
		this.propertyUnderTestIndex = propertyUnderTestIndex;
		this.propertyUnderTestName = propertyUnderTestName;
		this.entityInstantiator = entityInstantiator;
	}

	@Parameters(name = "{5}")
	public static List<Object[]> parameters() {

		SampleMappingContext context = new SampleMappingContext();

		KotlinClassGeneratingEntityInstantiator generatingInstantiator = new KotlinClassGeneratingEntityInstantiator();
		ReflectionEntityInstantiator reflectionInstantiator = ReflectionEntityInstantiator.INSTANCE;

		List<Object[]> fixtures = new ArrayList<>();
		fixtures.addAll(createFixture(context, With32Args.class, 32, generatingInstantiator));
		fixtures.addAll(createFixture(context, With32Args.class, 32, reflectionInstantiator));
		fixtures.addAll(createFixture(context, With33Args.class, 33, generatingInstantiator));
		fixtures.addAll(createFixture(context, With33Args.class, 33, reflectionInstantiator));

		return fixtures;
	}

	private static List<Object[]> createFixture(SampleMappingContext context, Class<?> entityType, int propertyCount,
			EntityInstantiator entityInstantiator) {

		BasicPersistentEntity<Object, SamplePersistentProperty> persistentEntity = context.getPersistentEntity(entityType);

		return IntStream.range(0, propertyCount).mapToObj(i -> {

			return new Object[] { persistentEntity, propertyCount, i, Integer.toString(i), entityInstantiator,
					String.format("Property %d for %s using %s", i, entityType.getSimpleName(),
							entityInstantiator.getClass().getSimpleName()) };
		}).collect(Collectors.toList());
	}

	@Test // DATACMNS-1402
	public void shouldCreateInstanceWithSinglePropertySet() {

		Object instance = entityInstantiator.createInstance(entity, new SingleParameterValueProvider());

		for (int i = 0; i < propertyCount; i++) {

			Object value = ReflectionTestUtils.getField(instance, Integer.toString(i));

			if (propertyUnderTestIndex == i) {
				assertThat(value).describedAs("Property " + i + " of " + entity).isEqualTo(valueToSet);
			} else {
				assertThat(value).describedAs("Property " + i + " of " + entity).isEqualTo("");
			}
		}
	}

	@Test // DATACMNS-1402
	public void shouldCreateInstanceWithAllExceptSinglePropertySet() {

		Object instance = entityInstantiator.createInstance(entity, new AllButParameterValueProvider());

		for (int i = 0; i < propertyCount; i++) {

			Object value = ReflectionTestUtils.getField(instance, Integer.toString(i));

			if (propertyUnderTestIndex == i) {
				assertThat(value).describedAs("Property " + i + " of " + entity).isEqualTo("");
			} else {
				assertThat(value).describedAs("Property " + i + " of " + entity).isEqualTo(Integer.toString(i));
			}
		}
	}

	/**
	 * Return the value to set for the property to test.
	 */
	class SingleParameterValueProvider implements ParameterValueProvider<SamplePersistentProperty> {

		@Override
		public <T> T getParameterValue(Parameter<T, SamplePersistentProperty> parameter) {

			if (parameter.getName().equals(propertyUnderTestName)) {
				return (T) valueToSet;
			}
			return null;
		}
	}

	/**
	 * Return the property name as value for all properties except the one to test.
	 */
	class AllButParameterValueProvider implements ParameterValueProvider<SamplePersistentProperty> {

		@Override
		public <T> T getParameterValue(Parameter<T, SamplePersistentProperty> parameter) {

			if (!parameter.getName().equals(propertyUnderTestName)) {
				return (T) parameter.getName();
			}
			return null;
		}
	}
}
