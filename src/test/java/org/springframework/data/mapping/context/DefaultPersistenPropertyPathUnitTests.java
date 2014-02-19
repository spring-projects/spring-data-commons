/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Unit tests for {@link DefaultPersistentPropertyPath}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPersistenPropertyPathUnitTests<T extends PersistentProperty<T>> {

	@Mock T first, second;

	@Mock Converter<T, String> converter;

	PersistentPropertyPath<T> oneLeg;
	PersistentPropertyPath<T> twoLegs;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		oneLeg = new DefaultPersistentPropertyPath<T>(Arrays.asList(first));
		twoLegs = new DefaultPersistentPropertyPath<T>(Arrays.asList(first, second));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProperties() {
		new DefaultPersistentPropertyPath<T>(null);
	}

	@Test
	public void usesPropertyNameForSimpleDotPath() {

		when(first.getName()).thenReturn("foo");
		when(second.getName()).thenReturn("bar");

		assertThat(twoLegs.toDotPath(), is("foo.bar"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void usesConverterToCreatePropertyPath() {

		when(converter.convert((T) any())).thenReturn("foo");

		assertThat(twoLegs.toDotPath(converter), is("foo.foo"));
	}

	@Test
	public void returnsCorrectLeafProperty() {

		assertThat(twoLegs.getLeafProperty(), is(second));
		assertThat(oneLeg.getLeafProperty(), is(first));
	}

	@Test
	public void returnsCorrectBaseProperty() {

		assertThat(twoLegs.getBaseProperty(), is(first));
		assertThat(oneLeg.getBaseProperty(), is(first));
	}

	@Test
	public void detectsBasePathCorrectly() {

		assertThat(oneLeg.isBasePathOf(twoLegs), is(true));
		assertThat(twoLegs.isBasePathOf(oneLeg), is(false));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void calculatesExtensionCorrectly() {

		PersistentPropertyPath<T> extension = twoLegs.getExtensionForBaseOf(oneLeg);
		assertThat(extension, is((PersistentPropertyPath<T>) new DefaultPersistentPropertyPath<T>(Arrays.asList(second))));
	}

	@Test
	public void returnsTheCorrectParentPath() {
		assertThat(twoLegs.getParentPath(), is(oneLeg));
	}

	@Test
	public void returnsItselfAsParentPathIfSizeOne() {
		assertThat(oneLeg.getParentPath(), is(oneLeg));
	}

	@Test
	public void pathReturnsCorrectSize() {
		assertThat(oneLeg.getLength(), is(1));
		assertThat(twoLegs.getLength(), is(2));
	}

	/**
	 * @see DATACMNS-444
	 */
	@Test
	public void skipsMappedPropertyNameIfConverterReturnsNull() {

		String result = twoLegs.toDotPath(new Converter<T, String>() {

			@Override
			public String convert(T source) {
				return null;
			}
		});

		assertThat(result, is(nullValue()));
	}

	/**
	 * @see DATACMNS-444
	 */
	@Test
	public void skipsMappedPropertyNameIfConverterReturnsEmptyStrings() {

		String result = twoLegs.toDotPath(new Converter<T, String>() {

			@Override
			public String convert(T source) {
				return "";
			}
		});

		assertThat(result, is(nullValue()));
	}

}
