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
package org.springframework.data.mapping.model

import org.springframework.data.annotation.PersistenceCreator
import java.time.LocalDate

/**
 * @author Mark Paluch
 * @author Edward Poot
 */
@JvmInline
value class MyValueClass(val id: String)

/**
 * Simple class. Value class is flattened into `String`. However, `copy` requires a boxed value when called via reflection.
 */
data class WithMyValueClass(val id: MyValueClass) {

	// ByteCode explanation

	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// private WithMyValueClass(java.lang.String arg0) {}
	// ---------

	// ---------
	// synthetic constructor that we actually want to use
	// public synthetic WithMyValueClass(java.lang.String arg0, kotlin.jvm.internal.DefaultConstructorMarker arg1) {}
	// ---------

	// ---------
	// public static WithMyValueClass copy-R7yrDNU$default(WithMyValueClass var0, String var1, int var2, Object var3) {
	// ---------
}

data class WithMyValueClassPrivateConstructor private constructor(val id: MyValueClass) {

	// ByteCode explanation

	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// private WithMyValueClassPrivateConstructor(String id) {}
	// ---------
}

data class WithNullableMyValueClassPrivateConstructor private constructor(val id: MyNullableValueClass?) {

	// ByteCode explanation

	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// private WithNullableMyValueClassPrivateConstructor(MyNullableValueClass id) {}
	// ---------
}

data class WithMyValueClassPrivateConstructorAndDefaultValue private constructor(
	val id: MyValueClass = MyValueClass(
		"id"
	)
) {

	// ByteCode explanation
	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// private WithMyValueClassPrivateConstructorAndDefaultValue(java.lang.String id) {}
	// ---------

	// ---------
	// synthetic constructor that we actually want to use
	// synthetic WithMyValueClassPrivateConstructorAndDefaultValue(java.lang.String id, int arg1, kotlin.jvm.internal.DefaultConstructorMarker arg2) {}
	// ---------

}

@JvmInline
value class MyNullableValueClass(val id: String? = "id")

@JvmInline
value class MyNestedNullableValueClass(val id: MyNullableValueClass)

@JvmInline
value class MyGenericValue<T>(val id: T)

@JvmInline
value class MyGenericBoundValue<T : CharSequence>(val id: T)

data class MyEntity(
	val id: Long = 0L,
	val name: String,
	val createdBy: MyValueClass = MyValueClass("UNKNOWN"),
)

data class WithGenericValue(
	// ctor: WithGenericValue(CharSequence string, CharSequence charseq, Object recursive, DefaultConstructorMarker $constructor_marker)
	val string: MyGenericBoundValue<String>,
	val charseq: MyGenericBoundValue<CharSequence>,
	val recursive: MyGenericValue<MyGenericValue<String>>

	// copy: copy-XQwFSJ0$default(WithGenericValue var0, CharSequence var1, CharSequence var2, MyGenericValue var3, int var4, Object var5) {
)

data class WithGenericNullableValue(val recursive: MyGenericValue<MyGenericValue<String>>?)

@JvmInline
value class PrimitiveNullableValue(val id: Int?)

data class WithDefaultPrimitiveValue(
	val pvd: PrimitiveValue = PrimitiveValue(1)
)

/**
 * Copy method for nullable value component type uses wrappers while constructor uses the component type.
 */
data class WithPrimitiveNullableValue(
	// ctor:    public WithPrimitiveNullableValue(Integer nv, PrimitiveNullableValue nvn, Integer nvd, PrimitiveNullableValue nvdn, DefaultConstructorMarker $constructor_marker) {
	val nv: PrimitiveNullableValue,
	val nvn: PrimitiveNullableValue?,
	val nvd: PrimitiveNullableValue = PrimitiveNullableValue(1),
	val nvdn: PrimitiveNullableValue? = PrimitiveNullableValue(1),

	// copy:   copy-lcs_1S0$default(WithPrimitiveNullableValue var0, PrimitiveNullableValue var1, PrimitiveNullableValue var2, PrimitiveNullableValue var3, PrimitiveNullableValue var4, int var5, Object var6)
)

