/*
 * Copyright 2017-2024 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.ElementType;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.data.util.nonnull.NullableAnnotatedType;
import org.springframework.data.util.nonnull.packagelevel.NonNullOnPackage;
import org.springframework.data.util.nonnull.type.Jsr305NonnullAnnotatedType;
import org.springframework.data.util.nonnull.type.NonAnnotatedType;
import org.springframework.data.util.nonnull.type.NonNullableParameters;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link NullableUtils}.
 *
 * @author Mark Paluch
 */
class NullableUtilsUnitTests {

	@Test // DATACMNS-1154
	void packageAnnotatedShouldConsiderNonNullAnnotation() {

		var method = ReflectionUtils.findMethod(NonNullOnPackage.class, "nonNullArgs", String.class);

		assertThat(NullableUtils.isNonNull(method, ElementType.METHOD)).isTrue();
		assertThat(NullableUtils.isNonNull(method, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(method, ElementType.PACKAGE)).isFalse();

		Nullability.Introspector introspector = Nullability.introspect(NonNullOnPackage.class);
		Nullability mrt = introspector.forReturnType(method);

		assertThat(mrt.isNullable()).isFalse();
		assertThat(mrt.isNonNull()).isTrue();

		Nullability pn = introspector.forParameter(MethodParameter.forExecutable(method, 0));

		assertThat(pn.isNullable()).isFalse();
		assertThat(pn.isNonNull()).isTrue();
	}

	@Test // DATACMNS-1154
	void packageAnnotatedShouldConsiderNonNullAnnotationForClass() {

		assertThat(NullableUtils.isNonNull(NonNullOnPackage.class, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(NonNullOnPackage.class, ElementType.PACKAGE)).isFalse();
	}

	@Test // DATACMNS-1154
	void packageAnnotatedShouldConsiderNonNullAnnotationForMethod() {

		assertThat(NullableUtils.isNonNull(NonNullOnPackage.class, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(NonNullOnPackage.class, ElementType.PACKAGE)).isFalse();
	}

	@Test //
	void shouldConsiderJakartaNonNullParameters() {

		var method = ReflectionUtils.findMethod(org.springframework.data.util.nonnull.jakarta.NonNullOnPackage.class, "someMethod", String.class, String.class);
		Nullability.Introspector introspector = Nullability.introspect(method.getDeclaringClass());
		Nullability mrt = introspector.forReturnType(method);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNonNull()).isTrue();
		assertThat(mrt.isNullable()).isFalse();

		Nullability pn0 = introspector.forParameter(MethodParameter.forExecutable(method, 0));
		assertThat(pn0.isDeclared()).isTrue();
		assertThat(pn0.isNullable()).isFalse();
		assertThat(pn0.isNonNull()).isTrue();

		Nullability pn1 = introspector.forParameter(MethodParameter.forExecutable(method, 1));
		assertThat(pn1.isDeclared()).isTrue();
		assertThat(pn1.isNullable()).isTrue();
		assertThat(pn1.isNonNull()).isFalse();
	}

	@Test // DATACMNS-1154
	void shouldConsiderJsr305NonNullParameters() {

		assertThat(NullableUtils.isNonNull(NonNullableParameters.class, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(NonNullableParameters.class, ElementType.FIELD)).isFalse();

		var method = ReflectionUtils.findMethod(NonNullableParameters.class, "someMethod", String.class);
		Nullability.Introspector introspector = Nullability.introspect(method.getDeclaringClass());
		Nullability mrt = introspector.forReturnType(method);

		assertThat(mrt.isDeclared()).isFalse();
		assertThat(mrt.isNonNull()).isFalse();
		assertThat(mrt.isNullable()).isTrue();

		Nullability pn = introspector.forParameter(MethodParameter.forExecutable(method, 0));
		assertThat(pn.isDeclared()).isTrue();
		assertThat(pn.isNullable()).isFalse();
		assertThat(pn.isNonNull()).isTrue();
	}

	@Test // DATACMNS-1154
	void shouldConsiderJsr305NonNullAnnotation() {

		assertThat(NullableUtils.isNonNull(Jsr305NonnullAnnotatedType.class, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(Jsr305NonnullAnnotatedType.class, ElementType.FIELD)).isTrue();

		var method = ReflectionUtils.findMethod(Jsr305NonnullAnnotatedType.class, "someMethod", String.class);

		Nullability mrt = Nullability.forMethodReturnType(method);
		Nullability pn = Nullability.forMethodParameter(method.getParameters()[0]);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNullable()).isFalse();
		assertThat(mrt.isNonNull()).isTrue();

		assertThat(pn.isDeclared()).isTrue();
		assertThat(pn.isNullable()).isFalse();
		assertThat(pn.isNonNull()).isTrue();
	}

	@Test // DATACMNS-1154
	void shouldConsiderNonAnnotatedTypeNullable() {

		assertThat(NullableUtils.isNonNull(NonAnnotatedType.class, ElementType.PARAMETER)).isFalse();
		assertThat(NullableUtils.isNonNull(NonAnnotatedType.class, ElementType.FIELD)).isFalse();

		var method = ReflectionUtils.findMethod(NonAnnotatedType.class, "someMethod", String.class);

		Nullability mrt = Nullability.forMethodReturnType(method);
		Nullability pn = Nullability.forMethodParameter(method.getParameters()[0]);

		assertThat(mrt.isDeclared()).isFalse();
		assertThat(mrt.isNullable()).isTrue();
		assertThat(mrt.isNonNull()).isFalse();

		assertThat(pn.isDeclared()).isFalse();
		assertThat(pn.isNullable()).isTrue();
		assertThat(pn.isNonNull()).isFalse();
	}

	@Test // DATACMNS-1154
	void shouldConsiderParametersWithoutNullableAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "nonNullMethod", String.class);

		var returnValue = new MethodParameter(method, -1);
		var parameter = new MethodParameter(method, 0);

		assertThat(NullableUtils.isExplicitNullable(returnValue)).isFalse();
		assertThat(NullableUtils.isExplicitNullable(parameter)).isFalse();
	}

	@Test // DATACMNS-1154
	void shouldConsiderParametersNullableAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "nullableReturn");

