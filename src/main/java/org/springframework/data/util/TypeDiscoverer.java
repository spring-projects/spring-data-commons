/*
 * Copyright 2011-2022 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
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
 * @author Jürgen Diez
 * @author Alessandro Nistico
 * @author Johannes Englmeier
 */
public class TypeDiscoverer<S> implements TypeInformation<S> {

	protected static final Class<?>[] MAP_TYPES;
	private static final Class<?>[] COLLECTION_TYPES;

	static {

		var classLoader = TypeDiscoverer.class.getClassLoader();

		Set<Class<?>> mapTypes = new HashSet<>();
		mapTypes.add(Map.class);

		try {
			mapTypes.add(ClassUtils.forName("io.vavr.collection.Map", classLoader));
		} catch (ClassNotFoundException o_O) {}

		MAP_TYPES = mapTypes.toArray(new Class[0]);

		Set<Class<?>> collectionTypes = new HashSet<>();
		collectionTypes.add(Collection.class);

		try {
			collectionTypes.add(ClassUtils.forName("io.vavr.collection.Seq", classLoader));
		} catch (ClassNotFoundException o_O) {}

		try {
			collectionTypes.add(ClassUtils.forName("io.vavr.collection.Set", classLoader));
		} catch (ClassNotFoundException o_O) {}

		COLLECTION_TYPES = collectionTypes.toArray(new Class[0]);
	}

	ResolvableType resolvableType;
	private Map<String, Optional<TypeInformation<?>>> fields = new ConcurrentHashMap<>();

	private final Lazy<TypeInformation<?>> componentType;
	private final Lazy<TypeInformation<?>> valueType;

	public TypeDiscoverer(Class<?> type) {
		this(ResolvableType.forClass(type));
	}

	public TypeDiscoverer(ResolvableType type) {

		Assert.notNull(type, "Type must not be null");
		this.resolvableType = type;
		this.componentType = Lazy.of(this::doGetComponentType);
		this.valueType = Lazy.of(this::doGetMapValueType);
	}

