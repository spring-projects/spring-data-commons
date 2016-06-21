/*
 * Copyright 2011-2015 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Basic {@link TypeDiscoverer} that contains basic functionality to discover property types.
 * 
 * @author Oliver Gierke
 */
class TypeDiscoverer<S> implements TypeInformation<S> {

	private final Type type;
	private final Map<TypeVariable<?>, Type> typeVariableMap;
	private final Map<String, Optional<TypeInformation<?>>> fieldTypes = new ConcurrentHashMap<>();
	private final int hashCode;

	private final Lazy<Class<S>> resolvedType;
	private final Lazy<Optional<TypeInformation<?>>> componentType;
	private final Lazy<Optional<TypeInformation<?>>> valueType;

	/**
	 * Creates a new {@link TypeDiscoverer} for the given type, type variable map and parent.
	 * 
	 * @param type must not be {@literal null}.
	 * @param typeVariableMap must not be {@literal null}.
	 */
	protected TypeDiscoverer(Type type, Map<TypeVariable<?>, Type> typeVariableMap) {

		Assert.notNull(type);
		Assert.notNull(typeVariableMap);

		this.type = type;
		this.resolvedType = Lazy.of(() -> resolveType(type));
		this.componentType = Lazy.of(() -> doGetComponentType());
		this.valueType = Lazy.of(() -> doGetMapValueType());
		this.typeVariableMap = typeVariableMap;
		this.hashCode = 17 + (31 * type.hashCode()) + (31 * typeVariableMap.hashCode());
	}

	/**
	 * Returns the type variable map.
	 * 
	 * @return
	 */
	protected Map<TypeVariable<?>, Type> getTypeVariableMap() {
		return typeVariableMap;
	}

	private TypeInformation<?> createInfo(Optional<Type> fieldType) {
		return fieldType.map(it -> createInfo(it)).orElseThrow(() -> new IllegalArgumentException());
	}

