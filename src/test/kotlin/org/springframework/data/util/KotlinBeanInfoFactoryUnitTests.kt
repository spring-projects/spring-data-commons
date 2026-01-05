/*
 * Copyright 2023-present the original author or authors.
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
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
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

	@Test // GH-3109
	internal fun considersJavaBeansGettersOnly() {

		val pds = BeanUtils.getPropertyDescriptors(InlineClassWithProperty::class.java)

		assertThat(pds).hasSize(1).extracting("name").contains("value")
	}

	@Test // GH-3249
	internal fun considersBooleanGetAndIsGetters() {

		val isAndGet = BeanUtils.getPropertyDescriptors(KClassWithIsGetter::class.java)
		assertThat(isAndGet[0].readMethod.name).isEqualTo("isFromOuterSpace")

		val getOnly = BeanUtils.getPropertyDescriptors(KClassWithGetGetter::class.java)
		assertThat(getOnly[0].readMethod.name).isEqualTo("getFromOuterSpace")
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

		val pds =
			BeanUtils.getPropertyDescriptors(MyRepositoryFactoryBeanImpl::class.java)

		assertThat(pds).extracting("name")
			.contains("myQueryLookupStrategyKey", "repositoryBaseClass")
	}

	@Test // GH-2993
	internal fun skipsAsymmetricGettersAndSetters() {

		val pds = BeanUtils.getPropertyDescriptors(MyEntity::class.java)

		assertThat(pds).hasSize(1)
		assertThat(pds[0].writeMethod).isNull()
		assertThat(pds[0].readMethod).isNotNull()
	}

	@Test // GH-3140
	internal fun specializesBeanMethods() {

		var pds = BeanUtils.getPropertyDescriptors(Entity::class.java)

		assertThat(pds.find { it.name == "id" }!!.readMethod!!.declaringClass).isEqualTo(
			Entity::class.java
		)

		pds = BeanUtils.getPropertyDescriptors(DogEntity::class.java)

		assertThat(pds.find { it.name == "name" }!!.readMethod!!.declaringClass).isEqualTo(
			DogEntity::class.java
		)
	}

	@Test // GH-3167
	internal fun supportsPropertiesWithDifferentAccessorTypes() {

		val pds = BeanUtils.getPropertyDescriptors(User::class.java)
		assertThat(pds).isNotEmpty
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

	class MyRepositoryFactoryBeanImpl<R, E, I>(repository: Class<R>) :
		RepositoryFactoryBeanSupport<R, E, I>(repository)
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

	class Entity(
		private val id: Long? = null,

		val name: String
	) : Persistable<Long> {

		override fun getId(): Long? = id

		override fun isNew(): Boolean = id == null
	}

	open class DogEntity : Animal() {

		@Id
		override fun getName(): String {
			return super.getName()
		}
	}

	class User : AbstractAuditable() {
		var name: String? = null
	}

	open class KClassWithGetGetter() {

		private var fromOuterSpace: Boolean = false

		open fun getFromOuterSpace() = fromOuterSpace

		open fun setFromOuterSpace(newValue: Boolean) {
			this.fromOuterSpace = newValue
		}
	}

	open class KClassWithIsGetter() {

		private var fromOuterSpace: Boolean = false

		open fun isFromOuterSpace() = fromOuterSpace

		open fun getFromOuterSpace() = fromOuterSpace

		open fun setFromOuterSpace(newValue: Boolean) {
			this.fromOuterSpace = newValue
		}
	}

}
