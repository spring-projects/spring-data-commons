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
package org.springframework.data.convert

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.mapping.PersistentEntity
import org.springframework.data.mapping.context.SamplePersistentProperty
import org.springframework.data.mapping.model.ParameterValueProvider
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer

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

		val entity = mockk<PersistentEntity<Contact, SamplePersistentProperty>>()
		val constructor = PreferredConstructorDiscoverer.discover<Contact, SamplePersistentProperty>(Contact::class.java)

		every { provider.getParameterValue<String>(any()) }.returnsMany("Walter", "White")
		every { entity.persistenceConstructor } returns constructor

		val instance: Contact = ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider)

		assertThat(instance.firstname).isEqualTo("Walter")
		assertThat(instance.lastname).isEqualTo("White")
	}

	@Test // DATACMNS-1126
	fun `should create instance and fill in defaults`() {

		val entity = mockk<PersistentEntity<ContactWithDefaulting, SamplePersistentProperty>>()
		val constructor = PreferredConstructorDiscoverer.discover<ContactWithDefaulting, SamplePersistentProperty>(ContactWithDefaulting::class.java)

		every { provider.getParameterValue<String>(any()) }.returnsMany("Walter", null)
		every { entity.persistenceConstructor } returns constructor

		val instance: ContactWithDefaulting = ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider)

		assertThat(instance.firstname).isEqualTo("Walter")
		assertThat(instance.lastname).isEqualTo("White")
	}

	data class Contact(val firstname: String, val lastname: String)

	data class ContactWithDefaulting(val firstname: String, val lastname: String = "White")
}

