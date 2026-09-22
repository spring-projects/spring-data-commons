/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.core.MemberDescriptor.KPropertyReferenceDescriptor
import kotlin.jvm.internal.PropertyReference1Impl

/**
 * Kotlin unit tests for [TypedPropertyPathFeature].
 *
 * @author Mark Paluch
 */
class TypedPropertyPathFeatureKtUnitTests {

	@Test // GH-3521
	fun shouldDescribeUnboundPropertyReference() {

		val descriptor =
			TypedPropertyPathFeature.KotlinDelegate.getPropertyReference((Person::address).javaClass)

		assertThat(descriptor).isInstanceOf(KPropertyReferenceDescriptor::class.java)
		assertThat(descriptor!!.owner).isEqualTo(Person::class.java)
		assertThat(descriptor.member).isEqualTo(Person::class.java.getMethod("getAddress"))
	}

	@Test // GH-3521
	fun shouldIgnoreBoundPropertyReference() {

		val person = Person()

		assertThat(TypedPropertyPathFeature.KotlinDelegate.getPropertyReference((person::address).javaClass)).isNull()
	}

	@Test // GH-3521
	fun shouldIgnorePropertyReferenceBaseClasses() {

		val descriptor = TypedPropertyPathFeature.KotlinDelegate.getPropertyReference(
			PropertyReference1Impl::class.java
		)

		assertThat(descriptor).isNull()
	}

	class Person {
		var address: String? = null
	}

}
