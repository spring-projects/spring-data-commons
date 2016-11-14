/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.ReflectionUtils.DescribedFieldFilter;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * @author Oliver Gierke
 */
public class ReflectionUtilsUnitTests {

	@SuppressWarnings("rawtypes") Constructor constructor;
	Field reference;

	@Before
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

	@Test(expected = IllegalStateException.class)
	public void rejectsNonUniqueField() {
		ReflectionUtils.findField(Sample.class, new ReflectionUtils.AnnotationFieldFilter(Autowired.class));
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

	/**
	 * @see DATACMNS-542
	 */
	@Test
	public void detectsConstructorForCompleteMatch() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, 2, "test")).hasValue(constructor);
	}

	/**
	 * @see DATACMNS-542
	 */
	@Test
	public void detectsConstructorForMatchWithNulls() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, 2, null)).hasValue(constructor);
	}

	/**
	 * @see DATACMNS-542
	 */
	@Test
	public void rejectsConstructorIfNumberOfArgumentsDontMatch() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, 2, "test", "test")).isNotPresent();
	}

	/**
	 * @see DATACMNS-542
	 */
	@Test
	public void rejectsConstructorForNullForPrimitiveArgument() throws Exception {
		assertThat(ReflectionUtils.findConstructor(ConstructorDetection.class, null, "test")).isNotPresent();
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
