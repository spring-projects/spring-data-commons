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
package org.springframework.data.mapping.model;

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaConstructor

/**
 * Unit tests for [KotlinValueUtils].
 * @author Mark Paluch
 */
class KotlinValueUtilsUnitTests {

	@Test // GH-1947
	internal fun considersCustomInnerConstructorRules() {

		val ctor = WithCustomInner::class.constructors.iterator().next();
		assertThat(ctor.javaConstructor.toString()).contains("(org.springframework.data.mapping.model.Inner,kotlin.jvm.internal.DefaultConstructorMarker)")

		val vh = KotlinValueUtils.getConstructorValueHierarchy(
			ctor.parameters.iterator().next()
		)

		assertThat(vh.actualType).isEqualTo(Inner::class.java)
		assertThat(vh.appliesBoxing()).isFalse
	}

	@Test // GH-1947
	internal fun considersCustomInnerCopyRules() {

		val copy = KotlinCopyMethod.findCopyMethod(WithCustomInner::class.java).get();
		assertThat(copy.syntheticCopyMethod.toString()).contains("(org.springframework.data.mapping.model.WithCustomInner,org.springframework.data.mapping.model.Inner,int,java.lang.Object)")

		val vh = KotlinValueUtils.getCopyValueHierarchy(
			copy.copyFunction.parameters.get(1)
		)

		assertThat(vh.parameterType).isEqualTo(Outer::class.java)
		assertThat(vh.actualType).isEqualTo(Inner::class.java)
		assertThat(vh.appliesBoxing()).isFalse
	}

	@Test // GH-1947
	internal fun inlinesTypesToStringConstructorRules() {

		val ctor = WithStringValue::class.constructors.iterator().next();
		assertThat(ctor.javaConstructor.toString()).contains("(java.lang.String,java.lang.String,java.lang.String,java.lang.String,kotlin.jvm.internal.DefaultConstructorMarker)")

		for (kParam in ctor.parameters) {

			val vh = KotlinValueUtils.getConstructorValueHierarchy(kParam)

			assertThat(vh.actualType).isEqualTo(String::class.java)
			assertThat(vh.appliesBoxing()).describedAs(kParam.toString()).isFalse
			assertThat(vh.wrap("1")).describedAs(kParam.toString()).isEqualTo("1")
		}
	}

	@Test // GH-1947
	internal fun inlinesTypesToStringCopyRules() {

		val copy = KotlinCopyMethod.findCopyMethod(WithStringValue::class.java).get();
		assertThat(copy.syntheticCopyMethod.toString()).contains("(org.springframework.data.mapping.model.WithStringValue,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,java.lang.Object)")

		for (kParam in copy.copyFunction.parameters) {

			if (kParam.kind == KParameter.Kind.INSTANCE) {
				continue
			}

			val vh = KotlinValueUtils.getCopyValueHierarchy(kParam)

			assertThat(vh.actualType).isEqualTo(String::class.java)
			assertThat(vh.appliesBoxing()).describedAs(kParam.toString()).isFalse
			assertThat(vh.wrap("1")).describedAs(kParam.toString()).isEqualTo("1")
		}
	}

	@Test // GH-1947
	internal fun inlinesTypesToIntConstructorRules() {

		val ctor = WithPrimitiveValue::class.constructors.iterator().next();
		assertThat(ctor.javaConstructor.toString()).contains("(int,org.springframework.data.mapping.model.PrimitiveValue,int,org.springframework.data.mapping.model.PrimitiveValue,kotlin.jvm.internal.DefaultConstructorMarker)")

		val iterator = ctor.parameters.iterator()

		val nv = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(nv.actualType).isEqualTo(Int::class.java)
		assertThat(nv.appliesBoxing()).isFalse

		val nvn = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(nvn.actualType).isEqualTo(Int::class.java)
		assertThat(nvn.appliesBoxing()).isTrue
		assertThat(nvn.parameterType).isEqualTo(PrimitiveValue::class.java)

		val nvd = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(nvd.actualType).isEqualTo(Int::class.java)
		assertThat(nvd.appliesBoxing()).isFalse

		val nvdn = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(nvdn.actualType).isEqualTo(Int::class.java)
		assertThat(nvn.parameterType).isEqualTo(PrimitiveValue::class.java)
		assertThat(nvdn.appliesBoxing()).isTrue
	}

