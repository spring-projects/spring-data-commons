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
@file:Suppress("UNCHECKED_CAST")

package org.springframework.data.core

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * Abstraction of a property path consisting of [KProperty1].
 *
 * @author Tjeu Kayim
 * @author Mark Paluch
 * @author Yoann de Martino
 * @since 4.1
 */
internal interface KPropertyPath<T, out P> : KProperty1<T, P> {
	val property: KProperty1<T, *>
	val leaf: KProperty1<*, P>

}

/**
 * Abstraction of a single property reference wrapping [KProperty1].
 *
 * @author Mark Paluch
 * @since 4.1
 */
internal class KSinglePropertyReference<T, M, out P>(
	val parent: KProperty1<T, M?>,
	val child: KProperty1<M, P>
) : KProperty1<T, P> by child as KProperty1<T, P>, KPropertyPath<T, P> {

	override fun get(receiver: T): P {

		val get = parent.get(receiver)

		if (get != null) {
			return child.get(get)
		}

		throw NullPointerException("Parent property returned null")
	}

	override fun getDelegate(receiver: T): Any {
		return child
	}

	override val property: KProperty1<T, *>
		get() = parent
	override val leaf: KProperty1<*, P>
		get() = child

}

/**
 * Abstraction of a property path that consists of parent [KProperty],
 * and child property [KProperty], where parent [parent] has an [Iterable]
 * of children, so it represents 1-M mapping.
 *
 * @author Mikhail Polivakha
 * @since 4.1
 */
internal class KIterablePropertyReference<T, M, out P>(
	val parent: KProperty1<T, Iterable<M?>?>,
	val child: KProperty1<M, P>
) : KProperty1<T, P> by child as KProperty1<T, P>, KPropertyPath<T, P> {

	override fun get(receiver: T): P {
		throw UnsupportedOperationException("Collection retrieval not supported")
	}

	override fun getDelegate(receiver: T): Any {
		throw UnsupportedOperationException("Collection retrieval not supported")
	}

	override val property: KProperty1<T, *>
		get() = parent
	override val leaf: KProperty1<*, P>
		get() = child

}


/**
 * Recursively construct field name for a nested property.
 *
 * @author Tjeu Kayim
 * @author Mikhail Polivakha
 * @since 4.1
 */
internal fun asString(property: KProperty<*>): String {

	return when (property) {
		is KPropertyPath<*, *> ->
			"${asString(property.property)}.${property.leaf.name}"

		else -> property.name
	}

}

