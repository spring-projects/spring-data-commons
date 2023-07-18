/*
 * Copyright 2018-2023 the original author or authors.
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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

	private static final SampleMappingContext MAPPING_CONTEXT = new SampleMappingContext();

	@SuppressWarnings("unchecked")
	public static List<Object[]> parameters() {

		List<Object[]> parameters = new ArrayList<>();

		var factory = new ClassGeneratingPropertyAccessorFactory();

		var beanWrapper = (Function<Object, PersistentPropertyAccessor<?>>) BeanWrapper::new;
		var classGenerating = (Function<Object, PersistentPropertyAccessor<?>>) it -> factory
				.getPropertyAccessor(MAPPING_CONTEXT.getRequiredPersistentEntity(it.getClass()), it);

		parameters.add(new Object[] { beanWrapper, "BeanWrapper" });
		parameters.add(new Object[] { classGenerating, "ClassGeneratingPropertyAccessorFactory" });

		return parameters;
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldSetProperty(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new DataClass();
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "id");

		accessor.setProperty(property, "value");

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", "value");
		assertThat(accessor.getBean()).isSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo("value");
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldSetKotlinDataClassProperty(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new DataClassKt("foo");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "id");

		accessor.setProperty(property, "value");

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", "value");
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo("value");
	}

	@ParameterizedTest // DATACMNS-1322, DATACMNS-1391
	@MethodSource("parameters")
	void shouldSetExtendedKotlinDataClassProperty(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new ExtendedDataClassKt(0, "bar");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "id");

		accessor.setProperty(property, 1L);

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", 1L);
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo(1L);
	}

	@ParameterizedTest // DATACMNS-1391
	@MethodSource("parameters")
	void shouldUseKotlinGeneratedCopyMethod(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new UnusedCustomCopy(new Timestamp(100));
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "date");

		accessor.setProperty(property, new Timestamp(200));

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("date", new Timestamp(200));
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo(new Timestamp(200));
	}

	@ParameterizedTest // DATACMNS-1391
	@MethodSource("parameters")
	void kotlinCopyMethodShouldNotSetUnsettableProperty(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new SingleSettableProperty(1.1);
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "version");

		assertThatThrownBy(() -> accessor.setProperty(property, 1)).isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest // DATACMNS-1451
	@MethodSource("parameters")
	void shouldSet17thImmutableNullableKotlinProperty(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new With33Args();
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "17");

		accessor.setProperty(property, "foo");

		var updated = (With33Args) accessor.getBean();
		assertThat(updated.get17()).isEqualTo("foo");
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldWitherProperty(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new ValueClass("foo", "bar");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "id");

		accessor.setProperty(property, "value");

		assertThat(accessor.getBean()).hasFieldOrPropertyWithValue("id", "value");
		assertThat(accessor.getBean()).isNotSameAs(bean);
		assertThat(accessor.getProperty(property)).isEqualTo("value");
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldRejectImmutablePropertyUpdate(Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new ValueClass("foo", "bar");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "immutable");

		assertThatThrownBy(() -> accessor.setProperty(property, "value")).isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest // DATACMNS-1322
	@MethodSource("parameters")
	void shouldRejectImmutableKotlinClassPropertyUpdate(
			Function<Object, PersistentPropertyAccessor<?>> propertyAccessorFunction) {

		var bean = new ValueClassKt("foo");
		PersistentPropertyAccessor accessor = propertyAccessorFunction.apply(bean);
		var property = getProperty(bean, "immutable");

		assertThatThrownBy(() -> accessor.setProperty(property, "value")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test // DATACMNS-1422
	void shouldUseReflectionIfFrameworkTypesNotVisible() throws Exception {

		var classLoader = HidingClassLoader.hide(Assert.class);
		classLoader.excludePackage("org.springframework.data.mapping.model");

		var entityType = classLoader
				.loadClass("org.springframework.data.mapping.model.PersistentPropertyAccessorTests$ClassLoaderTest");

		var factory = new ClassGeneratingPropertyAccessorFactory();
		var entity = MAPPING_CONTEXT.getRequiredPersistentEntity(entityType);

		assertThat(factory.isSupported(entity)).isFalse();
	}

	private static SamplePersistentProperty getProperty(Object bean, String propertyName) {
		return MAPPING_CONTEXT.getRequiredPersistentEntity(bean.getClass()).getRequiredPersistentProperty(propertyName);
	}

	static class DataClass {
		String id;

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

	static class ClassLoaderTest {}

	private static final class ValueClass {
		private final String id;
		private final String immutable;

		public ValueClass(String id, String immutable) {
			this.id = id;
			this.immutable = immutable;
		}

		public String getId() {
			return this.id;
		}

		public String getImmutable() {
			return this.immutable;
		}

		public ValueClass withId(String id) {
			return this.id == id ? this : new ValueClass(id, this.immutable);
		}
	}

	static class UnsettableVersion {

		private final int version = (int) Math.random();
	}

}