	@Test // GH-1947
	internal fun inlinesTypesToIntCopyRules() {

		val copy = KotlinCopyMethod.findCopyMethod(WithPrimitiveValue::class.java).get();
		assertThat(copy.syntheticCopyMethod.toString()).contains("(org.springframework.data.mapping.model.WithPrimitiveValue,int,org.springframework.data.mapping.model.PrimitiveValue,int,org.springframework.data.mapping.model.PrimitiveValue,int,java.lang.Object)")

		val parameters = copy.copyFunction.parameters;

		val nv = KotlinValueUtils.getConstructorValueHierarchy(parameters[1]);
		assertThat(nv.actualType).isEqualTo(Int::class.java)
		assertThat(nv.appliesBoxing()).isFalse

		val nvn = KotlinValueUtils.getConstructorValueHierarchy(parameters[2]);
		assertThat(nvn.actualType).isEqualTo(Int::class.java)
		assertThat(nvn.appliesBoxing()).isTrue
		assertThat(nvn.parameterType).isEqualTo(PrimitiveValue::class.java)

		val nvd = KotlinValueUtils.getConstructorValueHierarchy(parameters[3]);
		assertThat(nvd.actualType).isEqualTo(Int::class.java)
		assertThat(nvd.appliesBoxing()).isFalse

		val nvdn = KotlinValueUtils.getConstructorValueHierarchy(parameters[4]);
		assertThat(nvdn.actualType).isEqualTo(Int::class.java)
		assertThat(nvn.parameterType).isEqualTo(PrimitiveValue::class.java)
		assertThat(nvdn.appliesBoxing()).isTrue
	}

	@Test // GH-1947
	internal fun inlinesTypesToPrimitiveArrayCopyRules() {

		val copy = KotlinCopyMethod.findCopyMethod(WithPrimitiveArrays::class.java).get();
		assertThat(copy.syntheticCopyMethod.toString()).contains("(org.springframework.data.mapping.model.WithPrimitiveArrays,int[],int[],org.springframework.data.mapping.model.PrimitiveNullableArrayValue,int[],int[],int,java.lang.Object)")

		val parameters = copy.copyFunction.parameters;

		val pa = KotlinValueUtils.getConstructorValueHierarchy(parameters[1]);
		assertThat(pa.actualType).isEqualTo(IntArray::class.java)
		assertThat(pa.appliesBoxing()).isFalse

		val pan = KotlinValueUtils.getConstructorValueHierarchy(parameters[2]);
		assertThat(pan.actualType).isEqualTo(IntArray::class.java)
		assertThat(pan.appliesBoxing()).isFalse

		val pna = KotlinValueUtils.getConstructorValueHierarchy(parameters[3]);
		assertThat(pna.actualType).isEqualTo(IntArray::class.java)
		assertThat(pna.parameterType).isEqualTo(PrimitiveNullableArrayValue::class.java)
		assertThat(pna.appliesBoxing()).isTrue

		val pad = KotlinValueUtils.getConstructorValueHierarchy(parameters[4]);
		assertThat(pad.actualType).isEqualTo(IntArray::class.java)
		assertThat(pad.appliesBoxing()).isFalse

		val pand = KotlinValueUtils.getConstructorValueHierarchy(parameters[5]);
		assertThat(pand.actualType).isEqualTo(IntArray::class.java)
		assertThat(pand.appliesBoxing()).isFalse
	}

