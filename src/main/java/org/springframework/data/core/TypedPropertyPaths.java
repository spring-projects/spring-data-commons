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

import kotlin.reflect.KProperty;

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
import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;
import org.springframework.data.core.MemberDescriptor.FieldDescriptor;
import org.springframework.data.core.MemberDescriptor.KPropertyReferenceDescriptor;
import org.springframework.data.core.MemberDescriptor.MethodDescriptor;
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
			TypedPropertyPath.class, TypedPropertyPaths.class);

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

		if (lambda instanceof ComposedPropertyPath<?, ?, ?> || lambda instanceof ResolvedTypedPropertyPathSupport<?, ?>) {
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
	public static <T, P> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> delegate, PropertyPathMetadata metadata) {

		if (KotlinDetector.isKotlinReflectPresent() && metadata instanceof KPropertyPathMetadata kmp) {
			return new ResolvedKPropertyPath(kmp.getProperty(), metadata);
		}

		return new ResolvedTypedPropertyPath<>(delegate, metadata);
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

		if (KotlinDetector.isKotlinReflectPresent() && reference instanceof KPropertyReferenceDescriptor kProperty) {
			return KPropertyPathMetadata.of(kProperty);
		}

		if (reference instanceof MethodDescriptor method) {
			return PropertyPathMetadata.ofMethod(method);
		}

		return PropertyPathMetadata.ofField((MemberDescriptor.MethodDescriptor.FieldDescriptor) reference);
	}

	/**
	 * Metadata describing a single property path segment including its owner type, property type, and name.
	 */
	static class PropertyPathMetadata {

		private final TypeInformation<?> owner;
		private final String property;
		private final TypeInformation<?> propertyType;

		PropertyPathMetadata(Class<?> owner, String property, ResolvableType propertyType) {
			this(TypeInformation.of(owner), property, TypeInformation.of(propertyType));
		}

		PropertyPathMetadata(TypeInformation<?> owner, String property, TypeInformation<?> propertyType) {
			this.owner = owner;
			this.property = property;
			this.propertyType = propertyType;
		}

		/**
		 * Create a new {@code PropertyPathMetadata} from a method.
		 */
		public static PropertyPathMetadata ofMethod(MethodDescriptor method) {

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

			return new PropertyPathMetadata(method.getOwner(), descriptor.getName(), method.getType());
		}

		private static String getPropertyName(String methodName) {

			if (methodName.startsWith("is")) {
				return Introspector.decapitalize(methodName.substring(2));
			} else if (methodName.startsWith("get")) {
				return Introspector.decapitalize(methodName.substring(3));
			}

			return methodName;
		}

		/**
		 * Create a new {@code PropertyPathMetadata} from a field.
		 */
		public static PropertyPathMetadata ofField(FieldDescriptor field) {
			return new PropertyPathMetadata(field.owner(), field.getMember().getName(), field.getType());
		}

		public TypeInformation<?> owner() {
			return owner;
		}

		public String property() {
			return property;
		}

		public TypeInformation<?> propertyType() {
			return propertyType;
		}

	}

	/**
	 * Kotlin-specific {@link PropertyPathMetadata} implementation.
	 */
	static class KPropertyPathMetadata extends PropertyPathMetadata {

		private final KProperty<?> property;

		KPropertyPathMetadata(Class<?> owner, KProperty<?> property, ResolvableType propertyType) {
			super(owner, property.getName(), propertyType);
			this.property = property;
		}

		/**
		 * Create a new {@code KPropertyPathMetadata}.
		 */
		public static KPropertyPathMetadata of(KPropertyReferenceDescriptor descriptor) {
			return new KPropertyPathMetadata(descriptor.getOwner(), descriptor.property(), descriptor.getType());
		}

		public KProperty<?> getProperty() {
			return property;
		}
	}

	/**
	 * A {@link TypedPropertyPath} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static abstract class ResolvedTypedPropertyPathSupport<T, P> implements TypedPropertyPath<T, P> {

		private final PropertyPathMetadata metadata;
		private final List<PropertyPath> list;
		private final String toString;

		ResolvedTypedPropertyPathSupport(PropertyPathMetadata metadata) {
			this.metadata = metadata;
			this.list = List.of(this);
			this.toString = metadata.owner().getType().getSimpleName() + "." + toDotPath();
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
		public boolean equals(@Nullable Object obj) {

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof PropertyPath that)) {
				return false;
			}

			return Objects.equals(this.toDotPath(), that.toDotPath())
					&& Objects.equals(this.getOwningType(), that.getOwningType());
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public String toString() {
			return toString;
		}

	}

	/**
	 * A {@link TypedPropertyPath} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class ResolvedTypedPropertyPath<T, P> extends ResolvedTypedPropertyPathSupport<T, P> {

		private final TypedPropertyPath<T, P> function;

		ResolvedTypedPropertyPath(TypedPropertyPath<T, P> function, PropertyPathMetadata metadata) {
			super(metadata);
			this.function = function;
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

	}

	/**
	 * A Kotlin-based {@link TypedPropertyPath} implementation that caches resolved metadata to avoid repeated
	 * introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class ResolvedKPropertyPath<T, P> extends ResolvedTypedPropertyPathSupport<T, P> {

		private final KProperty<P> property;

		ResolvedKPropertyPath(KPropertyPathMetadata metadata) {
			this((KProperty<P>) metadata.getProperty(), metadata);
		}

		ResolvedKPropertyPath(KProperty<P> property, PropertyPathMetadata metadata) {
			super(metadata);
			this.property = property;
		}

		@Override
		public @Nullable P get(T obj) {
			return property.call(obj);
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
	record ComposedPropertyPath<T, M, R>(TypedPropertyPath<T, M> base, TypedPropertyPath<M, R> next, String dotPath,
			String toStringRepresentation) implements TypedPropertyPath<T, R> {

		ComposedPropertyPath(TypedPropertyPath<T, M> first, TypedPropertyPath<M, R> second) {
			this(first, second, first.toDotPath() + "." + second.toDotPath(),
					first.getType().getSimpleName() + "." + first.toDotPath() + "." + second.toDotPath());
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
		public boolean equals(Object obj) {

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof PropertyPath that)) {
				return false;
			}

			return Objects.equals(this.toDotPath(), that.toDotPath())
					&& Objects.equals(this.getOwningType(), that.getOwningType());
		}

		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		@Override
		public String toString() {
			return toStringRepresentation;
		}

	}

}
