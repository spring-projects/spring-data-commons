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
 * Unit tests for [ReflectionEntityInstantiator] creating instances using Kotlin data classes.
 *
 * @author Mark Paluch
 * @author Sebastien Deleuze
 */
@Suppress("UNCHECKED_CAST")
class ReflectionEntityInstantiatorDataClassUnitTests {

	val provider = mockk<ParameterValueProvider<SamplePersistentProperty>>()

	@Test // DATACMNS-1126
	fun `should create instance`() {

		every { provider.getParameterValue<String>(any()) }.returnsMany("Walter", "White")

		val instance: Contact = construct(Contact::class)

		assertThat(instance.firstname).isEqualTo("Walter")
		assertThat(instance.lastname).isEqualTo("White")
	}

	@Test // DATACMNS-1126
	fun `should create instance and fill in defaults`() {

		every { provider.getParameterValue<String>(any()) }.returnsMany("Walter", null)

		val instance: ContactWithDefaulting = construct(ContactWithDefaulting::class)

		assertThat(instance.firstname).isEqualTo("Walter")
		assertThat(instance.lastname).isEqualTo("White")
	}

	@Test // GH-1947
	fun `should instantiate type with value class defaulting`() {

		every { provider.getParameterValue<Int>(any()) }.returns(1)

		val instance = construct(WithDefaultPrimitiveValue::class)

		assertThat(instance.pvd.id).isEqualTo(1)
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

	data class Contact(val firstname: String, val lastname: String)

	data class ContactWithDefaulting(val firstname: String, val lastname: String = "White")
}

