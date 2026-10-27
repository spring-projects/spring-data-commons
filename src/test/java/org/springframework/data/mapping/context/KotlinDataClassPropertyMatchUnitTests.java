/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.context.AbstractMappingContext.PersistentPropertyFilter.KotlinDataClassPropertyMatch;
import org.springframework.data.mapping.model.DataClassWithLazy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.util.KotlinReflectionUtils;

/**
 * Unit tests for {@link KotlinDataClassPropertyMatch}.
 *
 * @author Greg Taube
 */
class KotlinDataClassPropertyMatchUnitTests {

	private final KotlinDataClassPropertyMatch matcher = new KotlinDataClassPropertyMatch();

	@Test // GH-3567
	void skipsKotlinReflectionForNonMatchingField() throws Exception {

		Field field = DataClassWithLazy.class.getDeclaredField("amount");

		try (var reflection = mockStatic(KotlinReflectionUtils.class)) {
			assertThat(matcher.matches(field)).isFalse();
			reflection.verifyNoInteractions();
		}
	}

	@Test // GH-3567
	void skipsKotlinReflectionForNonMatchingProperty() throws Exception {

		Property property = property(DataClassWithLazy.class.getDeclaredField("amount"));

		try (var reflection = mockStatic(KotlinReflectionUtils.class)) {
			assertThat(matcher.matches(property)).isFalse();
			reflection.verifyNoInteractions();
		}
	}

	@Test // GH-3567
	void matchesDelegateFieldOnKotlinDataClass() throws Exception {
		assertThat(matcher.matches(DataClassWithLazy.class.getDeclaredField("foo$delegate"))).isTrue();
	}

	@Test // GH-3567
	void matchesDelegatePropertyOnKotlinDataClass() throws Exception {
		assertThat(matcher.matches(property(DataClassWithLazy.class.getDeclaredField("foo$delegate")))).isTrue();
	}

	@Test // GH-3567
	void doesNotMatchDelegateFieldOnJavaClass() throws Exception {
		assertThat(matcher.matches(JavaSample.class.getDeclaredField("foo$delegate"))).isFalse();
	}

	@Test // GH-3567
	void doesNotMatchDelegatePropertyOnJavaClass() throws Exception {
		assertThat(matcher.matches(property(JavaSample.class.getDeclaredField("foo$delegate")))).isFalse();
	}

	@Test // GH-3567
	void doesNotMatchPropertyWithoutField() {

		Property property = mock(Property.class);
		when(property.getField()).thenReturn(Optional.empty());

		try (var reflection = mockStatic(KotlinReflectionUtils.class)) {
			assertThat(matcher.matches(property)).isFalse();
			reflection.verifyNoInteractions();
		}
	}

	@Test // GH-3567
	void doesNotMatchDelegateFieldOnRegularKotlinClass() throws Exception {
		assertThat(matcher.matches(RegularClassWithLazy.class.getDeclaredField("foo$delegate"))).isFalse();
	}

	@Test // GH-3567
	void doesNotMatchDelegatePropertyOnRegularKotlinClass() throws Exception {
		assertThat(matcher.matches(property(RegularClassWithLazy.class.getDeclaredField("foo$delegate")))).isFalse();
	}

	private static Property property(Field field) {
		return Property.of(TypeInformation.of(field.getDeclaringClass()), field);
	}

	static class JavaSample {
		Object foo$delegate;
	}
}
