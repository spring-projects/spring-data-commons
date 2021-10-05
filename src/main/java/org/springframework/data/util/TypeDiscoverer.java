/*
 * Copyright 2011-2021 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.core.GenericTypeResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Basic {@link TypeDiscoverer} that contains basic functionality to discover property types.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class TypeDiscoverer<S> implements TypeInformation<S> {

	private static final Class<?>[] MAP_TYPES;

	static {

		var classLoader = TypeDiscoverer.class.getClassLoader();

		Set<Class<?>> mapTypes = new HashSet<>();
		mapTypes.add(Map.class);

		try {
			mapTypes.add(ClassUtils.forName("io.vavr.collection.Map", classLoader));
		} catch (ClassNotFoundException o_O) {}

		MAP_TYPES = mapTypes.toArray(new Class[0]);
	}

	private final Type type;
	private final Map<TypeVariable<?>, Type> typeVariableMap;
	private final Map<String, Optional<TypeInformation<?>>> fieldTypes = new ConcurrentHashMap<>();
	private final int hashCode;

	private final Lazy<Class<S>> resolvedType;
	private final Lazy<TypeInformation<?>> componentType;
	private final Lazy<TypeInformation<?>> valueType;

	/**
	 * Creates a new {@link TypeDiscoverer} for the given type, type variable map and parent.
	 *
	 * @param type must not be {@literal null}.
	 * @param typeVariableMap must not be {@literal null}.
	 */
	protected TypeDiscoverer(Type type, Map<TypeVariable<?>, Type> typeVariableMap) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(typeVariableMap, "TypeVariableMap must not be null!");

		this.type = type;
		this.resolvedType = Lazy.of(() -> resolveType(type));
		this.componentType = Lazy.of(this::doGetComponentType);
		this.valueType = Lazy.of(this::doGetMapValueType);
		this.typeVariableMap = typeVariableMap;
		this.hashCode = 17 + 31 * type.hashCode() + 31 * typeVariableMap.hashCode();
	}

	/**
	 * Returns the type variable map.
	 *
	 * @return
	 */
	protected Map<TypeVariable<?>, Type> getTypeVariableMap() {
		return typeVariableMap;
	}

	/**
	 * Creates {@link TypeInformation} for the given {@link Type}.
	 *
	 * @param fieldType must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected TypeInformation<?> createInfo(Type fieldType) {

		Assert.notNull(fieldType, "Field type must not be null!");

		if (fieldType.equals(this.type)) {
			return this;
		}

		if (fieldType instanceof Class) {
			return ClassTypeInformation.from((Class<?>) fieldType);
		}

		if (fieldType instanceof ParameterizedType parameterizedType) {

			return new ParameterizedTypeInformation(parameterizedType, this);
		}

		if (fieldType instanceof TypeVariable<?> variable) {

			return new TypeVariableTypeInformation(variable, this);
		}

		if (fieldType instanceof GenericArrayType) {
			return new GenericArrayTypeInformation((GenericArrayType) fieldType, this);
		}

		if (fieldType instanceof WildcardType wildcardType) {

			var bounds = wildcardType.getLowerBounds();

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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Class<S> resolveType(Type type) {

		Map<TypeVariable, Type> map = new HashMap<>();
		map.putAll(getTypeVariableMap());

		return (Class<S>) GenericTypeResolver.resolveType(type, map);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getParameterTypes(java.lang.reflect.Constructor)
	 */
	public List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor) {

		Assert.notNull(constructor, "Constructor must not be null!");

		List<TypeInformation<?>> parameterTypes = new ArrayList<>(constructor.getParameterCount());
		for (var parameter : constructor.getParameters()) {
			parameterTypes.add(createInfo(parameter.getParameterizedType()));
		}
		return parameterTypes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getProperty(java.lang.String)
	 */
	@Nullable
	public TypeInformation<?> getProperty(String fieldname) {

		var separatorIndex = fieldname.indexOf('.');

		if (separatorIndex == -1) {
			return fieldTypes.computeIfAbsent(fieldname, this::getPropertyInformation).orElse(null);
		}

		var head = fieldname.substring(0, separatorIndex);
		var info = getProperty(head);

		if (info == null) {
			return null;
		}

		return info.getProperty(fieldname.substring(separatorIndex + 1));
	}

	/**
	 * Returns the {@link TypeInformation} for the given atomic field. Will inspect fields first and return the type of a
	 * field if available. Otherwise it will fall back to a {@link PropertyDescriptor}.
	 *
	 * @see #getGenericType(PropertyDescriptor)
	 * @param fieldname
	 * @return
	 */
	@SuppressWarnings("null")
	private Optional<TypeInformation<?>> getPropertyInformation(String fieldname) {

		Class<?> rawType = getType();
		var field = ReflectionUtils.findField(rawType, fieldname);

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

		var descriptor = BeanUtils.getPropertyDescriptor(type, fieldname);

		if (descriptor != null) {
			return Optional.of(descriptor);
		}

		List<Class<?>> superTypes = new ArrayList<>();
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
	@Nullable
	private static Type getGenericType(PropertyDescriptor descriptor) {

		var method = descriptor.getReadMethod();

		if (method != null) {
			return method.getGenericReturnType();
		}

		method = descriptor.getWriteMethod();

		if (method == null) {
			return null;
		}

		var parameterTypes = method.getGenericParameterTypes();
		return parameterTypes.length == 0 ? null : parameterTypes[0];
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
	@Nullable
	public TypeInformation<?> getActualType() {

		if (isMap()) {
			return getMapValueType();
		}

		if (isCollectionLike()) {
			return getComponentType();
		}

		// TODO: Consider that we will support value types beyond Optional<T>, such as Json<T>, Foo<T> that should remain
		// configurable.
		if (isNullableWrapper()) {
			return getComponentType();
		}

		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#isMap()
	 */
	public boolean isMap() {

		var type = getType();

		for (var mapType : MAP_TYPES) {
			if (mapType.isAssignableFrom(type)) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getMapValueType()
	 */
	@Nullable
	public TypeInformation<?> getMapValueType() {
		return valueType.orElse(null);
	}

	@Nullable
	protected TypeInformation<?> doGetMapValueType() {
		return isMap() ? getTypeArgument(getBaseType(MAP_TYPES), 1)
				: getTypeArguments().stream().skip(1).findFirst().orElse(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#isCollectionLike()
	 */
	public boolean isCollectionLike() {

		Class<?> rawType = getType();

		return rawType.isArray() //
				|| Iterable.class.equals(rawType) //
				|| Collection.class.isAssignableFrom(rawType) //
				|| Streamable.class.isAssignableFrom(rawType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getComponentType()
	 */
	@Nullable
	public final TypeInformation<?> getComponentType() {
		return componentType.orElse(null);
	}

	@Nullable
	protected TypeInformation<?> doGetComponentType() {

		var rawType = getType();

		if (rawType.isArray()) {
			return createInfo(rawType.getComponentType());
		}

		if (isMap()) {
			return getTypeArgument(getBaseType(MAP_TYPES), 0);
		}

		if (Iterable.class.isAssignableFrom(rawType)) {
			return getTypeArgument(Iterable.class, 0);
		}

		if(isNullableWrapper()) {
			return getTypeArgument(rawType, 0);
		}

		var arguments = getTypeArguments();

		return arguments.size() > 0 ? arguments.get(0) : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getReturnType(java.lang.reflect.Method)
	 */
	public TypeInformation<?> getReturnType(Method method) {

		Assert.notNull(method, "Method must not be null!");
		return createInfo(method.getGenericReturnType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getMethodParameterTypes(java.lang.reflect.Method)
	 */
	public List<TypeInformation<?>> getParameterTypes(Method method) {

		Assert.notNull(method, "Method most not be null!");

		return Streamable.of(method.getGenericParameterTypes()).stream()//
				.map(this::createInfo)//
				.collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#getSuperTypeInformation(java.lang.Class)
	 */
	@Nullable
	public TypeInformation<?> getSuperTypeInformation(Class<?> superType) {

		Class<?> rawType = getType();

		if (!superType.isAssignableFrom(rawType)) {
			return null;
		}

		if (getType().equals(superType)) {
			return this;
		}

		List<Type> candidates = new ArrayList<>();
		var genericSuperclass = rawType.getGenericSuperclass();

		if (genericSuperclass != null) {
			candidates.add(genericSuperclass);
		}

		candidates.addAll(Arrays.asList(rawType.getGenericInterfaces()));

		for (var candidate : candidates) {

			var candidateInfo = createInfo(candidate);

			if (superType.equals(candidateInfo.getType())) {
				return candidateInfo;
			} else {

				var nestedSuperType = candidateInfo.getSuperTypeInformation(superType);

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

		var superTypeInformation = target.getSuperTypeInformation(getType());

		return superTypeInformation == null ? false : superTypeInformation.equals(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#specialize(org.springframework.data.util.ClassTypeInformation)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TypeInformation<? extends S> specialize(ClassTypeInformation<?> type) {

		Assert.notNull(type, "Type must not be null!");
		Assert.isTrue(getType().isAssignableFrom(type.getType()),
				() -> String.format("%s must be assignable from %s", getType(), type.getType()));

		var typeArguments = getTypeArguments();

		return (TypeInformation<? extends S>) (typeArguments.isEmpty() //
				? type //
				: type.createInfo(new SyntheticParamterizedType(type, getTypeArguments())));
	}

	@Nullable
	private TypeInformation<?> getTypeArgument(Class<?> bound, int index) {

		var arguments = GenericTypeResolver.resolveTypeArguments(getType(), bound);

		if (arguments != null) {
			return createInfo(arguments[index]);
		}

		return getSuperTypeInformation(bound) instanceof ParameterizedTypeInformation //
				? ClassTypeInformation.OBJECT //
				: null;
	}

	private Class<?> getBaseType(Class<?>[] candidates) {

		var type = getType();

		for (var candidate : candidates) {
			if (candidate.isAssignableFrom(type)) {
				return candidate;
			}
		}

		throw new IllegalArgumentException(String.format("Type %s not contained in candidates %s!", type, candidates));
	}

	private boolean isNullableWrapper() {
		return NullableWrapperConverters.supports(getType());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object obj) {

		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		var that = (TypeDiscoverer<?>) obj;

		if (!this.type.equals(that.type)) {
			return false;
		}

		if (this.typeVariableMap.isEmpty() && that.typeVariableMap.isEmpty()) {
			return true;
		}

		return this.typeVariableMap.equals(that.typeVariableMap);
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
	private static class SyntheticParamterizedType implements ParameterizedType {

		private final ClassTypeInformation<?> typeInformation;
		private final List<TypeInformation<?>> typeParameters;

		public SyntheticParamterizedType(ClassTypeInformation<?> typeInformation, List<TypeInformation<?>> typeParameters) {
			this.typeInformation = typeInformation;
			this.typeParameters = typeParameters;
		}

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
		@Nullable
		public Type getOwnerType() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.reflect.ParameterizedType#getActualTypeArguments()
		 */
		@Override
		public Type[] getActualTypeArguments() {

			var result = new Type[typeParameters.size()];

			for (var i = 0; i < typeParameters.size(); i++) {
				result[i] = typeParameters.get(i).getType();
			}

			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof SyntheticParamterizedType that)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(typeInformation, that.typeInformation)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(typeParameters, that.typeParameters);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			var result = ObjectUtils.nullSafeHashCode(typeInformation);
			result = 31 * result + ObjectUtils.nullSafeHashCode(typeParameters);
			return result;
		}
	}
}