@JvmInline
value class PrimitiveArrayValue(val ids: IntArray)

@JvmInline
value class PrimitiveNullableArrayValue(val ids: IntArray?)

data class WithPrimitiveArrays(

	// ctor WithPrimitiveArrays(int[], int[], int[], int[], int[], int, DefaultConstructorMarker)

	val pa: PrimitiveArrayValue,
	val pan: PrimitiveArrayValue?,
	val pna: PrimitiveNullableArrayValue,
	val pad: PrimitiveArrayValue = PrimitiveArrayValue(intArrayOf(1, 2, 3)),
	val pand: PrimitiveArrayValue? = PrimitiveArrayValue(intArrayOf(1, 2, 3))

	// copy: copy-NCSWWqw$default(WithPrimitiveArrays var0, int[] var1, int[] var2, PrimitiveNullableArrayValue var3, int[] var4, int[] var5, int var4, Object var5) {
)

@JvmInline
value class PrimitiveValue(val id: Int)

data class WithPrimitiveValue(

	// ctor: int,org.springframework.data.mapping.model.KotlinValueUtilsUnitTests$PrimitiveValue,int,org.springframework.data.mapping.model.KotlinValueUtilsUnitTests$PrimitiveValue,kotlin.jvm.internal.DefaultConstructorMarker
	val nv: PrimitiveValue,
	val nvn: PrimitiveValue?,
	val nvd: PrimitiveValue = PrimitiveValue(1),
	val nvdn: PrimitiveValue? = PrimitiveValue(1),

	// copy: copy-XQwFSJ0$default(WithPrimitiveValue var0, int var1, PrimitiveValue var2, int var3, PrimitiveValue var4, int var5, Object var6)
)

@JvmInline
value class StringValue(val id: String)

data class WithStringValue(

	// ctor: WithStringValue(String nv, String nvn, String nvd, String nvdn)
	val nv: StringValue,
	val nvn: StringValue?,
	val nvd: StringValue = StringValue("1"),
	val nvdn: StringValue? = StringValue("1"),

	// copy: copy-QB2wzyg$default(WithStringValue var0, String var1, String var2, String var3, String var4, int var5, Object var6)
)


data class Inner(val id: LocalDate, val foo: String)

@JvmInline
value class Outer(val id: Inner = Inner(LocalDate.MAX, ""))

data class WithCustomInner(

	// ctor: private WithCustomInner(Inner nv)
	val nv: Outer

	// copy: copy(org.springframework.data.mapping.model.WithCustomInner,org.springframework.data.mapping.model.Inner,int,java.lang.Object)
)

class WithNestedMyNullableValueClass(
	var id: MyNestedNullableValueClass? = MyNestedNullableValueClass(
		MyNullableValueClass("foo")
	), var baz: MyNullableValueClass? = MyNullableValueClass("id")

	// ByteCode explanation

	// ---------
	// private WithNestedMyNullableValueClass(MyNestedNullableValueClass id, MyNullableValueClass baz) {}
	// ---------

	// ---------
	// default constructor, detected by Discoverers.KOTLIN
	// note that these constructors use boxed variants ("MyNestedNullableValueClass")

	// public synthetic WithNestedMyNullableValueClass(MyNestedNullableValueClass id, MyNullableValueClass baz, DefaultConstructorMarker $constructor_marker) {}
	// ---------

	// ---------
	// translated by KotlinInstantiationDelegate.resolveKotlinJvmConstructor as we require a constructor that we can use to
	// provide the defaulting mask. This constructor only gets generated when parameters are nullable, otherwise
	// Kotlin doesn't create this constructor

	// public synthetic WithNestedMyNullableValueClass(MyNestedNullableValueClass var1, MyNullableValueClass var2, int var3, DefaultConstructorMarker var4) {}
	// ---------
)

