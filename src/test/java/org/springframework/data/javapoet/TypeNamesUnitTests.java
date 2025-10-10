/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.javapoet;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeVariableName;
import org.springframework.util.ReflectionUtils;

/**
 * Tests for {@link TypeNames}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class TypeNamesUnitTests {

	static Stream<Arguments> typeNames() {
		return Stream.of(Arguments.of(ResolvableType.forClass(String.class), TypeName.get(String.class)),
				Arguments.of(ResolvableType.forClassWithGenerics(Set.class, String.class),
						ParameterizedTypeName.get(Set.class, String.class)),
				Arguments.of(ResolvableType.forClass(Void.class), TypeName.get(Void.class)),
				Arguments.of(ResolvableType.forClass(Iterable.class, Object.class), TypeName.get(Iterable.class)));
	}

	static Stream<Arguments> classNames() {
		return Stream.of(Arguments.of(ResolvableType.forClass(String.class), TypeName.get(String.class)),
				Arguments.of(ResolvableType.forClassWithGenerics(Set.class, String.class), TypeName.get(Set.class)),
				Arguments.of(ResolvableType.forClass(Void.class), TypeName.get(Void.class)),
				Arguments.of(ResolvableType.forClass(Iterable.class, Object.class), TypeName.get(Iterable.class)));
	}

	@ParameterizedTest // GH-3357
	@MethodSource
	void typeNames(ResolvableType resolvableType, TypeName expected) {
		assertThat(TypeNames.typeName(resolvableType)).isEqualTo(expected);
	}

	@ParameterizedTest // GH-3357
	@MethodSource
	void classNames(ResolvableType resolvableType, TypeName expected) {
		assertThat(TypeNames.className(resolvableType)).isEqualTo(expected);
	}

	@Test // GH-3374
	void resolvedTypeNamesWithoutGenerics() {

		ResolvableType resolvableType = ResolvableType.forClass(List.class);
		assertThat(TypeNames.resolvedTypeName(resolvableType)).extracting(TypeName::toString).isEqualTo("java.util.List");
	}

	static List<Method> concreteMethods() {

		List<Method> methods = new ArrayList<>();

		ReflectionUtils.doWithMethods(Concrete.class, method -> {
			if (!method.getName().contains("baseMethod")) {
				return;
			}
			methods.add(method);
		});

		return methods;
	}

	static List<Method> otherMethods() {

		List<Method> methods = new ArrayList<>();

		ReflectionUtils.doWithMethods(Concrete.class, method -> {
			if (!method.getName().contains("otherMethod")) {
				return;
			}
			methods.add(method);
		});

		return methods;
	}

	@ParameterizedTest // GH-3374
	@MethodSource("concreteMethods")
	void resolvedTypeNamesForMethodParameters(Method method) {

		MethodParameter refiedObjectMethodParameter = new MethodParameter(method, 0).withContainingClass(Concrete.class);
		ResolvableType resolvedObjectParameterType = ResolvableType.forMethodParameter(refiedObjectMethodParameter);
		assertThat(TypeNames.typeName(resolvedObjectParameterType)).isEqualTo(TypeVariableName.get("T"));
		assertThat(TypeNames.resolvedTypeName(resolvedObjectParameterType)).isEqualTo(TypeName.get(MyType.class));

		MethodParameter refiedCollectionMethodParameter = new MethodParameter(method, 1)
				.withContainingClass(Concrete.class);
		ResolvableType resolvedCollectionParameterType = ResolvableType.forMethodParameter(refiedCollectionMethodParameter);
		assertThat(TypeNames.typeName(resolvedCollectionParameterType))
				.isEqualTo(ParameterizedTypeName.get(ClassName.get(java.util.List.class), TypeVariableName.get("T")));
		assertThat(TypeNames.resolvedTypeName(resolvedCollectionParameterType))
				.isEqualTo(ParameterizedTypeName.get(java.util.List.class, MyType.class));

		MethodParameter refiedArrayMethodParameter = new MethodParameter(method, 2).withContainingClass(Concrete.class);
		ResolvableType resolvedArrayParameterType = ResolvableType.forMethodParameter(refiedArrayMethodParameter);
		assertThat(TypeNames.typeName(resolvedArrayParameterType)).extracting(TypeName::toString).isEqualTo("T[]");
		assertThat(TypeNames.resolvedTypeName(resolvedArrayParameterType)).extracting(TypeName::toString)
				.isEqualTo("org.springframework.data.javapoet.TypeNamesUnitTests.MyType[]");

		ResolvableType resolvedReturnType = ResolvableType.forMethodReturnType(method, Concrete.class);
		assertThat(TypeNames.typeName(resolvedReturnType))
				.isEqualTo(ParameterizedTypeName.get(ClassName.get(java.util.List.class), TypeVariableName.get("T")));
		assertThat(TypeNames.resolvedTypeName(resolvedReturnType))
				.isEqualTo(ParameterizedTypeName.get(java.util.List.class, MyType.class));

	}

	@ParameterizedTest // GH-3374
	@MethodSource("otherMethods")
	void resolvedTypeNamesForOtherMethodParameters(Method method) {

		MethodParameter refiedObjectMethodParameter = new MethodParameter(method, 0).withContainingClass(Concrete.class);
		ResolvableType resolvedObjectParameterType = ResolvableType.forMethodParameter(refiedObjectMethodParameter);
		assertThat(TypeNames.typeName(resolvedObjectParameterType)).isEqualTo(TypeVariableName.get("RT"));
		assertThat(TypeNames.resolvedTypeName(resolvedObjectParameterType)).isEqualTo(TypeName.get(Object.class));

		MethodParameter refiedCollectionMethodParameter = new MethodParameter(method, 1)
				.withContainingClass(Concrete.class);
		ResolvableType resolvedCollectionParameterType = ResolvableType.forMethodParameter(refiedCollectionMethodParameter);
		assertThat(TypeNames.typeName(resolvedCollectionParameterType))
				.isEqualTo(ParameterizedTypeName.get(ClassName.get(java.util.List.class), TypeVariableName.get("RT")));
		assertThat(TypeNames.resolvedTypeName(resolvedCollectionParameterType))
				.isEqualTo(ClassName.get(java.util.List.class));

		MethodParameter refiedArrayMethodParameter = new MethodParameter(method, 2).withContainingClass(Concrete.class);
		ResolvableType resolvedArrayParameterType = ResolvableType.forMethodParameter(refiedArrayMethodParameter);
		assertThat(TypeNames.typeName(resolvedArrayParameterType)).extracting(TypeName::toString).isEqualTo("RT[]");
		assertThat(TypeNames.resolvedTypeName(resolvedArrayParameterType)).extracting(TypeName::toString)
				.isEqualTo("java.lang.Object[]");

		ResolvableType resolvedReturnType = ResolvableType.forMethodReturnType(method, Concrete.class);
		assertThat(TypeNames.typeName(resolvedReturnType)).extracting(TypeName::toString).isEqualTo("RT");
		assertThat(TypeNames.resolvedTypeName(resolvedReturnType)).isEqualTo(TypeName.get(Object.class));
	}

	@Test // GH-3374
	void resolvesTypeNamesForMethodParameters() throws NoSuchMethodException {

		Method method = Concrete.class.getDeclaredMethod("findByLocationNear", Point.class, Distance.class);

		ResolvableType resolvedReturnType = ResolvableType.forMethodReturnType(method, Concrete.class);

		assertThat(TypeNames.typeName(resolvedReturnType)).extracting(TypeName::toString).isEqualTo(
				"java.util.List<org.springframework.data.geo.GeoResult<org.springframework.data.javapoet.TypeNamesUnitTests.MyType>>");
		assertThat(TypeNames.resolvedTypeName(resolvedReturnType)).isEqualTo(ParameterizedTypeName
				.get(ClassName.get(java.util.List.class), ParameterizedTypeName.get(GeoResult.class, MyType.class)));
	}

	interface GenericBase<T> {

		java.util.List<T> baseMethod(T arg0, java.util.List<T> arg1, T... arg2);

		<RT> RT otherMethod(RT arg0, java.util.List<RT> arg1, RT... arg2);
	}

	interface Concrete extends GenericBase<MyType> {

		List<GeoResult<MyType>> findByLocationNear(Point point, Distance maxDistance);
	}

	static class MyType {}

}
