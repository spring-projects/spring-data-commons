/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.convert

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.data.mapping.PersistentEntity
import org.springframework.data.mapping.context.SamplePersistentProperty
import org.springframework.data.mapping.model.ParameterValueProvider
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer

/**
 * Unit tests for [ReflectionEntityInstantiator] creating instances using Kotlin data classes.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner::class)
@Suppress("UNCHECKED_CAST")
class ReflectionEntityInstantiatorDataClassUnitTests {

	@Mock lateinit var entity: PersistentEntity<*, *>
	@Mock lateinit var provider: ParameterValueProvider<SamplePersistentProperty>

	@Test // DATACMNS-1126
	fun `should create instance`() {

		val entity = this.entity as PersistentEntity<Contact, SamplePersistentProperty>
		val constructor = PreferredConstructorDiscoverer.discover<Contact, SamplePersistentProperty>(Contact::class.java)

		doReturn("Walter", "White").`when`(provider).getParameterValue<SamplePersistentProperty>(any())
		doReturn(constructor).whenever(entity).persistenceConstructor

		val instance: Contact = ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider)

		Assertions.assertThat(instance.firstname).isEqualTo("Walter")
		Assertions.assertThat(instance.lastname).isEqualTo("White")
	}

	@Test // DATACMNS-1126
	fun `should create instance and fill in defaults`() {

		val entity = this.entity as PersistentEntity<ContactWithDefaulting, SamplePersistentProperty>
		val constructor = PreferredConstructorDiscoverer.discover<ContactWithDefaulting, SamplePersistentProperty>(ContactWithDefaulting::class.java)

		doReturn("Walter", null).`when`(provider).getParameterValue<SamplePersistentProperty>(any())
		doReturn(constructor).whenever(entity).persistenceConstructor

		val instance: ContactWithDefaulting = ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider)

		Assertions.assertThat(instance.firstname).isEqualTo("Walter")
		Assertions.assertThat(instance.lastname).isEqualTo("White")
	}

	data class Contact(val firstname: String, val lastname: String)

	data class ContactWithDefaulting(val firstname: String, val lastname: String = "White")
}

