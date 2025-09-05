/*
 * Copyright 2021-2025 the original author or authors.
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

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.mapping.PersistentEntity
import org.springframework.data.mapping.context.SamplePersistentProperty
import kotlin.reflect.KClass

/**
 * Unit tests for [ReflectionEntityInstantiator] creating instances using Kotlin inline classes.
 *
 * @author Mark Paluch
 * See also https://github.com/spring-projects/spring-framework/issues/28638
 */
class ReflectionEntityInstantiatorValueClassUnitTests {

	val provider = mockk<ParameterValueProvider<SamplePersistentProperty>>()

	@Test // GH-1947
	fun `should create instance`() {

		every { provider.getParameterValue<String>(any()) }.returnsMany("Walter")

		val instance: WithMyValueClass =
			construct(WithMyValueClass::class)

		assertThat(instance.id.id).isEqualTo("Walter")
	}

	@Test // GH-1947
	fun `should create instance with defaulting without value`() {

		every { provider.getParameterValue<String>(any()) } returns null

		val instance: WithNestedMyNullableValueClass = construct(WithNestedMyNullableValueClass::class)

		assertThat(instance.id?.id?.id).isEqualTo("foo")
		assertThat(instance.baz?.id).isEqualTo("id")
	}

	@Test // GH-1947
	fun `should use annotated constructor for types using nullable value class`() {

		every { provider.getParameterValue<String>(any()) }.returnsMany("Walter", null)

		val instance = construct(WithValueClassPreferredConstructor::class)

		assertThat(instance.id?.id?.id).isEqualTo("foo")
		assertThat(instance.baz?.id).isEqualTo("Walter-pref")
	}

	private fun <T : Any> construct(typeToCreate: KClass<T>): T {

		val entity = mockk<PersistentEntity<T, SamplePersistentProperty>>()
		val constructor =
			PreferredConstructorDiscoverer.discover<T, SamplePersistentProperty>(
				typeToCreate.java
			)

		every { entity.instanceCreatorMetadata } returns constructor
		every { entity.type } returns constructor!!.constructor.declaringClass
		every { entity.typeInformation } returns mockk()

		return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider)
	}

}

