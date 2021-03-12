/*
 * Copyright 2018-2021 the original author or authors.
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

import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.*

/**
 * @author Mark Paluch
 */
data class DataClassKt(val id: String)

data class ExtendedDataClassKt(val id: Long, val name: String) {
	fun copy(name: String, id: Long): ExtendedDataClassKt {
		throw UnsupportedOperationException("Wrong copy method")
	}
}

data class SingleSettableProperty constructor(val id: UUID = UUID.randomUUID()) {
	val version: Int? = null
}

data class WithCustomCopyMethod(
	val id: String?,
	val userId: String,
	val status: String,
	val attempts: Int,
	val createdAt: LocalDateTime,
	val updatedAt: LocalDateTime,
	val sessionId: String?
) {

	fun copy(
		status: String,
		updatedAt: LocalDateTime,
		sessionId: String,
		attempts: Int = this.attempts
	) = WithCustomCopyMethod(
		this.id,
		this.userId,
		status,
		attempts,
		this.createdAt,
		updatedAt,
		sessionId
	)

}

data class ImmutableKotlinPerson(
	@Id val name: String,
	val wasOnboardedBy: List<ImmutableKotlinPerson>
)