	@Override
	public List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor) {

		Assert.notNull(constructor, "Constructor must not be null");

		List<TypeInformation<?>> target = new ArrayList<>();
		for (int i = 0; i < constructor.getParameterCount(); i++) {
			target.add(new TypeDiscoverer<>(ResolvableType.forConstructorParameter(constructor, i)));
		}
		return target;
	}

	@Override
	public TypeDescriptor toTypeDescriptor() {
		return new TypeDescriptor(resolvableType, null, null);
	}

	@Nullable
	@Override
	public TypeInformation<?> getProperty(String name) {

		var separatorIndex = name.indexOf('.');

		if (separatorIndex == -1) {
			return fields.computeIfAbsent(name, this::getPropertyInformation).orElse(null);
		}

		var head = name.substring(0, separatorIndex);
		var info = getProperty(head);

		if (info == null) {
			return null;
		}

		return info.getProperty(name.substring(separatorIndex + 1));
	}

	private Optional<TypeInformation<?>> getPropertyInformation(String fieldname) {

		Class<?> rawType = resolvableType.toClass();
		var field = ReflectionUtils.findField(rawType, fieldname);

		if (field != null) {
			return Optional.of(new TypeDiscoverer(ResolvableType.forField(field, resolvableType)));
		}

		return findPropertyDescriptor(rawType, fieldname).map(it -> {

			if (it.getReadMethod() != null) {
				return new TypeDiscoverer(ResolvableType.forMethodReturnType(it.getReadMethod(), rawType));
			}
			if (it.getWriteMethod() != null) {
				return new TypeDiscoverer(ResolvableType.forMethodParameter(it.getWriteMethod(), 0, rawType));
			}

			return new TypeDiscoverer(ResolvableType.forType(it.getPropertyType(), resolvableType));
		});
	}

	private Optional<PropertyDescriptor> findPropertyDescriptor(Class<?> type, String fieldname) {

		PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(type, fieldname);

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

	@Override
	public boolean isCollectionLike() {

		Class<S> type = getType();

		for (Class<?> collectionType : COLLECTION_TYPES) {
			if (collectionType.isAssignableFrom(type)) {
				return true;
			}
		}

		return type.isArray() //
				|| Iterable.class.equals(type) //
				|| Collection.class.isAssignableFrom(type) //
				|| Streamable.class.isAssignableFrom(type)
				|| CustomCollections.isCollection(type);
	}

	@Nullable
	@Override
	public TypeInformation<?> getComponentType() {
		return componentType.orElse(null);
	}

	@Nullable
	protected TypeInformation<?> doGetComponentType() {

		var rawType = getType();

		if (rawType.isArray()) {
			return new TypeDiscoverer<>(resolvableType.getComponentType());
		}

		if (isMap()) {
			if (ClassUtils.isAssignable(Map.class, rawType)) {
				ResolvableType mapValueType = resolvableType.asMap().getGeneric(0);
				if (ResolvableType.NONE.equals(mapValueType)) {
					return null;
				}

				return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
			}
			if (resolvableType.hasGenerics()) {
				ResolvableType mapValueType = resolvableType.getGeneric(0);
				return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
			}
			return Arrays.stream(resolvableType.getInterfaces()).filter(ResolvableType::hasGenerics)
					.findFirst()
					.map(it -> it.getGeneric(0))
					.map(TypeDiscoverer::new)
					.orElse(null);
		}

		if (Iterable.class.isAssignableFrom(rawType)) {

			ResolvableType iterableType = resolvableType.as(Iterable.class);
			ResolvableType mapValueType = iterableType.getGeneric(0);
			if (ResolvableType.NONE.equals(mapValueType)) {
				return null;
			}

			if (resolvableType.hasGenerics()) {
				mapValueType = resolvableType.getGeneric(0);
				return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
			}

			return mapValueType.resolve() != null ? new TypeDiscoverer<>(mapValueType) : null;
		}

		if (isNullableWrapper()) {
			ResolvableType mapValueType = resolvableType.getGeneric(0);
			if (ResolvableType.NONE.equals(mapValueType)) {
				return null;
			}
			return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
		}

		if (resolvableType.hasGenerics()) {
			ResolvableType mapValueType = resolvableType.getGeneric(0);
			return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
		}

		return null;
	}

	private boolean isNullableWrapper() {
		return NullableWrapperConverters.supports(getType());
	}

	@Override
	public boolean isMap() {
		return CustomCollections.isMap(getType());
	}

	@Nullable
	@Override
	public TypeInformation<?> getMapValueType() {
		return valueType.orElse(null);
	}

	@Nullable
	protected TypeInformation<?> doGetMapValueType() {

		if (isMap()) {
			if (ClassUtils.isAssignable(Map.class, getType())) {
				ResolvableType mapValueType = resolvableType.asMap().getGeneric(1);
				if (ResolvableType.NONE.equals(mapValueType)) {
					return null;
				}

				return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
			}
			if (resolvableType.hasGenerics()) {
				ResolvableType mapValueType = resolvableType.getGeneric(1);
				return mapValueType != null ? new TypeDiscoverer(mapValueType) : new ClassTypeInformation<>(Object.class);
			}
			return Arrays.stream(resolvableType.getInterfaces()).filter(ResolvableType::hasGenerics)
					.findFirst()
					.map(it -> it.getGeneric(1))
					.map(TypeDiscoverer::new)
					.orElse(null);
		}

		if (!resolvableType.hasGenerics()) {
			return null;
		}
		ResolvableType x = Arrays.stream(resolvableType.getGenerics()).skip(1).findFirst().orElse(null);
		if ((x == null) || ResolvableType.NONE.equals(x)) {
			return null;
		}

		return new TypeDiscoverer<>(x);
	}

	@Override
	public Class<S> getType() {
		return (Class<S>) resolvableType.toClass();
	}

	@Override
	public ClassTypeInformation<?> getRawTypeInformation() {
		return new ClassTypeInformation<>(this.resolvableType.getRawClass());
	}

	@Nullable
	@Override
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

	@Override
	public TypeInformation<?> getReturnType(Method method) {

		Assert.notNull(method, "Method must not be null");

		return new TypeDiscoverer(ResolvableType.forMethodReturnType(method, getType()));
	}

	@Override
	public List<TypeInformation<?>> getParameterTypes(Method method) {

		Assert.notNull(method, "Method most not be null");

		return Streamable.of(method.getParameters()).stream().map(MethodParameter::forParameter)
				.map(it -> ResolvableType.forMethodParameter(it, resolvableType)).map(TypeDiscoverer::new)
				.collect(Collectors.toList());

	}

	@Nullable
	@Override
	public TypeInformation<?> getSuperTypeInformation(Class<?> superType) {

		Class<?> rawType = getType();

		if (!superType.isAssignableFrom(rawType)) {
			return null;
		}

		if (rawType.equals(superType)) {
			return this;
		}

		List<ResolvableType> candidates = new ArrayList<>();

		ResolvableType genericSuperclass = resolvableType.getSuperType();
		if ((genericSuperclass != null) && !genericSuperclass.equals(ResolvableType.NONE)) {
			candidates.add(genericSuperclass);
		}

		candidates.addAll(Arrays.asList(resolvableType.getInterfaces()));

		for (var candidate : candidates) {
			if (ObjectUtils.nullSafeEquals(superType, candidate.toClass())) {

				if (resolvableType.getType() instanceof Class) {

					if (ObjectUtils.isEmpty(((Class) resolvableType.getType()).getTypeParameters())) {
						Class<?>[] classes = candidate.resolveGenerics(null);

						if (!Arrays.stream(classes).filter(it -> it != null).findAny().isPresent()) {
							return new TypeDiscoverer<>(ResolvableType.forRawClass(superType));
						}
					}
				}
				return new TypeDiscoverer(ResolvableType.forClass(superType, getType()));
			} else {
				var sup = candidate.getSuperType();
				if ((sup != null) && !ResolvableType.NONE.equals(sup)) {
					if (sup.equals(resolvableType)) {
						return this;
					}
					return new TypeDiscoverer(sup);
				}
			}
		}

		return new TypeDiscoverer(resolvableType.as(superType));
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.util.TypeInformation#isAssignableFrom(org.springframework.data.util.TypeInformation)
	 */
	public boolean isAssignableFrom(TypeInformation<?> target) {

		TypeInformation<?> superTypeInformation = target.getSuperTypeInformation(getType());

		if (superTypeInformation == null) {
			return false;
		}
		if (superTypeInformation.equals(this)) {
			return true;
		}

		if (resolvableType.isAssignableFrom(target.getType())) {
			return true;
		}

		return false;
	}

	@Override
	public List<TypeInformation<?>> getTypeArguments() {

		if (!resolvableType.hasGenerics()) {
			return Collections.emptyList();
		}

		return Arrays.stream(resolvableType.getGenerics()).map(it -> {
			if ((it == null) || ResolvableType.NONE.equals(it)) {
				return null;
			}
			return new TypeDiscoverer<>(it);

		}).collect(Collectors.toList());
	}

	@Override
	public TypeInformation<? extends S> specialize(ClassTypeInformation<?> type) {
		// if(isAssignableFrom(type)) {
		// return new ClassTypeInformation(type.getType());
		// }
		// return new NewTypeDiscoverer(type.resolvableType.as(getType()));
		// if(type.resolvableType.isAssignableFrom(type.resolvableType)) {
		// return (TypeInformation<? extends S>) type;
		// }

		if (this.resolvableType.getGenerics().length == type.resolvableType.getGenerics().length) {
			return new TypeDiscoverer<>(
					ResolvableType.forClassWithGenerics(type.getType(), this.resolvableType.getGenerics()));
		}

		return new ClassTypeInformation(type.getType());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || !ClassUtils.isAssignable(getClass(), o.getClass())) {
			return false;
		}

		TypeDiscoverer<?> that = (TypeDiscoverer<?>) o;

		if (!ObjectUtils.nullSafeEquals(getType(), that.getType())) {
			return false;
		}

		List<? extends Class<?>> collect1 = Arrays.stream(resolvableType.getGenerics()).map(ResolvableType::toClass)
				.collect(Collectors.toList());
		List<? extends Class<?>> collect2 = Arrays.stream(that.resolvableType.getGenerics()).map(ResolvableType::toClass)
				.collect(Collectors.toList());

		if (!ObjectUtils.nullSafeEquals(collect1, collect2)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(resolvableType.toClass());
	}

	@Override
	public String toString() {
		return getType().getName();
	}
}