	@Test // GH-1947
	internal fun inlinesPrimitiveArrayConstructorRules() {

		val ctor = WithPrimitiveArrays::class.constructors.iterator().next();
		assertThat(ctor.javaConstructor.toString()).contains("(int[],int[],int[],int[],int[],kotlin.jvm.internal.DefaultConstructorMarker)")

		val iterator = ctor.parameters.iterator()

		val pa = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(pa.actualType).isEqualTo(IntArray::class.java)
		assertThat(pa.appliesBoxing()).isFalse

		val pan = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(pan.actualType).isEqualTo(IntArray::class.java)
		assertThat(pan.appliesBoxing()).isFalse

		val pna = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(pna.actualType).isEqualTo(IntArray::class.java)
		assertThat(pna.appliesBoxing()).isFalse

		val pad = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(pad.actualType).isEqualTo(IntArray::class.java)
		assertThat(pad.appliesBoxing()).isFalse

		val pand = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(pand.actualType).isEqualTo(IntArray::class.java)
		assertThat(pand.appliesBoxing()).isFalse
	}

	@Test // GH-2986
	internal fun considersGenerics() {

		val copyFunction =
			WithGenericsInConstructor::class.memberFunctions.first { it.name == "copy" }

		val vh = KotlinValueUtils.getCopyValueHierarchy(
			copyFunction.parameters.get(1)
		)
		assertThat(vh.actualType).isEqualTo(Object::class.java)
	}

	@Test // GH-2986
	internal fun considersGenericsWithBounds() {

		val copyFunction =
			WithGenericsInConstructor::class.memberFunctions.first { it.name == "copy" }

		val vh = KotlinValueUtils.getCopyValueHierarchy(
			copyFunction.parameters.get(1)
		)
		assertThat(vh.actualType).isEqualTo(Object::class.java)
	}

	@Test // GH-1947
	internal fun inlinesGenericTypesConstructorRules() {

		val ctor = WithGenericValue::class.constructors.iterator().next();
		assertThat(ctor.javaConstructor.toString()).contains("(java.lang.CharSequence,java.lang.CharSequence,java.lang.Object,kotlin.jvm.internal.DefaultConstructorMarker)")

		val iterator = ctor.parameters.iterator()

		val string = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(string.actualType).isEqualTo(CharSequence::class.java)
		assertThat(string.appliesBoxing()).isFalse

		val charseq = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(charseq.actualType).isEqualTo(CharSequence::class.java)
		assertThat(charseq.appliesBoxing()).isFalse

		val recursive = KotlinValueUtils.getConstructorValueHierarchy(iterator.next());
		assertThat(recursive.actualType).isEqualTo(Any::class.java)
		assertThat(recursive.appliesBoxing()).isFalse
	}

	@Test // GH-1947
	internal fun inlinesGenericTypesCopyRules() {

		val copy = KotlinCopyMethod.findCopyMethod(WithGenericValue::class.java).get();
		assertThat(copy.syntheticCopyMethod.toString()).contains("(org.springframework.data.mapping.model.WithGenericValue,java.lang.CharSequence,java.lang.CharSequence,org.springframework.data.mapping.model.MyGenericValue,int,java.lang.Object)")

		val parameters = copy.copyFunction.parameters;

		val string = KotlinValueUtils.getConstructorValueHierarchy(parameters[1]);
		assertThat(string.actualType).isEqualTo(CharSequence::class.java)
		assertThat(string.appliesBoxing()).isFalse

		val charseq = KotlinValueUtils.getConstructorValueHierarchy(parameters[2]);
		assertThat(charseq.actualType).isEqualTo(CharSequence::class.java)
		assertThat(charseq.appliesBoxing()).isFalse

		val recursive = KotlinValueUtils.getConstructorValueHierarchy(parameters[3]);
		assertThat(recursive.actualType).isEqualTo(Object::class.java)
		assertThat(recursive.parameterType).isEqualTo(MyGenericValue::class.java)
		assertThat(recursive.appliesBoxing()).isFalse
	}

	data class WithGenericsInConstructor<T>(val bar: T? = null)

	data class WithGenericBoundInConstructor<T : CharSequence>(val bar: T? = null)

}

