/*
 * Copyright 2011-2016 the original author or authors.
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
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * Base class for all types that include parameterization of some kind. Crucial as we have to take note of the parent
 * class we will have to resolve generic parameters against.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class ParameterizedTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

	private final ParameterizedType type;
	private Boolean resolved;

	/**
	 * Creates a new {@link ParameterizedTypeInformation} for the given {@link Type} and parent {@link TypeDiscoverer}.
	 * 
	 * @param type must not be {@literal null}
	 * @param parent must not be {@literal null}
	 */
	public ParameterizedTypeInformation(ParameterizedType type, TypeDiscoverer<?> parent,
			Map<TypeVariable<?>, Type> typeVariableMap) {

		super(type, parent, typeVariableMap);
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#doGetMapValueType()
	 */
	@Override
	protected Optional<TypeInformation<?>> doGetMapValueType() {

		if (Map.class.isAssignableFrom(getType())) {

			Type[] arguments = type.getActualTypeArguments();

			if (arguments.length > 1) {
				return Optional.of(createInfo(arguments[1]));
			}
		}

		Class<?> rawType = getType();

		Set<Type> supertypes = new HashSet<>();
		Optional.ofNullable(rawType.getGenericSuperclass()).ifPresent(it -> supertypes.add(it));
		supertypes.addAll(Arrays.asList(rawType.getGenericInterfaces()));

		Optional<TypeInformation<?>> result = supertypes.stream()//
				.map(it -> Pair.of(it, resolveType(it)))//
				.filter(it -> Map.class.isAssignableFrom(it.getSecond()))//
				.<TypeInformation<?>>map(it -> {

					ParameterizedType parameterizedSupertype = (ParameterizedType) it.getFirst();
					Type[] arguments = parameterizedSupertype.getActualTypeArguments();
					return createInfo(arguments[1]);
				}).findFirst();

		return result.isPresent() ? result : super.doGetMapValueType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getTypeParameters()
	 */
	@Override
	public List<TypeInformation<?>> getTypeArguments() {

		List<TypeInformation<?>> result = new ArrayList<TypeInformation<?>>();

		for (Type argument : type.getActualTypeArguments()) {
			result.add(createInfo(argument));
		}

		return result;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#isAssignableFrom(org.springframework.data.util.TypeInformation)
	 */
	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {

		if (this.equals(target)) {
			return true;
		}

		Class<T> rawType = getType();
		Class<?> rawTargetType = target.getType();

		if (!rawType.isAssignableFrom(rawTargetType)) {
			return false;
		}

		TypeInformation<?> otherTypeInformation = rawType.equals(rawTargetType) ? target
				: target.getSuperTypeInformation(rawType);

		List<TypeInformation<?>> myParameters = getTypeArguments();
		List<TypeInformation<?>> typeParameters = otherTypeInformation.getTypeArguments();

		if (myParameters.size() != typeParameters.size()) {
			return false;
		}

		for (int i = 0; i < myParameters.size(); i++) {
			if (!myParameters.get(i).isAssignableFrom(typeParameters.get(i))) {
				return false;
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#doGetComponentType()
	 */
	@Override
	protected Optional<TypeInformation<?>> doGetComponentType() {
		return Optional.of(createInfo(type.getActualTypeArguments()[0]));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ParameterizedTypeInformation)) {
			return false;
		}

		ParameterizedTypeInformation<?> that = (ParameterizedTypeInformation<?>) obj;

		if (this.isResolvedCompletely() && that.isResolvedCompletely()) {
			return this.type.equals(that.type);
		}

		return super.equals(obj);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#hashCode()
	 */
	@Override
	public int hashCode() {
		return isResolvedCompletely() ? this.type.hashCode() : super.hashCode();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format("%s<%s>", getType().getName(),
				StringUtils.collectionToCommaDelimitedString(getTypeArguments()));
	}

	private boolean isResolvedCompletely() {

		if (resolved != null) {
			return resolved;
		}

		Type[] typeArguments = type.getActualTypeArguments();

		if (typeArguments.length == 0) {
			return cacheAndReturn(false);
		}

		for (Type typeArgument : typeArguments) {

			TypeInformation<?> info = createInfo(typeArgument);

			if (info instanceof ParameterizedTypeInformation) {
				if (!((ParameterizedTypeInformation<?>) info).isResolvedCompletely()) {
					return cacheAndReturn(false);
				}
			}

			if (!(info instanceof ClassTypeInformation)) {
				return cacheAndReturn(false);
			}
		}

		return cacheAndReturn(true);
	}

	private boolean cacheAndReturn(boolean resolved) {

		this.resolved = resolved;
		return resolved;
	}
}
