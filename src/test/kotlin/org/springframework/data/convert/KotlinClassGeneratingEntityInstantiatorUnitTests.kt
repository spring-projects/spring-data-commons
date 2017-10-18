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
import org.springframework.data.mapping.model.MappingInstantiationException
import org.springframework.data.mapping.model.ParameterValueProvider
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer
import java.lang.IllegalArgumentException

/**
 * Unit tests for [KotlinClassGeneratingEntityInstantiator] creating instances using Kotlin data classes.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner::class)
@Suppress("UNCHECKED_CAST")
class KotlinClassGeneratingEntityInstantiatorUnitTests {

	@Mock lateinit var entity: PersistentEntity<*, *>
	@Mock lateinit var provider: ParameterValueProvider<SamplePersistentProperty>

	@Test // DATACMNS-1126
	fun `should create instance`() {

		val entity = this.entity as PersistentEntity<Contact, SamplePersistentProperty>
		val constructor = PreferredConstructorDiscoverer.discover<Contact, SamplePersistentProperty>(Contact::class.java)

		doReturn("Walter", "White").`when`(provider).getParameterValue<SamplePersistentProperty>(any())
		doReturn(constructor).whenever(entity).persistenceConstructor
		doReturn(constructor.constructor.declaringClass).whenever(entity).type

		val instance: Contact = KotlinClassGeneratingEntityInstantiator().createInstance(entity, provider)

		Assertions.assertThat(instance.firstname).isEqualTo("Walter")
		Assertions.assertThat(instance.lastname).isEqualTo("White")
	}

	@Test // DATACMNS-1126
	fun `should create instance and fill in defaults`() {

		val entity = this.entity as PersistentEntity<ContactWithDefaulting, SamplePersistentProperty>
		val constructor = PreferredConstructorDiscoverer.discover<ContactWithDefaulting, SamplePersistentProperty>(ContactWithDefaulting::class.java)

		doReturn("Walter", null, "Skyler", null, null, null, null, null, null, null, /* 0-9 */
				null, null, null, null, null, null, null, null, null, null, /* 10-19 */
				null, null, null, null, null, null, null, null, null, null, /* 20-29 */
				null, "Walter", null, "Junior", null).`when`(provider).getParameterValue<SamplePersistentProperty>(any())
		doReturn(constructor).whenever(entity).persistenceConstructor
		doReturn(constructor.constructor.declaringClass).whenever(entity).type

		val instance: ContactWithDefaulting = KotlinClassGeneratingEntityInstantiator().createInstance(entity, provider)

		Assertions.assertThat(instance.prop0).isEqualTo("Walter")
		Assertions.assertThat(instance.prop2).isEqualTo("Skyler")
		Assertions.assertThat(instance.prop31).isEqualTo("Walter")
		Assertions.assertThat(instance.prop32).isEqualTo("White")
		Assertions.assertThat(instance.prop33).isEqualTo("Junior")
		Assertions.assertThat(instance.prop34).isEqualTo("White")
	}

	@Test // DATACMNS-1200
	fun `absent primitive value should cause MappingInstantiationException`() {

		val entity = this.entity as PersistentEntity<WithBoolean, SamplePersistentProperty>
		val constructor = PreferredConstructorDiscoverer.discover<WithBoolean, SamplePersistentProperty>(WithBoolean::class.java)

		doReturn(constructor).whenever(entity).persistenceConstructor
		doReturn(constructor.constructor.declaringClass).whenever(entity).type

		Assertions.assertThatThrownBy { KotlinClassGeneratingEntityInstantiator().createInstance(entity, provider) } //
				.isInstanceOf(MappingInstantiationException::class.java) //
				.hasMessageContaining("fun <init>(kotlin.Boolean)") //
				.hasCauseInstanceOf(IllegalArgumentException::class.java)
	}

	@Test // DATACMNS-1200
	fun `should apply primitive defaulting for absent parameters`() {

		val entity = this.entity as PersistentEntity<WithPrimitiveDefaulting, SamplePersistentProperty>
		val constructor = PreferredConstructorDiscoverer.discover<WithPrimitiveDefaulting, SamplePersistentProperty>(WithPrimitiveDefaulting::class.java)

		doReturn(constructor).whenever(entity).persistenceConstructor
		doReturn(constructor.constructor.declaringClass).whenever(entity).type

		val instance: WithPrimitiveDefaulting = KotlinClassGeneratingEntityInstantiator().createInstance(entity, provider)

		Assertions.assertThat(instance.aByte).isEqualTo(0)
		Assertions.assertThat(instance.aShort).isEqualTo(0)
		Assertions.assertThat(instance.anInt).isEqualTo(0)
		Assertions.assertThat(instance.aLong).isEqualTo(0L)
		Assertions.assertThat(instance.aFloat).isEqualTo(0.0f)
		Assertions.assertThat(instance.aDouble).isEqualTo(0.0)
		Assertions.assertThat(instance.aChar).isEqualTo('a')
		Assertions.assertThat(instance.aBool).isTrue()
	}

	data class Contact(val firstname: String, val lastname: String)

	data class ContactWithDefaulting(val prop0: String, val prop1: String = "White", val prop2: String,
									 val prop3: String = "White", val prop4: String = "White", val prop5: String = "White",
									 val prop6: String = "White", val prop7: String = "White", val prop8: String = "White",
									 val prop9: String = "White", val prop10: String = "White", val prop11: String = "White",
									 val prop12: String = "White", val prop13: String = "White", val prop14: String = "White",
									 val prop15: String = "White", val prop16: String = "White", val prop17: String = "White",
									 val prop18: String = "White", val prop19: String = "White", val prop20: String = "White",
									 val prop21: String = "White", val prop22: String = "White", val prop23: String = "White",
									 val prop24: String = "White", val prop25: String = "White", val prop26: String = "White",
									 val prop27: String = "White", val prop28: String = "White", val prop29: String = "White",
									 val prop30: String = "White", val prop31: String = "White", val prop32: String = "White",
									 val prop33: String, val prop34: String = "White"
	)

	data class WithBoolean(val state: Boolean)

	data class WithPrimitiveDefaulting(val aByte: Byte = 0, val aShort: Short = 0, val anInt: Int = 0, val aLong: Long = 0L,
									   val aFloat: Float = 0.0f, val aDouble: Double = 0.0, val aChar: Char = 'a', val aBool: Boolean = true)
}

