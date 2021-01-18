/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.mapping.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.PersistenceConstructor
import org.springframework.data.mapping.model.AbstractPersistentPropertyUnitTests.SamplePersistentProperty

/**
 * Unit tests for [PreferredConstructorDiscoverer].
 *
 * @author Mark Paluch
 */
class PreferredConstructorDiscovererUnitTests {

	@Test // DATACMNS-1126
	fun `should discover simple constructor`() {

		val constructor = PreferredConstructorDiscoverer.discover<Simple, SamplePersistentProperty>(Simple::class.java)

		assertThat(constructor.parameters.size).isEqualTo(1)
	}

	@Test // DATACMNS-1126
	fun `should reject two constructors`() {

		val constructor = PreferredConstructorDiscoverer.discover<TwoConstructors, SamplePersistentProperty>(TwoConstructors::class.java)

		assertThat(constructor.parameters.size).isEqualTo(1)
	}

	@Test // DATACMNS-1170
	fun `should fall back to no-args constructor if no primary constructor available`() {

		val constructor = PreferredConstructorDiscoverer.discover<TwoConstructorsWithoutDefault, SamplePersistentProperty>(TwoConstructorsWithoutDefault::class.java)

		assertThat(constructor.parameters).isEmpty()
	}

	@Test // DATACMNS-1126
	fun `should discover annotated constructor`() {

		val constructor = PreferredConstructorDiscoverer.discover<AnnotatedConstructors, SamplePersistentProperty>(AnnotatedConstructors::class.java)

		assertThat(constructor.parameters.size).isEqualTo(2)
	}

	@Test // DATACMNS-1126
	fun `should discover default constructor`() {

		val constructor = PreferredConstructorDiscoverer.discover<DefaultConstructor, SamplePersistentProperty>(DefaultConstructor::class.java)

		assertThat(constructor.parameters.size).isEqualTo(1)
	}

	@Test // DATACMNS-1126
	fun `should discover default annotated constructor`() {

		val constructor = PreferredConstructorDiscoverer.discover<TwoDefaultConstructorsAnnotated, SamplePersistentProperty>(TwoDefaultConstructorsAnnotated::class.java)

		assertThat(constructor.parameters.size).isEqualTo(3)
	}

	@Test // DATACMNS-1171
	@Suppress("UNCHECKED_CAST")
	fun `should not resolve constructor for synthetic Kotlin class`() {

		val c =
			Class.forName("org.springframework.data.mapping.model.TypeCreatingSyntheticClassKt") as Class<Any>

		val constructor =
			PreferredConstructorDiscoverer.discover<Any, SamplePersistentProperty>(c)

		assertThat(constructor).isNull()
	}

	@Test // DATACMNS-1800, gh-2215
	@ExperimentalUnsignedTypes
	fun `should discover constructor for class using unsigned types`() {

		val constructor =
			PreferredConstructorDiscoverer.discover<UnsignedTypesEntity, SamplePersistentProperty>(
				UnsignedTypesEntity::class.java
			)

		assertThat(constructor).isNotNull()
	}

	data class Simple(val firstname: String)

	class TwoConstructorsWithoutDefault {

		var firstname: String? = null

		constructor() {}

		constructor(firstname: String?) {
			this.firstname = firstname
		}
	}

	class TwoConstructors(val firstname: String) {
		constructor(firstname: String, lastname: String) : this(firstname)
	}

	class AnnotatedConstructors(val firstname: String) {

		@PersistenceConstructor
		constructor(firstname: String, lastname: String) : this(firstname)
	}

	class DefaultConstructor(val firstname: String = "foo")

	class TwoDefaultConstructorsAnnotated(
		val firstname: String = "foo",
		val lastname: String = "bar"
	) {

		@PersistenceConstructor
		constructor(firstname: String = "foo", lastname: String = "bar", age: Int) : this(
			firstname,
			lastname
		)
	}

	@ExperimentalUnsignedTypes
	data class UnsignedTypesEntity(
		val id: String,
		val a: UInt = 5u,
		val b: Int = 5,
		val c: Double = 1.5
	)
}
