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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import kotlin.Unit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.model.TypeCreatingSyntheticClassKt;
import org.springframework.data.repository.sample.User;
import org.springframework.data.util.ReflectionUtils.DescribedFieldFilter;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Unit tests for {@link ReflectionUtils}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ReflectionUtilsUnitTests {

	@SuppressWarnings("rawtypes") Constructor constructor;
	Field reference;

	@BeforeEach
	public void setUp() throws Exception {
		this.reference = Sample.class.getField("field");
		this.constructor = ConstructorDetection.class.getConstructor(int.class, String.class);
	}

	@Test
	public void findsFieldByFilter() {

		Field field = ReflectionUtils.findField(Sample.class, (FieldFilter) new FieldNameFieldFilter("field"));
		assertThat(field).isEqualTo(reference);
	}

	@Test
	public void returnsNullIfNoFieldFound() {

		Field field = ReflectionUtils.findField(Sample.class, (FieldFilter) new FieldNameFieldFilter("foo"));
		assertThat(field).isNull();
	}

	@Test
	public void rejectsNonUniqueField() {
		assertThatIllegalStateException().isThrownBy(
				() -> ReflectionUtils.findField(Sample.class, new ReflectionUtils.AnnotationFieldFilter(Autowired.class)));
	}

	@Test
	public void findsUniqueField() {

		Field field = ReflectionUtils.findField(Sample.class, new FieldNameFieldFilter("field"), false);
		assertThat(field).isEqualTo(reference);
	}

	@Test
	public void findsFieldInSuperclass() {

		class Subclass extends Sample {

		}

		Field field = ReflectionUtils.findField(Subclass.class, new FieldNameFieldFilter("field"));
		assertThat(field).isEqualTo(reference);
	}

	@Test
	public void setsNonPublicField() {

		Sample sample = new Sample();
		Field field = ReflectionUtils.findField(Sample.class, new FieldNameFieldFilter("first"));
		ReflectionUtils.setField(field, sample, "foo");
		assertThat(sample.first).isEqualTo("foo");
	}

	@Test // DATACMNS-542
	public void detectsConstructorForCompleteMatch() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, 2, "test")).hasValue(constructor);
	}

	@Test // DATACMNS-542
	public void detectsConstructorForMatchWithNulls() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, 2, null)).hasValue(constructor);
	}

	@Test // DATACMNS-542
	public void rejectsConstructorIfNumberOfArgumentsDontMatch() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, 2, "test", "test")).isNotPresent();
	}

	@Test // DATACMNS-542
	public void rejectsConstructorForNullForPrimitiveArgument() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, null, "test")).isNotPresent();
	}

	@Test // DATACMNS-1154
	public void discoversNoReturnType() throws Exception {

		MethodParameter parameter = new MethodParameter(DummyInterface.class.getDeclaredMethod("noReturnValue"), -1);
		assertThat(ReflectionUtils.isNullable(parameter)).isTrue();
	}

	@Test // DATACMNS-1154
	public void discoversNullableReturnType() throws Exception {

		MethodParameter parameter = new MethodParameter(DummyInterface.class.getDeclaredMethod("nullableReturnValue"), -1);
		assertThat(ReflectionUtils.isNullable(parameter)).isTrue();
	}

	@Test // DATACMNS-1154
	public void discoversNonNullableReturnType() throws Exception {

		MethodParameter parameter = new MethodParameter(DummyInterface.class.getDeclaredMethod("mandatoryReturnValue"), -1);
		assertThat(ReflectionUtils.isNullable(parameter)).isFalse();
	}

	@Test // DATACMNS-1154
	public void discoversNullableParameter() throws Exception {

		MethodParameter parameter = new MethodParameter(
				DummyInterface.class.getDeclaredMethod("nullableParameter", User.class), 0);
		assertThat(ReflectionUtils.isNullable(parameter)).isTrue();
	}

	@Test // DATACMNS-1154
	public void discoversNonNullablePrimitiveParameter() throws Exception {

		MethodParameter parameter = new MethodParameter(DummyInterface.class.getDeclaredMethod("primitive", int.class), 0);
		assertThat(ReflectionUtils.isNullable(parameter)).isFalse();
	}

	@Test // DATACMNS-1171
	public void discoversKotlinClass() {

		assertThat(ReflectionUtils.isKotlinClass(TypeCreatingSyntheticClass.class)).isTrue();
		assertThat(ReflectionUtils.isSupportedKotlinClass(TypeCreatingSyntheticClass.class)).isTrue();
	}

	@Test // DATACMNS-1171
	public void discoversUnsupportedKotlinClass() {

		assertThat(ReflectionUtils.isKotlinClass(TypeCreatingSyntheticClassKt.class)).isTrue();
		assertThat(ReflectionUtils.isSupportedKotlinClass(TypeCreatingSyntheticClassKt.class)).isFalse();
	}

	@Test // DATACMNS-1779
	public void shouldReportIsVoid() {

		assertThat(ReflectionUtils.isVoid(Void.class)).isTrue();
		assertThat(ReflectionUtils.isVoid(Void.TYPE)).isTrue();
		assertThat(ReflectionUtils.isVoid(Unit.class)).isTrue();
		assertThat(ReflectionUtils.isVoid(String.class)).isFalse();
	}

	static class Sample {

		public String field;

		@Autowired String first, second;
	}

	static class FieldNameFieldFilter implements DescribedFieldFilter {

		private final String name;

		public FieldNameFieldFilter(String name) {
			this.name = name;
		}

		public boolean matches(Field field) {
			return field.getName().equals(name);
		}

		public String getDescription() {
			return String.format("Filter for fields named %s", name);
		}
	}

	static class ConstructorDetection {
		public ConstructorDetection(int i, String string) {}
	}
}
