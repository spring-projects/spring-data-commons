/*
 * Copyright 2023-2025 the original author or authors.
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
// TODO SPLIT
class KotlinBeanInfoFactoryUnitTests {

	@Test // GH-2994
	internal fun includesPropertiesFromJavaSupertypes() {

		val pds =
			BeanUtils.getPropertyDescriptors(MyRepositoryFactoryBeanImpl::class.java)

		assertThat(pds).extracting("name")
			.contains("myQueryLookupStrategyKey", "repositoryBaseClass")
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
}
