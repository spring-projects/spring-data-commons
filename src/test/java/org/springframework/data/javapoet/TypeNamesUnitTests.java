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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;

/**
 * @author Christoph Strobl
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

}
