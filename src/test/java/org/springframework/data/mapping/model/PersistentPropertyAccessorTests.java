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

import lombok.Data;
import lombok.Value;
import lombok.With;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.util.Assert;

/**
 * Unit tests for {@link PersistentPropertyAccessor} through {@link BeanWrapper} and
 * {@link ClassGeneratingPropertyAccessorFactory}.
 *
 * @author Mark Paluch
 */
public class PersistentPropertyAccessorTests {

	private final static SampleMappingContext MAPPING_CONTEXT = new SampleMappingContext();

	@SuppressWarnings("unchecked")
	public static List<Object[]> parameters() {

		List<Object[]> parameters = new ArrayList<>();

		ClassGeneratingPropertyAccessorFactory factory = new ClassGeneratingPropertyAccessorFactory();

		Function<Object, PersistentPropertyAccessor<?>> beanWrapper = BeanWrapper::new;
		Function<Object, PersistentPropertyAccessor<?>> classGenerating = it -> factory
				.getPropertyAccessor(MAPPING_CONTEXT.getRequiredPersistentEntity(it.getClass()), it);

		parameters.add(new Object[] { beanWrapper, "BeanWrapper" });
		parameters.add(new Object[] { classGenerating, "ClassGeneratingPropertyAccessorFactory" });

		return parameters;
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldSetProperty(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		DataClass bean = new DataClass();
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "id");

		accessor.setProperty(property, "value");

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", "value");
		assertThat(accessor.getBean()).isSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo("value");
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldSetKotlinDataClassProperty(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		DataClassKt bean = new DataClassKt("foo");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "id");

		accessor.setProperty(property, "value");

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", "value");
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo("value");
	}

	@ParameterizedTest // DATACMNS-1322, DATACMNS-1391
	@MethodSource("parameters")
	void shouldSetExtendedKotlinDataClassProperty(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		ExtendedDataClassKt bean = new ExtendedDataClassKt(0, "bar");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "id");

		accessor.setProperty(property, 1L);

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", 1L);
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo(1L);
	}

	@ParameterizedTest // DATACMNS-1391
	@MethodSource("parameters")
	void shouldUseKotlinGeneratedCopyMethod(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		UnusedCustomCopy bean = new UnusedCustomCopy(new Timestamp(100));
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "date");

		accessor.setProperty(property, new Timestamp(200));

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("date", new Timestamp(200));
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo(new Timestamp(200));
	}

	@ParameterizedTest // DATACMNS-1391
	@MethodSource("parameters")
	void kotlinCopyMethodShouldNotSetUnsettableProperty(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		SingleSettableProperty bean = new SingleSettableProperty(UUID.randomUUID());
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "version");

		assertThatThrownBy(() -> accessor.setProperty(property, 1)).isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest // DATACMNS-1451
	@MethodSource("parameters")
	void shouldSet17thImmutableNullableKotlinProperty(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		With33Args bean = new With33Args();
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "17");

		accessor.setProperty(property, "foo");

		With33Args updated = (With33Args) accessor.getBean();
		assertThat(updated.get17()).isEqualTo("foo");
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldWitherProperty(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		ValueClass bean = new ValueClass("foo", "bar");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "id");

		accessor.setProperty(property, "value");

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", "value");
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo("value");
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldRejectImmutablePropertyUpdate(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		ValueClass bean = new ValueClass("foo", "bar");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "immutable");

		assertThatThrownBy(() -> accessor.setProperty(property, "value")).isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldRejectImmutableKotlinClassPropertyUpdate(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		ValueClassKt bean = new ValueClassKt("foo");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		SamplePersistentProperty property = getProperty(bean, "immutable");

		assertThatThrownBy(() -> accessor.setProperty(property, "value")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test // DATACMNS-1422
	void shouldUseReflectionIfFrameworkTypesNotVisible() throws Exception {

		HidingClassLoader classLoader = HidingClassLoader.hide(Assert.class);
		classLoader.excludePackage("org.springframework.data.mapping.model");

		Class<?> entityType = classLoader
				.loadClass("org.springframework.data.mapping.model.PersistentPropertyAccessorTests$ClassLoaderTest");

		ClassGeneratingPropertyAccessorFactory factory = new ClassGeneratingPropertyAccessorFactory();
		BasicPersistentEntity<Object, SamplePersistentProperty> entity = MAPPING_CONTEXT
				.getRequiredPersistentEntity(entityType);

		assertThat(factory.isSupported(entity)).isFalse();
	}

	private static SamplePersistentProperty getProperty(Object bean, String propertyName) {
		return MAPPING_CONTEXT.getRequiredPersistentEntity(bean.getClass()).getRequiredPersistentProperty(propertyName);
	}

	@Data
	static class DataClass {
		String id;
	}

	static class ClassLoaderTest {}

	@Value

	private static class ValueClass {
		@With String id;
		String immutable;
	}

}
