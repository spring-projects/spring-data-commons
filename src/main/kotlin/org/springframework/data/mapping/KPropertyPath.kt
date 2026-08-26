/*
 * Copyright 2018-present the original author or authors.
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
package org.springframework.data.mapping

import org.springframework.data.core.NestedKPropertyPath
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import org.springframework.data.core.toDotPath as coreToDotPath

/**
 * Abstraction of a property path consisting of [KProperty].
 *
 * @author Tjeu Kayim
 * @author Mark Paluch
 * @author Yoann de Martino
 * @since 2.5
 */
private class KPropertyPath<T, U>(
	val parent: KProperty<U?>,
	val child: KProperty1<U, T>
) : KProperty<T> by child, NestedKPropertyPath {

	override val property: KProperty<*>
		get() = parent
	override val leaf: KProperty<*>
		get() = child

}

/**
 * Abstraction of a property path that consists of parent [KProperty],
 * and child property [KProperty], where parent [parent] has an [Iterable]
 * of children, so it represents 1-M mapping.
 *
 * @author Mikhail Polivakha
 * @since 3.5
 */
internal class KIterablePropertyPath<T, U>(
	val parent: KProperty<Iterable<U?>?>,
	val child: KProperty1<U, T>
) : KProperty<T> by child, NestedKPropertyPath {

	override val property: KProperty<*>
		get() = parent
	override val leaf: KProperty<*>
		get() = child

}

/**
 * Render a nested property path in dot notation.
 * @author Tjeu Kayim
 * @author Mikhail Polivakha
 */
@Deprecated("since 4.1.2, use the org.springframework.data.core extensions instead")
fun asString(property: KProperty<*>): String = property.coreToDotPath()

/**
 * Builds [KPropertyPath] from Property References.
 * Refer to a nested property in an embeddable or association.
 *
 * For example, referring to the field "author.name":
 * ```
 * Book::author / Author::name isEqualTo "Herman Melville"
 * ```
 * @author Tjeu Kayim
 * @author Yoann de Martino
 * @since 2.5
 */
@JvmName("div")
@Deprecated("since 4.1, use the org.springframework.data.core extensions instead")
operator fun <T, U> KProperty<T?>.div(other: KProperty1<T, U>): KProperty<U> =
	KPropertyPath(this, other)

/**
 * Builds [KPropertyPath] from Property References.
 * Refer to a nested property in an embeddable or association.
 *
 * Note, that this function is different from [div] above in the
 * way that it represents a division operator overloading for
 * child references, where parent to child reference relation is 1-M, not 1-1.
 * It implies that parent defines a [Collection] of children.
 **
 * For example, referring to the field "books.title":
 * ```
 * Author::books / Book::title contains "Bartleby"
 * ```
 * @author Mikhail Polivakha
 * @since 3.5
 */
@JvmName("divIterable")
@Deprecated("since 4.1, use the org.springframework.data.core extensions instead")
operator fun <T, U> KProperty<Collection<T?>?>.div(other: KProperty1<T, U>): KProperty<U> =
	KIterablePropertyPath(this, other)
