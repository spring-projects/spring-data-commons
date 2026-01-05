/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;
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
class TypeDiscoverer<S> implements TypeInformation<S> {

	private static final ConcurrentLruCache<ResolvableType, TypeInformation<?>> CACHE = new ConcurrentLruCache<>(64,
			TypeDiscoverer::new);

	private final ResolvableType resolvableType;
	private final Map<String, Optional<TypeInformation<?>>> fields = new ConcurrentHashMap<>();
	private final Lazy<Boolean> isMap = Lazy.of(() -> CustomCollections.isMap(getType()));
	private final Lazy<Boolean> isCollectionLike = Lazy.of(() -> {

		Class<S> type = getType();

		return type.isArray() //
				|| Iterable.class.equals(type) //
				|| Collection.class.isAssignableFrom(type) //
				|| Streamable.class.isAssignableFrom(type) || CustomCollections.isCollection(type);
	});
	private final Lazy<TypeInformation<?>> componentType;
	private final Lazy<TypeInformation<?>> valueType;
	private final Map<Constructor<?>, List<TypeInformation<?>>> constructorParameters = new ConcurrentHashMap<>();
	private final Lazy<List<TypeInformation<?>>> typeArguments;

	private final Lazy<List<TypeInformation<?>>> resolvedGenerics;

	protected TypeDiscoverer(ResolvableType type) {

		Assert.notNull(type, "Type must not be null");

		this.resolvableType = type;
		this.componentType = Lazy.of(this::doGetComponentType);
		this.valueType = Lazy.of(this::doGetMapValueType);
		this.typeArguments = Lazy.of(this::doGetTypeArguments);
		this.resolvedGenerics = Lazy.of(() -> Arrays.stream(resolvableType.getGenerics()) //
				.map(TypeInformation::of) // use TypeInformation comparison to remove any attachments to variableResolver
																	// holding the type source
				.collect(Collectors.toList()));
	}

	static TypeDiscoverer<?> ofCached(ResolvableType type) {

		Assert.notNull(type, "Type must not be null");

		return (TypeDiscoverer<?>) CACHE.get(type);
	}

