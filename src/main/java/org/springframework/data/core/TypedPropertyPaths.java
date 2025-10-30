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
package org.springframework.data.core;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CompositeIterator;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utility class to read metadata and resolve {@link TypedPropertyPath} instances.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class TypedPropertyPaths {

	private static final Map<ClassLoader, Map<Object, PropertyPathMetadata>> lambdas = new WeakHashMap<>();
	private static final Map<ClassLoader, Map<TypedPropertyPath<?, ?>, ResolvedTypedPropertyPath<?, ?>>> resolved = new WeakHashMap<>();

	private static final SerializableLambdaReader reader = new SerializableLambdaReader(PropertyPath.class,
			TypedPropertyPath.class,
			TypedPropertyPaths.class);

	/**
	 * Compose a {@link TypedPropertyPath} by appending {@code next}.
	 */
	public static <T, M, R> TypedPropertyPath<T, R> compose(TypedPropertyPath<T, M> owner, TypedPropertyPath<M, R> next) {
		return new ComposedPropertyPath<>(owner, next);
	}

	/**
	 * Introspect {@link TypedPropertyPath} and return an introspected {@link ResolvedTypedPropertyPath} variant.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <P, T> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> lambda) {

		if (lambda instanceof ComposedPropertyPath<?, ?, ?> || lambda instanceof ResolvedTypedPropertyPath<?, ?>) {
			return lambda;
		}

		Map<TypedPropertyPath<?, ?>, ResolvedTypedPropertyPath<?, ?>> cache;
		synchronized (resolved) {
			cache = resolved.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}

		return (TypedPropertyPath<T, P>) cache.computeIfAbsent(lambda,
				o -> new ResolvedTypedPropertyPath(o, getMetadata(lambda)));
	}

	/**
	 * Retrieve {@link PropertyPathMetadata} for a given {@link TypedPropertyPath}.
	 */
	public static PropertyPathMetadata getMetadata(TypedPropertyPath<?, ?> lambda) {

		Map<Object, PropertyPathMetadata> cache;
		synchronized (lambdas) {
			cache = lambdas.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}
		Map<Object, PropertyPathMetadata> lambdaMap = cache;

		return lambdaMap.computeIfAbsent(lambda, o -> read(lambda));
	}

	private static PropertyPathMetadata read(TypedPropertyPath<?, ?> lambda) {

		MemberDescriptor reference = reader.read(lambda);

		if (reference instanceof MemberDescriptor.MethodDescriptor method) {
			return PropertyPathMetadata.ofMethod(method);
		}

		return PropertyPathMetadata.ofField((MemberDescriptor.MethodDescriptor.FieldDescriptor) reference);
	}

	/**
	 * Metadata describing a single property path segment including its owner type, property type, and name.
	 *
	 * @param owner the type that owns the property.
	 * @param property the property name.
	 * @param propertyType the type of the property.
	 */
	record PropertyPathMetadata(TypeInformation<?> owner, String property, TypeInformation<?> propertyType) {

		public static PropertyPathMetadata ofMethod(MemberDescriptor.MethodDescriptor method) {

			PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method.method());
			String methodName = method.getMember().getName();

			if (descriptor == null) {

				String propertyName = getPropertyName(methodName);
				TypeInformation<?> owner = TypeInformation.of(method.owner());
				TypeInformation<?> fallback = owner.getProperty(propertyName);

				if (fallback != null) {
					return new PropertyPathMetadata(owner, propertyName, fallback);
				}

				throw new IllegalArgumentException(
						"Cannot find PropertyDescriptor from method '%s.%s()'".formatted(method.owner().getName(), methodName));
			}

			return new PropertyPathMetadata(TypeInformation.of(method.getOwner()), descriptor.getName(),
					TypeInformation.of(method.getType()));
		}

		private static String getPropertyName(String methodName) {

			if (methodName.startsWith("is")) {
				return Introspector.decapitalize(methodName.substring(2));
			} else if (methodName.startsWith("get")) {
				return Introspector.decapitalize(methodName.substring(3));
			}

			return methodName;
		}

		public static PropertyPathMetadata ofField(MemberDescriptor.MethodDescriptor.FieldDescriptor field) {
			return new PropertyPathMetadata(TypeInformation.of(field.owner()), field.getMember().getName(),
					TypeInformation.of(field.getType()));
		}

	}

	/**
	 * A {@link TypedPropertyPath} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class ResolvedTypedPropertyPath<T, P> implements TypedPropertyPath<T, P> {

		private final TypedPropertyPath<T, P> function;
		private final PropertyPathMetadata metadata;
		private final List<PropertyPath> list;

		ResolvedTypedPropertyPath(TypedPropertyPath<T, P> function, PropertyPathMetadata metadata) {
			this.function = function;
			this.metadata = metadata;
			this.list = List.of(this);
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

		@Override
		public TypeInformation<?> getOwningType() {
			return metadata.owner();
		}

		@Override
		public String getSegment() {
			return metadata.property();
		}

		@Override
		public TypeInformation<?> getTypeInformation() {
			return metadata.propertyType();
		}

		@Override
		public Iterator<PropertyPath> iterator() {
			return list.iterator();
		}

		@Override
		public Stream<PropertyPath> stream() {
			return list.stream();
		}

		@Override
		public List<PropertyPath> toList() {
			return list;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			var that = (ResolvedTypedPropertyPath<?, ?>) obj;
			return Objects.equals(this.function, that.function) && Objects.equals(this.metadata, that.metadata);
		}

		@Override
		public int hashCode() {
			return Objects.hash(function, metadata);
		}

		@Override
		public String toString() {
			return metadata.owner().getType().getSimpleName() + "." + toDotPath();
		}

	}

	/**
	 * A {@link TypedPropertyPath} that represents the composition of two property paths, enabling navigation through
	 * nested properties.
	 *
	 * @param <T> the root owning type.
	 * @param <M> the intermediate property type (connecting first and second paths).
	 * @param <R> the final property type.
	 * @param base the initial path segment.
	 * @param next the next path segment.
	 * @param dotPath the precomputed dot-notation path string.
	 */
	record ComposedPropertyPath<T, M, R>(TypedPropertyPath<T, M> base, TypedPropertyPath<M, R> next,
			String dotPath) implements TypedPropertyPath<T, R> {

		ComposedPropertyPath(TypedPropertyPath<T, M> first, TypedPropertyPath<M, R> second) {
			this(first, second, first.toDotPath() + "." + second.toDotPath());
		}

		@Override
		public @Nullable R get(T obj) {
			M intermediate = base.get(obj);
			return intermediate != null ? next.get(intermediate) : null;
		}

		@Override
		public TypeInformation<?> getOwningType() {
			return base.getOwningType();
		}

		@Override
		public String getSegment() {
			return base().getSegment();
		}

		@Override
		public PropertyPath getLeafProperty() {
			return next.getLeafProperty();
		}

		@Override
		public TypeInformation<?> getTypeInformation() {
			return base.getTypeInformation();
		}

		@Override
		public TypedPropertyPath<M, R> next() {
			return next;
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public String toDotPath() {
			return dotPath;
		}

		@Override
		public Stream<PropertyPath> stream() {
			return Stream.concat(base.stream(), next.stream());
		}

		@Override
		public Iterator<PropertyPath> iterator() {
			CompositeIterator<PropertyPath> iterator = new CompositeIterator<>();
			iterator.add(base.iterator());
			iterator.add(next.iterator());
			return iterator;
		}

		@Override
		public String toString() {
			return getOwningType().getType().getSimpleName() + "." + toDotPath();
		}

	}

}
