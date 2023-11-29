/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.BeanUtils
import org.springframework.data.repository.Repository
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
import org.springframework.data.repository.core.support.RepositoryFactorySupport

/**
 * Unit tests for [KotlinBeanInfoFactory].
 * @author Mark Paluch
 */
class KotlinBeanInfoFactoryUnitTests {

	@Test
	internal fun determinesDataClassProperties() {

		val pds = BeanUtils.getPropertyDescriptors(SimpleDataClass::class.java)

		assertThat(pds).hasSize(2).extracting("name").contains("id", "name")

		for (pd in pds) {
			if (pd.name == "id") {
				assertThat(pd.readMethod.name).isEqualTo("getId")
				assertThat(pd.writeMethod).isNull()
			}

			if (pd.name == "name") {
				assertThat(pd.readMethod.name).isEqualTo("getName")
				assertThat(pd.writeMethod.name).isEqualTo("setName")
			}
		}
	}

	@Test
	internal fun determinesInlineClassConsumerProperties() {

		val pds = BeanUtils.getPropertyDescriptors(WithValueClass::class.java)

		assertThat(pds).hasSize(1).extracting("name").containsOnly("address")
		assertThat(pds[0].readMethod.name).startsWith("getAddress-") // hashCode suffix
		assertThat(pds[0].writeMethod.name).startsWith("setAddress-") // hashCode suffix
	}

	@Test
	internal fun determinesInlineClassWithDefaultingConsumerProperties() {

		val pds = BeanUtils.getPropertyDescriptors(WithOptionalValueClass::class.java)

		assertThat(pds).hasSize(1).extracting("name").containsOnly("address")
		assertThat(pds[0].readMethod.name).startsWith("getAddress-") // hashCode suffix
		assertThat(pds[0].writeMethod).isNull()
	}

	@Test // GH-2964
	internal fun backsOffForInterfaces() {

		val pds = BeanUtils.getPropertyDescriptors(FirstnameOnly::class.java)

		assertThat(pds).hasSize(1).extracting("name").containsOnly("firstname")
	}

	@Test // GH-2990
	internal fun backsOffForEnums() {

		val pds = BeanUtils.getPropertyDescriptors(MyEnum::class.java)

		assertThat(pds).extracting("name").contains("ordinal")
	}

	@Test // GH-2994
	internal fun includesPropertiesFromJavaSupertypes() {

		val pds = BeanUtils.getPropertyDescriptors(MyRepositoryFactoryBeanImpl::class.java)

		assertThat(pds).extracting("name").contains("myQueryLookupStrategyKey", "repositoryBaseClass")
	}

	@Test // GH-2993
	internal fun skipsAsymmetricGettersAndSetters() {

		val pds = BeanUtils.getPropertyDescriptors(MyEntity::class.java)

		assertThat(pds).hasSize(1)
		assertThat(pds[0].writeMethod).isNull()
		assertThat(pds[0].readMethod).isNotNull()
	}

	data class SimpleDataClass(val id: String, var name: String)

	@JvmInline
	value class EmailAddress(val address: String)

	data class WithValueClass(var address: EmailAddress)

	data class WithOptionalValueClass(val address: EmailAddress? = EmailAddress("un@known"))

	interface FirstnameOnly {
		fun getFirstname(): String
	}

	enum class MyEnum {
		Foo, Bar
	}

	class MyRepositoryFactoryBeanImpl<R, E, I>(repository: Class<R>) : RepositoryFactoryBeanSupport<R, E, I>(repository)
			where R : Repository<E, I>, E : Any, I : Any {

		private var myQueryLookupStrategyKey: String
			get() = ""
			set(value) {

			}

		override fun createRepositoryFactory(): RepositoryFactorySupport {
			throw UnsupportedOperationException()
		}
	}

	interface Interval<T> {
		val end: T
	}

	class MyEntity : Interval<Long> {
		override var end: Long = -1L
			protected set
	}
}
