/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Property information for a plain {@link Class}.
 *
 * @author Oliver Gierke
 */
public class ClassTypeInformation<S> extends TypeDiscoverer<S> {

	private final Class<S> type;

	/**
	 * Simple factory method to easily create new instances of {@link ClassTypeInformation}.
	 *  
	 * @param <S>
	 * @param type
	 * @return
	 */
	public static <S> TypeInformation<S> from(Class<S> type) {
		return new ClassTypeInformation<S>(type);
	}

	/**
	 * Creates {@link ClassTypeInformation} for the given type.
	 *
	 * @param type
	 */
	public ClassTypeInformation(Class<S> type) {
		this(type, GenericTypeResolver.getTypeVariableMap(type));
	}


	@SuppressWarnings("rawtypes") 
	ClassTypeInformation(Class<S> type, Map<TypeVariable, Type> typeVariableMap) {
		super(type, typeVariableMap);
		this.type = type;
	}

	/*
		 * (non-Javadoc)
		 *
		 * @see org.springframework.data.document.mongodb.TypeDiscovererTest.FieldInformation#getType()
		 */
	@Override
	public Class<S> getType() {
		return type;
	}

	/* (non-Javadoc)
		 * @see org.springframework.data.util.TypeDiscoverer#getComponentType()
		 */
	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public TypeInformation<?> getComponentType() {

		if (type.isArray()) {
			return createInfo(resolveArrayType(type));
		}

		TypeVariable<?>[] typeParameters = type.getTypeParameters();
		return typeParameters.length > 0 ? new TypeVariableTypeInformation(typeParameters[0], this.getType(), this) : null;
	}

	private static Type resolveArrayType(Class<?> type) {
		Assert.isTrue(type.isArray());
		Class<?> componentType = type.getComponentType();
		return componentType.isArray() ? resolveArrayType(componentType) : componentType;
	}
}