/*
 * Copyright 2012 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

	Field reference;

	@Before
	public void setUp() throws Exception {
		this.reference = Sample.class.getField("field");
	}

	@Test
	public void findsFieldByFilter() {

		Field field = ReflectionUtils.findField(Sample.class, (FieldFilter) new FieldNameFieldFilter("field"));
		assertThat(field, is(reference));
	}

	@Test
	public void returnsNullIfNoFieldFound() {

		Field field = ReflectionUtils.findField(Sample.class, (FieldFilter) new FieldNameFieldFilter("foo"));
		assertThat(field, is(nullValue()));
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsNonUniqueField() {
		ReflectionUtils.findField(Sample.class, new ReflectionUtils.AnnotationFieldFilter(Autowired.class));
	}

	@Test
	public void findsUniqueField() {

		Field field = ReflectionUtils.findField(Sample.class, new FieldNameFieldFilter("field"), false);
		assertThat(field, is(reference));
	}

	@Test
	public void findsFieldInSuperclass() {

		class Subclass extends Sample {

		}

		Field field = ReflectionUtils.findField(Subclass.class, new FieldNameFieldFilter("field"));
		assertThat(field, is(reference));
	}

	@Test
	public void setsNonPublicField() {

		Sample sample = new Sample();
		Field field = ReflectionUtils.findField(Sample.class, new FieldNameFieldFilter("first"));
		ReflectionUtils.setField(field, sample, "foo");
		assertThat(sample.first, is("foo"));
	}

	static class Sample {

		public String field;

		@Autowired
		String first, second;
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
}
