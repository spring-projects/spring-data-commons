/*
 * Copyright 2017-2025 the original author or authors.
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
 * @author Christoph Strobl
 */
@SuppressWarnings("deprecation")
class NullableUtilsUnitTests {

	@Test // DATACMNS-1154
	void packageAnnotatedShouldConsiderNonNullAnnotation() {

		var method = ReflectionUtils.findMethod(NonNullOnPackage.class, "nonNullReturnValue");

		assertThat(NullableUtils.isNonNull(method, ElementType.METHOD)).isTrue();
		assertThat(NullableUtils.isNonNull(method, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(method, ElementType.PACKAGE)).isFalse();
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

	@Test // DATACMNS-1154
	void shouldConsiderJsr305NonNullParameters() {

		assertThat(NullableUtils.isNonNull(NonNullableParameters.class, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(NonNullableParameters.class, ElementType.FIELD)).isFalse();
	}

	@Test // DATACMNS-1154
	void shouldConsiderJsr305NonNullAnnotation() {

		assertThat(NullableUtils.isNonNull(Jsr305NonnullAnnotatedType.class, ElementType.PARAMETER)).isTrue();
		assertThat(NullableUtils.isNonNull(Jsr305NonnullAnnotatedType.class, ElementType.FIELD)).isTrue();
	}

	@Test // DATACMNS-1154
	void shouldConsiderNonAnnotatedTypeNullable() {

		assertThat(NullableUtils.isNonNull(NonAnnotatedType.class, ElementType.PARAMETER)).isFalse();
		assertThat(NullableUtils.isNonNull(NonAnnotatedType.class, ElementType.FIELD)).isFalse();
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
	}

	@Test // DATACMNS-1154
	void shouldConsiderParametersJsr305NullableMetaAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "jsr305NullableReturn");

		assertThat(NullableUtils.isExplicitNullable(new MethodParameter(method, -1))).isTrue();
	}

	@Test // DATACMNS-1154
	void shouldConsiderParametersJsr305NonnullAnnotation() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "jsr305NullableReturnWhen");

		assertThat(NullableUtils.isExplicitNullable(new MethodParameter(method, -1))).isTrue();
	}

	@Test // GH-3242
	void shouldConsiderPrimitivesNonNullable() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "primitiveReturn");

		assertThat(NullableUtils.isNonNull(method, ElementType.METHOD)).isTrue();
		assertThat(NullableUtils.isNonNull(boolean.class, ElementType.PARAMETER)).isTrue();
	}

	@Test // GH-3242
	void shouldConsiderVoidNullable() {

		var method = ReflectionUtils.findMethod(NullableAnnotatedType.class, "noReturnActually");

		assertThat(NullableUtils.isNonNull(method, ElementType.METHOD)).isFalse();
	}

	@Test // GH-3242
	void shouldConsiderTypesNotAnnotatedWhereWeCannotDetermineThePackageAsNullable() throws ClassNotFoundException {

		Class<?> typeInDefaultPackage = getClass().getClassLoader().loadClass("TypeInDefaultPackage");

		assertThat(NullableUtils.isNonNull(typeInDefaultPackage, ElementType.PARAMETER)).isFalse();
	}
}
