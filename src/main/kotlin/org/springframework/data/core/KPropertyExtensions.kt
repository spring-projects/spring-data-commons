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
package org.springframework.data.core

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1


/**
 * Extension for [KProperty] providing an `toPath` function to render a [KProperty] in dot notation.
 *
 * @author Mark Paluch
 * @since 2.5
 * @see org.springframework.data.core.PropertyPath.toDotPath
 */
fun KProperty<*>.toDotPath(): String = asString(this)

/**
 * Extension for [KProperty1] providing an `toPropertyPath` function to create a [TypedPropertyPath].
 *
 * @author Mark Paluch
 * @since 4.1
 * @see org.springframework.data.core.PropertyPath.toDotPath
 */
fun <T : Any, P> KProperty1<T, P>.toPropertyPath(): TypedPropertyPath<T, out P> =
	KTypedPropertyPath.of(this)

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
 * @since 4.1
 */
@JvmName("div")
@Suppress("UNCHECKED_CAST")
operator fun <T, M, P> KProperty1<T, M?>.div(other: KProperty1<M, P?>): KProperty1<T, P> =
	KSinglePropertyReference(this, other) as KProperty1<T, P>

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
 * @since 4.1
 */
@JvmName("divIterable")
@Suppress("UNCHECKED_CAST")
operator fun <T, M, P> KProperty1<T, Collection<M?>?>.div(other: KProperty1<M, P?>): KProperty1<T, P> =
	KIterablePropertyReference(this, other) as KProperty1<T, P>
