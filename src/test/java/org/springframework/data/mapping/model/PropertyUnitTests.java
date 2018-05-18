/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import lombok.Value;
import lombok.experimental.Wither;

import org.junit.Test;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link Property}.
 *
 * @author Mark Paluch
 */
public class PropertyUnitTests {

	@Test // DATACMNS-1322
	public void shouldNotFindWitherMethod() {

		assertThat(Property
				.of(ClassTypeInformation.from(ImmutableType.class), ReflectionUtils.findField(ImmutableType.class, "id"))
				.getWither()).isEmpty();
		assertThat(Property
				.of(ClassTypeInformation.from(ImmutableType.class), ReflectionUtils.findField(ImmutableType.class, "name"))
				.getWither()).isEmpty();
	}

	@Test // DATACMNS-1322
	public void shouldDiscoverWitherMethod() {

		Property property = Property.of(ClassTypeInformation.from(WitherType.class),
				ReflectionUtils.findField(WitherType.class, "id"));

		assertThat(property.getWither()).isPresent().hasValueSatisfying(actual -> {
			assertThat(actual.getName()).isEqualTo("withId");
			assertThat(actual.getReturnType()).isEqualTo(WitherType.class);
		});
	}

	@Value
	static class ImmutableType {
		String id;
		String name;

		ImmutableType withId(Long id) {
			return null;
		}

		ImmutableType withName(Object id) {
			return null;
		}

		Object withName(String id) {
			return null;
		}
	}

	@Value
	@Wither
	static class WitherType {
		String id;
		String name;
	}
}