/**
 * Nullability on the domain class means that public and static copy methods uses both the boxed type.
 */
data class DataClassWithNullableValueClass(

	val fullyNullable: MyNullableValueClass? = MyNullableValueClass("id"),
	val innerNullable: MyNullableValueClass = MyNullableValueClass("id")

	// ByteCode

	// private final MyNullableValueClass fullyNullable;
	// private final String innerNullable;

	// ---------
	// private DataClassWithNullableValueClass(MyNullableValueClass fullyNullable, String innerNullable)
	// ---------

	// ---------
	// public DataClassWithNullableValueClass(MyNullableValueClass var1, MyNullableValueClass var2, int var3, DefaultConstructorMarker var4) {
	// ---------

	// ---------
	// public final DataClassWithNullableValueClass copy-bwh045w (@Nullable MyNullableValueClass fullyNullable, @NotNull String innerNullable) {
	// ---------

	// ---------
	// public static DataClassWithNullableValueClass copy-bwh045w$default(DataClassWithNullableValueClass var0, MyNullableValueClass var1, MyNullableValueClass var2, int var3, Object var4) {}
	// ---------
)

/**
 * Nullability on a nested level (domain class property isn't nullable, the inner value in the value class is) means that public copy method uses the value component type while the static copy method uses the boxed type.
 *
 * This creates a skew in getter vs. setter. The setter would be required to set the boxed value while the getter returns plain String. Sigh.
 */
data class DataClassWithNestedNullableValueClass(
	val nullableNestedNullable: MyNestedNullableValueClass?,
	val nestedNullable: MyNestedNullableValueClass

	// ByteCode

	// ---------
	// public DataClassWithNestedNullableValueClass(MyNestedNullableValueClass nullableNestedNullable, String nestedNullable, DefaultConstructorMarker $constructor_marker) {
	// ---------

	// ---------
	// public final DataClassWithNestedNullableValueClass copy-W2GYjxM(@Nullable MyNestedNullableValueClass nullableNestedNullable, @NotNull String nestedNullable) { … }
	// ---------

	// ---------
	// public static DataClassWithNestedNullableValueClass copy-W2GYjxM$default(DataClassWithNestedNullableValueClass var0, MyNestedNullableValueClass var1, MyNestedNullableValueClass var2, int var3, Object var4) {
	// ---------
)

class WithValueClassPreferredConstructor(
	val id: MyNestedNullableValueClass? = MyNestedNullableValueClass(
		MyNullableValueClass("foo")
	), val baz: MyNullableValueClass? = MyNullableValueClass("id")
) {

	@PersistenceCreator
	constructor(
		a: String, id: MyNestedNullableValueClass? = MyNestedNullableValueClass(
			MyNullableValueClass("foo")
		)
	) : this(id, MyNullableValueClass(a + "-pref")) {

	}

	// ByteCode explanation

	// ---------
	// private WithPreferredConstructor(MyNestedNullableValueClass id, MyNullableValueClass baz) {}
	// ---------

	// ---------
	//    public WithPreferredConstructor(MyNestedNullableValueClass var1, MyNullableValueClass var2, int var3, DefaultConstructorMarker var4) {}
	// ---------

	// ---------
	// private WithPreferredConstructor(String a, MyNestedNullableValueClass id) {}
	// ---------

	// ---------
	// this is the one we need to invoke to pass on the defaulting mask
	// public synthetic WithPreferredConstructor(String var1, MyNestedNullableValueClass var2, int var3, DefaultConstructorMarker var4) {}
	// ---------

	// ---------
	// public synthetic WithPreferredConstructor(MyNestedNullableValueClass id, MyNullableValueClass baz, DefaultConstructorMarker $constructor_marker) {
	// ---------

	// ---------
	// annotated constructor, detected by Discoverers.KOTLIN
	// @PersistenceCreator
	// public WithPreferredConstructor(String a, MyNestedNullableValueClass id, DefaultConstructorMarker $constructor_marker) {
	// ---------

}

