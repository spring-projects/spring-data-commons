/*
 * Copyright 2020-2025 the original author or authors.
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
 * Extension function to compose a [TypedPropertyPath] with a [KProperty1].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> TypedPropertyPath<T, P>.then(next: KProperty1<T, P?>): TypedPropertyPath<T, N> {
	val nextPath = KTypedPropertyPath.of<T, P>(next) as TypedPropertyPath<P, N>
	return TypedPropertyPaths.ComposedPropertyPath(this, nextPath)
}

/**
 * Extension function to compose a [TypedPropertyPath] with a [KProperty].
 *
 * @since 4.1
 */
fun <T : Any, P : Any, N : Any> TypedPropertyPath<T, P>.then(next: KProperty<P?>): TypedPropertyPath<T, N> {
	val nextPath = KTypedPropertyPath.of<T, P>(next) as TypedPropertyPath<P, N>
	return TypedPropertyPaths.ComposedPropertyPath(this, nextPath)
}

/**
 * Helper to create [TypedPropertyPath] from [KProperty].
 *
 * @since 4.1
 */
class KTypedPropertyPath {

	/**
	 * Companion object for static factory methods.
	 */
	companion object {

		/**
		 * Create a [TypedPropertyPath] from a [KProperty1].
		 */
		fun <T : Any, P : Any> of(property: KProperty1<T, P?>): TypedPropertyPath<T, P> {
			return of((property as KProperty<P?>))
		}

		/**
		 * Create a [TypedPropertyPath] from a [KProperty].
		 */
		fun <T : Any, P : Any> of(property: KProperty<P?>): TypedPropertyPath<T, P> {

			if (property is KProperty1<*, *>) {
				val metadata = TypedPropertyPaths.KPropertyPathMetadata.of(
					MemberDescriptor.KPropertyReferenceDescriptor.create(
						String::class.java,
						property as KProperty1<*, *>
					)
				)
				return TypedPropertyPaths.ResolvedKPropertyPath(metadata)
			}

			if (property is KPropertyPath<*, *>) {

				val paths = property as KPropertyPath<*, *>

				val parent = of<Any, Any>(paths.parent)
				val child = of<Any, Any>(paths.child)

				return TypedPropertyPaths.ComposedPropertyPath(parent, child) as TypedPropertyPath<T, P>
			}

			if (property is KIterablePropertyPath<*, *>) {

				val paths = property as KIterablePropertyPath<*, *>

				val parent = of<Any, Any>(paths.parent)
				val child = of<Any, Any>(paths.child)

				return TypedPropertyPaths.ComposedPropertyPath(parent, child) as TypedPropertyPath<T, P>
			}

			throw IllegalArgumentException("Property ${property.name} is not a KProperty")
		}

	}

}