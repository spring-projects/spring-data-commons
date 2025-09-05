/*
 * Copyright 2013-2025 the original author or authors.
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
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Unit tests for {@link SnakeCaseFieldNamingStrategy}.
 *
 * @author Ryan Tenney
 * @author Oliver Gierke
 * @since 1.9
 */
@ExtendWith(MockitoExtension.class)
class SnakeCaseFieldNamingStrategyUnitTests {

	private FieldNamingStrategy strategy = new SnakeCaseFieldNamingStrategy();

	@Mock PersistentProperty<?> property;

	@Test // DATACMNS-523
	void rendersSnakeCaseFieldNames() {

		assertFieldNameForPropertyName("fooBar", "foo_bar");
		assertFieldNameForPropertyName("FooBar", "foo_bar");
		assertFieldNameForPropertyName("foo_bar", "foo_bar");
		assertFieldNameForPropertyName("FOO_BAR", "foo_bar");
	}

	private void assertFieldNameForPropertyName(String propertyName, String fieldName) {

		when(property.getName()).thenReturn(propertyName);
		assertThat(strategy.getFieldName(property)).isEqualTo(fieldName);
	}
}
