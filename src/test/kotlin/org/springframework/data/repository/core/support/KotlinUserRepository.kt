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
package org.springframework.data.repository.core.support

import org.springframework.data.repository.Repository
import org.springframework.data.repository.sample.User

/**
 * @author Mark Paluch
 */
interface KotlinUserRepository : Repository<User, String> {

	fun findByUsername(username: String): User?

	fun findById(username: String): User

	fun findByOptionalId(username: String?): User?

	val findRouteQuery: String
}
