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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;

/**
 * Base class for all types that include parameterization of some kind. Crucial as we have to take note of the parent
 * class we will have to resolve generic parameters against.
 * 
 * @author Oliver Gierke
 */
class ParameterizedTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

	private final ParameterizedType type;

	/**
	 * Creates a new {@link ParameterizedTypeInformation} for the given {@link Type} and parent {@link TypeDiscoverer}.
	 * 
	 * @param type must not be {@literal null}
	 * @param parent must not be {@literal null}
	 */
	public ParameterizedTypeInformation(ParameterizedType type, TypeDiscoverer<?> parent) {
		super(type, parent, null);
		this.type = type;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getMapValueType()
	 */
	@Override
	public TypeInformation<?> getMapValueType() {

		if (Map.class.equals(getType())) {
			Type[] arguments = type.getActualTypeArguments();
			return createInfo(arguments[1]);
		}

		Class<?> rawType = getType();

		Set<Type> supertypes = new HashSet<Type>();
		supertypes.add(rawType.getGenericSuperclass());
		supertypes.addAll(Arrays.asList(rawType.getGenericInterfaces()));

		for (Type supertype : supertypes) {
			Class<?> rawSuperType = GenericTypeResolver.resolveType(supertype, getTypeVariableMap());
			if (Map.class.isAssignableFrom(rawSuperType)) {
				ParameterizedType parameterizedSupertype = (ParameterizedType) supertype;
				Type[] arguments = parameterizedSupertype.getActualTypeArguments();
				return createInfo(arguments[1]);
			}
		}

		return super.getMapValueType();
	}
}
