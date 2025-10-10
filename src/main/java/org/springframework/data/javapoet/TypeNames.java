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

import java.util.Arrays;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ArrayTypeName;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ClassUtils;

/**
 * Collection of {@link org.springframework.javapoet.TypeName} transformation utilities.
 * <p>
 * This class delivers some simple functionality that should be provided by the JavaPoet framework. It also provides
 * easy-to-use methods to convert between types.
 * <p>
 * Mainly for internal use within the framework
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 4.0
 */
public abstract class TypeNames {

	/**
	 * Obtain a {@link TypeName class name} for the given type, resolving primitive wrappers as necessary.
	 *
	 * @param type the class to use.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName classNameOrWrapper(Class<?> type) {
		return ClassUtils.isPrimitiveOrWrapper(type) ? TypeName.get(ClassUtils.resolvePrimitiveIfNecessary(type))
				: TypeName.get(type);
	}

	/**
	 * Obtain a {@link TypeName class name} for the given {@link ResolvableType}, resolving primitive wrappers as
	 * necessary. Ideal to represent a type name used as {@code Class} value as generic parameters are not considered.
	 *
	 * @param resolvableType the resolvable type to use.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName classNameOrWrapper(ResolvableType resolvableType) {
		return classNameOrWrapper(resolvableType.toClass());
	}

	/**
	 * Obtain a {@link TypeName} for the given {@link ResolvableType}. Ideal to represent a type name used as
	 * {@code Class} value as generic parameters are not considered.
	 *
	 * @param resolvableType the resolvable type to use.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName className(ResolvableType resolvableType) {
		return TypeName.get(resolvableType.toClass());
	}

	/**
	 * Obtain a {@link TypeName} for the underlying type of the given {@link ResolvableType}. Can render a class name, a
	 * type signature with resolved generics or a generic type variable.
	 *
	 * @param resolvableType the resolvable type represent.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName resolvedTypeName(ResolvableType resolvableType) {

		if (resolvableType.equals(ResolvableType.NONE)) {
			return TypeName.get(Object.class);
		}

		if (resolvableType.hasResolvableGenerics()) {
			return ParameterizedTypeName.get(ClassName.get(resolvableType.toClass()),
					Arrays.stream(resolvableType.getGenerics()).map(TypeNames::resolvedTypeName).toArray(TypeName[]::new));
		}

		if (!resolvableType.hasGenerics()) {

			Class<?> resolvedType = resolvableType.toClass();

			if (!resolvableType.isArray() || resolvedType.isArray()) {
				return TypeName.get(resolvedType);
			}

			if (resolvableType.isArray()) {
				return ArrayTypeName.of(resolvedType);
			}

			return TypeName.get(resolvedType);
		}

		return ClassName.get(resolvableType.toClass());
	}

	/**
	 * Obtain a {@link TypeName} for the underlying type of the given {@link ResolvableType}. Can render a class name, a
	 * type signature or a generic type variable.
	 *
	 * @param resolvableType the resolvable type represent.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName typeName(ResolvableType resolvableType) {
		return TypeName.get(resolvableType.getType());
	}

	/**
	 * Obtain a {@link TypeName} for the given type, resolving primitive wrappers as necessary. Ideal to represent a type
	 * parameter for parametrized types as primitive boxing is considered.
	 *
	 * @param type the class to be represented.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName typeNameOrWrapper(Class<?> type) {
		return typeNameOrWrapper(ResolvableType.forClass(type));
	}

	/**
	 * Obtain a {@link TypeName} for the given {@link ResolvableType}, resolving primitive wrappers as necessary. Can
	 * render a class name, a type signature or a generic type variable. Ideal to represent a type parameter for
	 * parametrized types as primitive boxing is considered.
	 *
	 * @param resolvableType the resolvable type to be represented.
	 * @return the corresponding {@link TypeName}.
	 */
	public static TypeName typeNameOrWrapper(ResolvableType resolvableType) {
		return ClassUtils.isPrimitiveOrWrapper(resolvableType.toClass())
				? TypeName.get(ClassUtils.resolvePrimitiveIfNecessary(resolvableType.toClass()))
				: resolvedTypeName(resolvableType);
	}

	private TypeNames() {}

}
