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
package org.springframework.data.repository.aot.generate;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.javapoet.TypeNames;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.javapoet.TypeName;

/**
 * Value object that encapsulates introspection of a method's return type, providing convenient access to its
 * characteristics such as projection, optionality, array status, and actual type information.
 * <p>
 * Designed to support repository method analysis in the context of Ahead-of-Time (AOT) processing, this class leverages
 * {@link ReturnedType}, {@link ResolvableType}, and {@link TypeInformation} to expose both the declared and actual
 * return types, including handling of wrapper types, projections, and primitive types.
 * <p>
 * Typical usage involves querying the return type characteristics to drive code generation or runtime behavior in
 * repository infrastructure.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class MethodReturn {

	private final ReturnedType returnedType;
	private final Class<?> actualReturnClass;
	private final ResolvableType returnType;
	private final ResolvableType actualType;
	private final TypeName typeName;
	private final TypeName className;
	private final TypeName actualTypeName;
	private final TypeName actualClassName;

	/**
	 * Create a new {@code MethodReturn} instance based on the given {@link ReturnedType} and its {@link ResolvableType
	 * method return type}.
	 *
	 * @param returnedType the returned type to inspect.
	 * @param returnType the method return type.
	 */
	public MethodReturn(ReturnedType returnedType, ResolvableType returnType) {

		this.returnedType = returnedType;
		this.returnType = returnType;
		this.typeName = TypeNames.resolvedTypeName(returnType);
		this.className = TypeNames.className(returnType);

		Class<?> returnClass = returnType.toClass();
		TypeInformation<?> typeInformation = TypeInformation.of(returnType);
		TypeInformation<?> actualType = typeInformation.isMap() ? typeInformation
				: (typeInformation.getType().equals(Stream.class) ? typeInformation.getComponentType()
						: typeInformation.getActualType());

		if (actualType != null) {
			this.actualType = actualType.toResolvableType();
			this.actualTypeName = TypeNames.resolvedTypeName(this.actualType);
			this.actualClassName = TypeNames.className(this.actualType);
			this.actualReturnClass = actualType.getType();
		} else {
			this.actualType = returnType;
			this.actualTypeName = typeName;
			this.actualClassName = className;
			this.actualReturnClass = returnClass;
		}
	}

	/**
	 * Returns whether the method return type is a projection. Query projections (e.g. returning {@code String} or
	 * {@code int} are not considered.
	 *
	 * @return {@literal true} if the return type is a projection.
	 */
	public boolean isProjecting() {
		return returnedType.isProjecting();
	}

	/**
	 * Returns whether the method return type is an interface-based projection.
	 *
	 * @return {@literal true} if the return type is an interface-based projection.
	 */
	public boolean isInterfaceProjection() {
		return isProjecting() && returnedType.getReturnedType().isInterface();
	}

	/**
	 * Returns whether the method return type is {@code Optional}.
	 *
	 * @return {@literal true} if the return type is {@code Optional}.
	 */
	public boolean isOptional() {
		return Optional.class.isAssignableFrom(toClass());
	}

	/**
	 * Returns whether the method return type is an array.
	 *
	 * @return {@literal true} if the return type is an array.
	 */
	public boolean isArray() {
		return toClass().isArray();
	}

	/**
	 * Returns whether the method return type is {@code void}. Considers also {@link Void} and Kotlin's {@code Unit}.
	 *
	 * @return {@literal true} if the return type is {@code void}.
	 */
	public boolean isVoid() {
		return ReflectionUtils.isVoid(toClass());
	}

	/**
	 * Returns the {@link Class} representing the declared return type.
	 *
	 * @return the declared return class.
	 */
	public Class<?> toClass() {
		return returnType.toClass();
	}

	/**
	 * Returns the actual type (i.e. component type of a collection).
	 *
	 * @return the actual type.
	 */
	public ResolvableType getActualType() {
		return actualType;
	}

	/**
	 * Returns the {@link TypeName} representing the declared return type.
	 *
	 * @return the declared return type name.
	 */
	public TypeName getTypeName() {
		return typeName;
	}

	/**
	 * Returns the {@link TypeName} representing the declared return class (i.e. without generics).
	 *
	 * @return the declared return class name.
	 */
	public TypeName getClassName() {
		return className;
	}

	/**
	 * Returns the actual {@link TypeName} representing the declared return type (component type of collections).
	 *
	 * @return the actual return type name.
	 */
	public TypeName getActualTypeName() {
		return actualTypeName;
	}

	/**
	 * Returns the actual {@link TypeName} representing the declared return class (component type of collections).
	 *
	 * @return the actual return class name.
	 */
	public TypeName getActualClassName() {
		return actualClassName;
	}

	/**
	 * Returns the {@link Class} representing the actual return type.
	 *
	 * @return the actual return class.
	 */
	public Class<?> getActualReturnClass() {
		return actualReturnClass;
	}

}
