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
package org.springframework.data.mapping.model

/**
 * @author Mark Paluch
 */
@JvmInline
value class MyInlineClass(val id: String)

data class WithMyValueClass(val id: MyInlineClass) {

	// ByteCode explanation

	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// private WithMyValueClass(java.lang.String arg0) {}
	// ---------

	// ---------
	// synthetic constructor that we actually want to use
	// public synthetic WithMyValueClass(java.lang.String arg0, kotlin.jvm.internal.DefaultConstructorMarker arg1) {}
	// ---------
}

@JvmInline
value class MyNullableInlineClass(val id: String? = "id")

@JvmInline
value class MyNestedNullableInlineClass(val id: MyNullableInlineClass)

class WithNestedMyNullableInlineClass(
	val id: MyNestedNullableInlineClass? = MyNestedNullableInlineClass(
		MyNullableInlineClass("foo")
	), val baz: MyNullableInlineClass? = MyNullableInlineClass("id")

	// ByteCode explanation

	// ---------
	// private WithNestedMyNullableInlineClass(MyNestedNullableInlineClass id, MyNullableInlineClass baz) {}
	// ---------

	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// note that these constructors use boxed variants ("MyNestedNullableInlineClass")

	// public WithNestedMyNullableInlineClass(MyNestedNullableInlineClass id, MyNullableInlineClass baz, DefaultConstructorMarker $constructor_marker) {}
	// ---------

	// ---------
	// translated by KotlinInstantiationDelegate.resolveKotlinJvmConstructor as we require a constructor that we can use to
	// provide the nullability mask. This constructor only gets generated when parameters are nullable, otherwise
	// Kotlin doesn't create this constructor

	// public WithNestedMyNullableInlineClass(MyNestedNullableInlineClass var1, MyNullableInlineClass var2, int var3, DefaultConstructorMarker var4) {}
	// ---------

)