	/**
	 * Creates {@link TypeInformation} for the given {@link Type}.
	 * 
	 * @param fieldType
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	protected TypeInformation<?> createInfo(Type fieldType) {

		if (fieldType.equals(this.type)) {
			return this;
		}

		if (fieldType instanceof Class) {
			return new ClassTypeInformation((Class<?>) fieldType);
		}

		Class<S> resolveType = resolveType(fieldType);
		Map<TypeVariable, Type> variableMap = new HashMap<TypeVariable, Type>();
		variableMap.putAll(GenericTypeResolver.getTypeVariableMap(resolveType));

		if (fieldType instanceof ParameterizedType) {

			ParameterizedType parameterizedType = (ParameterizedType) fieldType;

			TypeVariable<Class<S>>[] typeParameters = resolveType.getTypeParameters();
			Type[] arguments = parameterizedType.getActualTypeArguments();

			for (int i = 0; i < typeParameters.length; i++) {
				variableMap.put(typeParameters[i], arguments[i]);
			}

			return new ParameterizedTypeInformation(parameterizedType, this, variableMap);
		}

		if (fieldType instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) fieldType;
			return new TypeVariableTypeInformation(variable, type, this, variableMap);
		}

		if (fieldType instanceof GenericArrayType) {
			return new GenericArrayTypeInformation((GenericArrayType) fieldType, this, variableMap);
		}

		if (fieldType instanceof WildcardType) {

			WildcardType wildcardType = (WildcardType) fieldType;
			Type[] bounds = wildcardType.getLowerBounds();

			if (bounds.length > 0) {
				return createInfo(bounds[0]);
			}

			bounds = wildcardType.getUpperBounds();

			if (bounds.length > 0) {
				return createInfo(bounds[0]);
			}
		}

		throw new IllegalArgumentException();
	}

	/**
	 * Resolves the given type into a plain {@link Class}.
	 * 
	 * @param type
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	protected Class<S> resolveType(Type type) {

		Map<TypeVariable, Type> map = new HashMap<TypeVariable, Type>();
		map.putAll(getTypeVariableMap());

		return (Class<S>) GenericTypeResolver.resolveType(type, map);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getParameterTypes(java.lang.reflect.Constructor)
	 */
	public List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor) {

		Assert.notNull(constructor, "Constructor must not be null!");

		Type[] types = constructor.getGenericParameterTypes();
		List<TypeInformation<?>> result = new ArrayList<TypeInformation<?>>(types.length);

		for (Type parameterType : types) {
			result.add(createInfo(parameterType));
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getProperty(java.lang.String)
	 */
	public Optional<TypeInformation<?>> getProperty(String fieldname) {

		int separatorIndex = fieldname.indexOf('.');

		if (separatorIndex == -1) {
			return fieldTypes.computeIfAbsent(fieldname, it -> getPropertyInformation(it));
		}

		String head = fieldname.substring(0, separatorIndex);
		Optional<TypeInformation<?>> info = getProperty(head);

		return info.map(it -> it.getProperty(fieldname.substring(separatorIndex + 1))).orElseGet(() -> Optional.empty());
	}

	/**
	 * Returns the {@link TypeInformation} for the given atomic field. Will inspect fields first and return the type of a
	 * field if available. Otherwise it will fall back to a {@link PropertyDescriptor}.
	 * 
	 * @see #getGenericType(PropertyDescriptor)
	 * @param fieldname
	 * @return
	 */
	private Optional<TypeInformation<?>> getPropertyInformation(String fieldname) {

		Class<?> rawType = getType();
		Field field = ReflectionUtils.findField(rawType, fieldname);

		if (field != null) {
			return Optional.of(createInfo(field.getGenericType()));
		}

		return findPropertyDescriptor(rawType, fieldname).map(it -> createInfo(getGenericType(it)));

	}

	/**
	 * Finds the {@link PropertyDescriptor} for the property with the given name on the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @param fieldname must not be {@literal null} or empty.
	 * @return
	 */
	private static Optional<PropertyDescriptor> findPropertyDescriptor(Class<?> type, String fieldname) {

		PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(type, fieldname);

		if (descriptor != null) {
			return Optional.of(descriptor);
		}

		List<Class<?>> superTypes = new ArrayList<Class<?>>();
		superTypes.addAll(Arrays.asList(type.getInterfaces()));
		superTypes.add(type.getSuperclass());

		return Streamable.of(type.getInterfaces()).stream()//
				.flatMap(it -> Optionals.toStream(findPropertyDescriptor(it, fieldname)))//
				.findFirst();
	}

	/**
	 * Returns the generic type for the given {@link PropertyDescriptor}. Will inspect its read method followed by the
	 * first parameter of the write method.
	 * 
	 * @param descriptor must not be {@literal null}
	 * @return
	 */
	private static Optional<Type> getGenericType(PropertyDescriptor descriptor) {

		Method method = descriptor.getReadMethod();

		if (method != null) {
			return Optional.of(method.getGenericReturnType());
		}

		method = descriptor.getWriteMethod();

		if (method == null) {
			return Optional.empty();
		}

		Type[] parameterTypes = method.getGenericParameterTypes();
		return Optional.ofNullable(parameterTypes.length == 0 ? null : parameterTypes[0]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getType()
	 */
	public Class<S> getType() {
		return resolvedType.get();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getRawTypeInformation()
	 */
	@Override
	public ClassTypeInformation<?> getRawTypeInformation() {
		return ClassTypeInformation.from(getType()).getRawTypeInformation();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getActualType()
	 */
	public TypeInformation<?> getActualType() {

		if (isMap()) {
			return getMapValueType().orElse(null);
		}

		if (isCollectionLike()) {
			return getComponentType().orElse(null);
		}

		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#isMap()
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getMapValueType()
	 */
	public Optional<TypeInformation<?>> getMapValueType() {
		return valueType.get();
	}

	protected Optional<TypeInformation<?>> doGetMapValueType() {
		return isMap() ? getTypeArgument(Map.class, 1) : getTypeArguments().stream().skip(1).findFirst();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#isCollectionLike()
	 */
	public boolean isCollectionLike() {

		Class<?> rawType = getType();

		return rawType.isArray() || Iterable.class.equals(rawType) || Collection.class.isAssignableFrom(rawType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getComponentType()
	 */
	public final Optional<TypeInformation<?>> getComponentType() {
		return componentType.get();
	}

	protected Optional<TypeInformation<?>> doGetComponentType() {

		Class<S> rawType = getType();

		if (rawType.isArray()) {
			return Optional.of(createInfo(rawType.getComponentType()));
		}

		if (isMap()) {
			return getTypeArgument(Map.class, 0);
		}

		if (Iterable.class.isAssignableFrom(rawType)) {
			return getTypeArgument(Iterable.class, 0);
		}

		List<TypeInformation<?>> arguments = getTypeArguments();

		return arguments.size() > 0 ? Optional.of(arguments.get(0)) : Optional.empty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getReturnType(java.lang.reflect.Method)
	 */
	public TypeInformation<?> getReturnType(Method method) {

		Assert.notNull(method);
		return createInfo(method.getGenericReturnType());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getMethodParameterTypes(java.lang.reflect.Method)
	 */
	public List<TypeInformation<?>> getParameterTypes(Method method) {

		Assert.notNull(method, "Method most not be null!");

		return Streamable.of(method.getGenericParameterTypes()).stream()//
				.map(it -> createInfo(it))//
				.collect(Collectors.toList());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getSuperTypeInformation(java.lang.Class)
	 */
	public TypeInformation<?> getSuperTypeInformation(Class<?> superType) {

		Class<?> rawType = getType();

		if (!superType.isAssignableFrom(rawType)) {
			return null;
		}

		if (getType().equals(superType)) {
			return this;
		}

		List<Type> candidates = new ArrayList<>();

		Type genericSuperclass = rawType.getGenericSuperclass();
		if (genericSuperclass != null) {
			candidates.add(genericSuperclass);
		}
		candidates.addAll(Arrays.asList(rawType.getGenericInterfaces()));

		for (Type candidate : candidates) {

			TypeInformation<?> candidateInfo = createInfo(candidate);

			if (superType.equals(candidateInfo.getType())) {
				return candidateInfo;
			} else {
				TypeInformation<?> nestedSuperType = candidateInfo.getSuperTypeInformation(superType);
				if (nestedSuperType != null) {
					return nestedSuperType;
				}
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getTypeParameters()
	 */
	public List<TypeInformation<?>> getTypeArguments() {
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#isAssignableFrom(org.springframework.data.util.TypeInformation)
	 */
	public boolean isAssignableFrom(TypeInformation<?> target) {
		return target.getSuperTypeInformation(getType()).equals(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#specialize(org.springframework.data.util.ClassTypeInformation)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TypeInformation<? extends S> specialize(ClassTypeInformation<?> type) {

		Assert.isTrue(getType().isAssignableFrom(type.getType()));

		List<TypeInformation<?>> arguments = getTypeArguments();

		return (TypeInformation<? extends S>) (arguments.isEmpty() ? type
				: createInfo(new SyntheticParamterizedType(type, arguments)));
	}

	private Optional<TypeInformation<?>> getTypeArgument(Class<?> bound, int index) {

		Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(getType(), bound);

		if (arguments == null) {
			return Optional.ofNullable(
					getSuperTypeInformation(bound) instanceof ParameterizedTypeInformation ? ClassTypeInformation.OBJECT : null);
		}

		return Optional.of(createInfo(arguments[index]));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		TypeDiscoverer<?> that = (TypeDiscoverer<?>) obj;

		return this.type.equals(that.type) && this.typeVariableMap.equals(that.typeVariableMap);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * A synthetic {@link ParameterizedType}.
	 *
	 * @author Oliver Gierke
	 * @since 1.11
	 */
	@EqualsAndHashCode
	@RequiredArgsConstructor
	private static class SyntheticParamterizedType implements ParameterizedType {

		private final @NonNull ClassTypeInformation<?> typeInformation;
		private final @NonNull List<TypeInformation<?>> typeParameters;

		/*
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getRawType()
		 */
		@Override
		public Type getRawType() {
			return typeInformation.getType();
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getOwnerType()
		 */
		@Override
		public Type getOwnerType() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getActualTypeArguments()
		 */
		@Override
		public Type[] getActualTypeArguments() {

			Type[] result = new Type[typeParameters.size()];

			for (int i = 0; i < typeParameters.size(); i++) {
				result[i] = typeParameters.get(i).getType();
			}

			return result;
		}
	}
}
