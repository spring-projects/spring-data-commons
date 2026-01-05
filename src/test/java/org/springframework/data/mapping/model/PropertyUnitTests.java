/*
 * Copyright 2018-present the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.data.core.TypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link Property}.
 *
 * @author Mark Paluch
 */
class PropertyUnitTests {

	@Test // DATACMNS-1322
	void shouldNotFindWitherMethod() {

		assertThat(Property
				.of(TypeInformation.of(ImmutableType.class), ReflectionUtils.findField(ImmutableType.class, "id")).getWither())
						.isEmpty();
		assertThat(
				Property.of(TypeInformation.of(ImmutableType.class), ReflectionUtils.findField(ImmutableType.class, "name"))
						.getWither()).isEmpty();
	}

	@Test // DATACMNS-1322
	void shouldDiscoverWitherMethod() {

		var property = Property.of(TypeInformation.of(WitherType.class), ReflectionUtils.findField(WitherType.class, "id"));

		assertThat(property.getWither()).isPresent().hasValueSatisfying(actual -> {
			assertThat(actual.getName()).isEqualTo("withId");
			assertThat(actual.getReturnType()).isEqualTo(WitherType.class);
		});
	}

	@Test // DATACMNS-1421
	void shouldDiscoverDerivedWitherMethod() {

		var property = Property.of(TypeInformation.of(DerivedWitherClass.class),
				ReflectionUtils.findField(DerivedWitherClass.class, "id"));

		assertThat(property.getWither()).isPresent().hasValueSatisfying(actual -> {
			assertThat(actual.getName()).isEqualTo("withId");
			assertThat(actual.getReturnType()).isEqualTo(DerivedWitherClass.class);
			assertThat(actual.getDeclaringClass()).isEqualTo(DerivedWitherClass.class);
		});
	}

	@Test // DATACMNS-1421
	void shouldNotDiscoverWitherMethodWithIncompatibleReturnType() {

		var property = Property.of(TypeInformation.of(AnotherLevel.class),
				ReflectionUtils.findField(AnotherLevel.class, "id"));

		assertThat(property.getWither()).isEmpty();
	}

	static class ImmutableType {

		final String id;
		final String name;

		public ImmutableType(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

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

	private static final class WitherType {

		private final String id;
		private final String name;

		public WitherType(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return this.id;
		}

		public String getName() {
			return this.name;
		}

		public WitherType withId(String id) {
			return this.id == id ? this : new WitherType(id, this.name);
		}

		public WitherType withName(String name) {
			return this.name == name ? this : new WitherType(this.id, name);
		}
	}

	static abstract class WitherBaseClass {

		abstract WitherBaseClass withId(String id);
	}

	static abstract class WitherIntermediateClass extends WitherBaseClass {

		@Override
		abstract WitherIntermediateClass withId(String id);
	}

	static class DerivedWitherClass extends WitherIntermediateClass {

		private final String id;

		DerivedWitherClass(String id) {
			this.id = id;
		}

		@Override
		DerivedWitherClass withId(String id) {
			return new DerivedWitherClass(id);
		}
	}

	static class AnotherLevel extends DerivedWitherClass {

		private AnotherLevel(String id) {
			super(id);
		}

		@Override
		DerivedWitherClass withId(String id) {
			return new AnotherLevel(id);
		}
	}
}
