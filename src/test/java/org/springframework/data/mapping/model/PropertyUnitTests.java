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

	@Test // DATACMNS-1421
	public void shouldDiscoverDerivedWitherMethod() {

		Property property = Property.of(ClassTypeInformation.from(DerivedWitherClass.class),
				ReflectionUtils.findField(DerivedWitherClass.class, "id"));

		assertThat(property.getWither()).isPresent().hasValueSatisfying(actual -> {
			assertThat(actual.getName()).isEqualTo("withId");
			assertThat(actual.getReturnType()).isEqualTo(DerivedWitherClass.class);
			assertThat(actual.getDeclaringClass()).isEqualTo(DerivedWitherClass.class);
		});
	}

	@Test // DATACMNS-1421
	public void shouldNotDiscoverWitherMethodWithIncompatibleReturnType() {

		Property property = Property.of(ClassTypeInformation.from(AnotherLevel.class),
				ReflectionUtils.findField(AnotherLevel.class, "id"));

		assertThat(property.getWither()).isEmpty();
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

	static abstract class WitherBaseClass {

		abstract WitherBaseClass withId(String id);
	}

	static abstract class WitherIntermediateClass extends WitherBaseClass {

		abstract WitherIntermediateClass withId(String id);
	}

	static class DerivedWitherClass extends WitherIntermediateClass {

		private final String id;

		protected DerivedWitherClass(String id) {
			this.id = id;
		}

		DerivedWitherClass withId(String id) {
			return new DerivedWitherClass(id);
		}
	}

	static class AnotherLevel extends DerivedWitherClass {

		private AnotherLevel(String id) {
			super(id);
		}

		DerivedWitherClass withId(String id) {
			return new AnotherLevel(id);
		}
	}
}