		assertThat(NullableUtils.isExplicitNullable(new MethodParameter(method, -1))).isTrue();

		Nullability mrt = Nullability.forMethodReturnType(method);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNullable()).isTrue();
		assertThat(mrt.isNonNull()).isFalse();
	}

	@Test // DATACMNS-1154
	void shouldConsiderMethodReturnJsr305NullableMetaAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "jsr305NullableReturn");

		assertThat(NullableUtils.isExplicitNullable(new MethodParameter(method, -1))).isTrue();

		Nullability mrt = Nullability.forMethodReturnType(method);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNullable()).isTrue();
		assertThat(mrt.isNonNull()).isFalse();
	}

	@Test // DATACMNS-1154
	void shouldConsiderMethodReturnJsr305NonnullAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "jsr305NullableReturnWhen");

		assertThat(NullableUtils.isExplicitNullable(new MethodParameter(method, -1))).isTrue();

		Nullability mrt = Nullability.forMethodReturnType(method);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNullable()).isTrue();
		assertThat(mrt.isNonNull()).isFalse();
	}

	@Test //
	void shouldConsiderMethodReturnJakartaNonnullAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "jakartaNonnullReturnWhen");

		Nullability mrt = Nullability.forMethodReturnType(method);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNullable()).isFalse();
		assertThat(mrt.isNonNull()).isTrue();
	}

	@Test //
	void shouldConsiderMethodReturnJakartaNullableAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "jakartaNullableReturnWhen");

		Nullability mrt = Nullability.forMethodReturnType(method);

		assertThat(mrt.isDeclared()).isTrue();
		assertThat(mrt.isNullable()).isTrue();
		assertThat(mrt.isNonNull()).isFalse();
	}
}
