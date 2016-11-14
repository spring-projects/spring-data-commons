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

import static org.assertj.core.api.Assertions.*;
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
	public void setUp() {
		oneLeg = new DefaultPersistentPropertyPath<>(Arrays.asList(first));
		twoLegs = new DefaultPersistentPropertyPath<>(Arrays.asList(first, second));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullProperties() {
		new DefaultPersistentPropertyPath<>(null);
	}

	@Test
	public void usesPropertyNameForSimpleDotPath() {

		when(first.getName()).thenReturn("foo");
		when(second.getName()).thenReturn("bar");

		assertThat(twoLegs.toDotPath()).isEqualTo("foo.bar");
	}

	@Test
	public void usesConverterToCreatePropertyPath() {

		when(converter.convert(any())).thenReturn("foo");

		assertThat(twoLegs.toDotPath(converter)).isEqualTo("foo.foo");
	}

	@Test
	public void returnsCorrectLeafProperty() {

		assertThat(twoLegs.getLeafProperty()).isEqualTo(second);
		assertThat(oneLeg.getLeafProperty()).isEqualTo(first);
	}

	@Test
	public void returnsCorrectBaseProperty() {

		assertThat(twoLegs.getBaseProperty()).isEqualTo(first);
		assertThat(oneLeg.getBaseProperty()).isEqualTo(first);
	}

	@Test
	public void detectsBasePathCorrectly() {

		assertThat(oneLeg.isBasePathOf(twoLegs)).isTrue();
		assertThat(twoLegs.isBasePathOf(oneLeg)).isFalse();
	}

	@Test
	public void calculatesExtensionCorrectly() {

		PersistentPropertyPath<T> extension = twoLegs.getExtensionForBaseOf(oneLeg);

		assertThat(extension).isEqualTo(new DefaultPersistentPropertyPath<>(Arrays.asList(second)));
	}

	@Test
	public void returnsTheCorrectParentPath() {
		assertThat(twoLegs.getParentPath()).isEqualTo(oneLeg);
	}

	@Test
	public void returnsItselfAsParentPathIfSizeOne() {
		assertThat(oneLeg.getParentPath()).isEqualTo(oneLeg);
	}

	@Test
	public void pathReturnsCorrectSize() {
		assertThat(oneLeg.getLength()).isEqualTo(1);
		assertThat(twoLegs.getLength()).isEqualTo(2);
	}

	/**
	 * @see DATACMNS-444
	 */
	@Test
	public void skipsMappedPropertyNameIfConverterReturnsNull() {
		assertThat(twoLegs.toDotPath(source -> null)).isNull();
	}

	/**
	 * @see DATACMNS-444
	 */
	@Test
	public void skipsMappedPropertyNameIfConverterReturnsEmptyStrings() {
		assertThat(twoLegs.toDotPath(source -> "")).isNull();
	}
}