	@Override
	public List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor) {

		Assert.notNull(constructor, "Constructor must not be null");

		return constructorParameters.computeIfAbsent(constructor, it -> {

			List<TypeInformation<?>> target = new ArrayList<>();

			for (int i = 0; i < it.getParameterCount(); i++) {
				target.add(TypeInformation.of(ResolvableType.forConstructorParameter(it, i)));
			}

			return target;
		});
	}

	@Override
	public @Nullable TypeInformation<?> getProperty(String name) {

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

	@Override
	public boolean isCollectionLike() {

		Class<S> type = getType();

		return isCollectionLike.get();
	}

	@Override
	public @Nullable TypeInformation<?> getComponentType() {
		return componentType.orElse(null);
	}

	protected @Nullable TypeInformation<?> doGetComponentType() {

		if (resolvableType.isArray()) {
			return TypeInformation.of(resolvableType.getComponentType());
		}

		Class<S> rawType = getType();

		if (isMap()) {
			return getTypeArgument(CustomCollections.getMapBaseType(rawType), 0);
		}

		List<TypeInformation<?>> arguments = getTypeArguments();

		if (!arguments.isEmpty()) {
			return arguments.get(0);
		}

		if (Iterable.class.isAssignableFrom(rawType)) {
			return getTypeArgument(Iterable.class, 0);
		}

		if (isNullableWrapper()) {
			return getTypeArgument(rawType, 0);
		}

		return null;
	}

	@Override
	public boolean isMap() {
		return isMap.get();
	}

	@Override
	public @Nullable TypeInformation<?> getMapValueType() {
		return valueType.orElse(null);
	}

	protected @Nullable TypeInformation<?> doGetMapValueType() {

		return isMap() //
				? getTypeArgument(CustomCollections.getMapBaseType(getType()), 1)
				: getTypeArguments().stream().skip(1).findFirst().orElse(null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<S> getType() {
		return (Class<S>) resolvableType.toClass();
	}

	@Override
	public TypeDescriptor toTypeDescriptor() {
		return new TypeDescriptor(resolvableType, getType(), null);
	}

	@Override
	public ResolvableType toResolvableType() {
		return resolvableType;
	}

	@Override
	public TypeInformation<?> getRawTypeInformation() {
		return new ClassTypeInformation<>(ResolvableType.forRawClass(resolvableType.toClass()));
	}

	@Override
	public @Nullable TypeInformation<?> getActualType() {

		if (isMap()) {
			return getMapValueType();
		}

		if (isCollectionLike()) {
			return getComponentType();
		}

		if (isNullableWrapper()) {
			return getComponentType();
		}

		return this;
	}

	@Override
	public TypeInformation<?> getReturnType(Method method) {
		return TypeInformation.of(ResolvableType.forMethodReturnType(method, getType()));
	}

	@Override
	public List<TypeInformation<?>> getParameterTypes(Method method) {

		Assert.notNull(method, "Method most not be null");

		return Arrays.stream(method.getParameters()) //
				.map(MethodParameter::forParameter) //
				.map(it -> ResolvableType.forMethodParameter(it, resolvableType)) //
				.<TypeInformation<?>> map(TypeInformation::of) //
				.toList();

	}

	@Override
	public @Nullable TypeInformation<?> getSuperTypeInformation(Class<?> superType) {

		Class<?> rawType = getType();

		if (!superType.isAssignableFrom(rawType)) {
			return null;
		}

		if (rawType.equals(superType)) {
			return this;
		}

		var resolvableSuperType = resolvableType.as(superType);
		var type = resolvableType.getType();

		if (!(type instanceof Class) || !ObjectUtils.isEmpty(((Class<?>) type).getTypeParameters())) {
			return TypeInformation.of(resolvableSuperType);
		}

		var noGenericsResolvable = !Arrays.stream(resolvableSuperType.resolveGenerics()).filter(it -> it != null).findAny()
				.isPresent();

		return noGenericsResolvable ? new ClassTypeInformation<>(ResolvableType.forRawClass(superType))
				: TypeInformation.of(resolvableSuperType);
	}

	@Override
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
		return typeArguments.get();
	}

	private List<TypeInformation<?>> doGetTypeArguments() {

		if (!resolvableType.hasGenerics()) {
			return Collections.emptyList();
		}

		return Arrays.stream(resolvableType.getGenerics())
				.<TypeInformation<?>> map(it -> it.resolve(Object.class) == null ? null : TypeInformation.of(it)).toList();
	}

	@Override
	@SuppressWarnings("unchecked")
	public TypeInformation<? extends S> specialize(TypeInformation<?> type) {

		if (this.getTypeArguments().size() == type.getTypeArguments().size()) {
			return (TypeInformation<? extends S>) TypeInformation
					.of(ResolvableType.forClassWithGenerics(type.getType(), this.resolvableType.getGenerics()));
		}

		return TypeInformation.of((Class<S>) type.getType());
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if ((o == null) || !ObjectUtils.nullSafeEquals(getClass(), o.getClass())) {
			return false;
		}

		var that = (TypeDiscoverer<?>) o;

		if (!ObjectUtils.nullSafeEquals(getType(), that.getType())) {
			return false;
		}

		// in case types cannot be resolved resort to toString checking to avoid infinite recursion caused by raw types and
		// self-referencing generics
		if (that.resolvableType.hasUnresolvableGenerics() || this.resolvableType.hasUnresolvableGenerics()) {
			return ObjectUtils.nullSafeEquals(that.resolvableType.toString(), this.resolvableType.toString());
		}

		return ObjectUtils.nullSafeEquals(resolvedGenerics.get(), that.resolvedGenerics.get());
	}

	@Override
	public int hashCode() {

		int result = 31 * getClass().hashCode();
		result += 31 * getType().hashCode();

		return result;
	}

	@Override
	public String toString() {
		return resolvableType.toString();
	}

	@Nullable
	private TypeInformation<?> getTypeArgument(Class<?> bound, int index) {

		var superTypeInformation = getSuperTypeInformation(bound);

		if (superTypeInformation == null) {
			return null;
		}

		var arguments = superTypeInformation.getTypeArguments();

		if (arguments.isEmpty() || (index > (arguments.size() - 1))) {
			return null;
		}

		return arguments.get(index);
	}

	private Optional<TypeInformation<?>> getPropertyInformation(String fieldname) {

		var rawType = getType();
		var field = ReflectionUtils.findField(rawType, fieldname);

		return field != null ? Optional.of(TypeInformation.of(ResolvableType.forField(field, resolvableType)))
				: Optional.ofNullable(BeanUtils.getPropertyDescriptor(rawType, fieldname))
						.filter(it -> it.getName().equals(fieldname)).map(it -> from(it, rawType)).map(TypeInformation::of);
	}

	private ResolvableType from(PropertyDescriptor descriptor, Class<?> rawType) {

		var method = descriptor.getReadMethod();

		if (method != null) {
			return ResolvableType.forMethodReturnType(method, rawType);
		}

		method = descriptor.getWriteMethod();

		if (method != null) {
			return ResolvableType.forMethodParameter(method, 0, rawType);
		}

		return ResolvableType.forType(descriptor.getPropertyType(), resolvableType);
	}

	private boolean isNullableWrapper() {
		return NullableWrapperConverters.supports(getType());
	}
}
