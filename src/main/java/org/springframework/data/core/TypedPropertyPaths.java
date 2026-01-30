/*
 * Copyright 2025-present the original author or authors.
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
import kotlin.reflect.KProperty1;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;
import org.springframework.data.core.MemberDescriptor.KPropertyPathDescriptor;
import org.springframework.data.core.MemberDescriptor.KPropertyReferenceDescriptor;
import org.springframework.data.core.MemberDescriptor.MethodDescriptor;
import org.springframework.data.core.PropertyReferences.PropertyMetadata;
import org.springframework.util.CompositeIterator;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utility class to read metadata and resolve {@link TypedPropertyPath} instances.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class TypedPropertyPaths {

	private static final Map<ClassLoader, Map<Serializable, ResolvedTypedPropertyPath<?, ?>>> resolved = new WeakHashMap<>();

	private static final SerializableLambdaReader reader = new SerializableLambdaReader(PropertyPath.class,
			PropertyReference.class, PropertyReferences.class, TypedPropertyPath.class, TypedPropertyPaths.class);

	/**
	 * Compose a {@link TypedPropertyPath} by appending {@code next}.
	 */
	public static <T, M, P> TypedPropertyPath<T, P> compose(PropertyReference<T, M> owner, PropertyReference<M, P> next) {
		return compose(of(owner), of(next));
	}

	/**
	 * Compose a {@link TypedPropertyPath} by appending {@code next}.
	 */
	public static <T, M, P> TypedPropertyPath<T, P> compose(TypedPropertyPath<T, M> owner, PropertyReference<M, P> next) {
		return compose(of(owner), of(next));
	}

	/**
	 * Compose a {@link TypedPropertyPath} by appending {@code next}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, M, P> TypedPropertyPath<T, P> compose(TypedPropertyPath<T, M> owner, TypedPropertyPath<M, P> next) {

		if (owner instanceof ForwardingPropertyPath<?, ?, ?> fwd) {

			List<PropertyPath> paths = fwd.stream().map(ForwardingPropertyPath::getSelf).collect(Collectors.toList());
			Collections.reverse(paths);

			ForwardingPropertyPath result = null;
			for (PropertyPath path : paths) {

				if (result == null) {
					result = new ForwardingPropertyPath((TypedPropertyPath) path, next);
				} else {
					result = new ForwardingPropertyPath((TypedPropertyPath) path, result);
				}
			}

			return result;
		}

		return new ForwardingPropertyPath<>(of(owner), next);
	}

	/**
	 * Introspect {@link PropertyReference} and return an introspected {@link ResolvedTypedPropertyPath} variant.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <P, T> TypedPropertyPath<T, P> of(PropertyReference<T, P> lambda) {

		if (lambda instanceof Resolved) {
			return (TypedPropertyPath) lambda;
		}

		Map<PropertyReference<?, ?>, TypedPropertyPath<?, ?>> cache;
		synchronized (resolved) {
			cache = (Map) resolved.computeIfAbsent(lambda.getClass().getClassLoader(),
					k -> new ConcurrentReferenceHashMap<>());
		}

		return (TypedPropertyPath) cache.computeIfAbsent(lambda, TypedPropertyPaths::doResolvePropertyReference);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T, P> TypedPropertyPath<?, ?> doResolvePropertyReference(PropertyReference<T, P> lambda) {

		if (lambda instanceof PropertyReferences.ResolvedPropertyReferenceSupport resolved) {
			return new PropertyReferenceWrapper<>(resolved);
		}

		PropertyMetadata metadata = read(lambda);

		if (KotlinDetector.isKotlinReflectPresent()) {
			if (metadata instanceof KPropertyPathMetadata kMetadata
					&& kMetadata.getProperty() instanceof KPropertyPath<?, ?> ref) {
				return KotlinDelegate.of(ref);
			}
		}

		return new ResolvedPropertyReference<>(lambda, metadata);
	}

	/**
	 * Introspect {@link TypedPropertyPath} and return an introspected {@link ResolvedTypedPropertyPath} variant.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <P, T> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> lambda) {

		if (lambda instanceof Resolved) {
			return lambda;
		}

		Map<TypedPropertyPath<?, ?>, TypedPropertyPath<?, ?>> cache;
		synchronized (resolved) {
			cache = (Map) resolved.computeIfAbsent(lambda.getClass().getClassLoader(),
					k -> new ConcurrentReferenceHashMap<>());
		}

		return (TypedPropertyPath) cache.computeIfAbsent(lambda,
				TypedPropertyPaths::doResolvePropertyPathReference);
	}

	private static <T, P> TypedPropertyPath<?, ?> doResolvePropertyPathReference(TypedPropertyPath<T, P> lambda) {

		PropertyMetadata metadata = read(lambda);

		if (KotlinDetector.isKotlinReflectPresent()) {
			if (metadata instanceof KPropertyPathMetadata kMetadata
					&& kMetadata.getProperty() instanceof KPropertyPath<?, ?> ref) {
				return KotlinDelegate.of(ref);
			}
		}

		return new ResolvedTypedPropertyPath<>(lambda, metadata);
	}

	private static PropertyMetadata read(Object lambda) {

		MemberDescriptor reference = reader.read(lambda);

		if (KotlinDetector.isKotlinReflectPresent()) {

			if (reference instanceof KPropertyReferenceDescriptor descriptor) {
				return KPropertyPathMetadata.of(descriptor);
			}

			if (reference instanceof KPropertyPathDescriptor descriptor) {
				return KPropertyPathMetadata.of(descriptor);
			}
		}

		if (reference instanceof MethodDescriptor method) {
			return PropertyMetadata.ofMethod(method);
		}

		return PropertyMetadata.ofField((MemberDescriptor.MethodDescriptor.FieldDescriptor) reference);
	}

	/**
	 * Kotlin-specific {@link PropertyMetadata} implementation supporting composed {@link KProperty property paths}.
	 */
	static class KPropertyPathMetadata extends PropertyMetadata {

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

		/**
		 * Create a new {@code KPropertyPathMetadata}.
		 */
		public static KPropertyPathMetadata of(KPropertyPathDescriptor descriptor) {
			return new KPropertyPathMetadata(descriptor.getOwner(), descriptor.property(), descriptor.getType());
		}

		public KProperty<?> getProperty() {
			return property;
		}
	}

	/**
	 * Delegate to handle property path composition of single-property and property-path KProperty1 references.
	 */
	static class KotlinDelegate {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public static <T, P> TypedPropertyPath<T, P> of(Object property) {

			if (property instanceof KPropertyPath paths) {
				return TypedPropertyPaths.compose(of(paths.getProperty()), of(paths.getLeaf()));
			}

			if (property instanceof KProperty1 kProperty) {

				Field javaField = ReflectJvmMapping.getJavaField(kProperty);
				Method getter = ReflectJvmMapping.getJavaGetter(kProperty);

				Class<?> owner = javaField != null ? javaField.getDeclaringClass()
						: Objects.requireNonNull(getter).getDeclaringClass();
				KPropertyPathMetadata metadata = TypedPropertyPaths.KPropertyPathMetadata
						.of(MemberDescriptor.KPropertyReferenceDescriptor.create(owner, kProperty));
				return new TypedPropertyPaths.ResolvedKPropertyPath(metadata);
			}

			throw new IllegalArgumentException("Property " + property + " is not a KProperty");
		}

	}

	/**
	 * Marker interface to indicate a resolved and processed property path.
	 */
	interface Resolved {

	}

	/**
	 * A {@link TypedPropertyPath} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static abstract class ResolvedTypedPropertyPathSupport<T, P> implements TypedPropertyPath<T, P>, Resolved {

		private final PropertyMetadata metadata;
		private final List<PropertyPath> list;
		private final String toString;

		ResolvedTypedPropertyPathSupport(PropertyMetadata metadata) {
			this.metadata = metadata;
			this.list = List.of(this);
			this.toString = metadata.owner().getType().getSimpleName() + "." + toDotPath();
		}

		@Override
		@SuppressWarnings("unchecked")
		public TypeInformation<T> getOwningType() {
			return (TypeInformation<T>) metadata.owner();
		}

		@Override
		public String getSegment() {
			return metadata.property();
		}

		@Override
		@SuppressWarnings("unchecked")
		public TypeInformation<P> getTypeInformation() {
			return (TypeInformation<P>) metadata.propertyType();
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
			return PropertyPathUtil.equals(this, obj);
		}

		@Override
		public int hashCode() {
			return PropertyPathUtil.hashCode(this);
		}

		@Override
		public String toString() {
			return toString;
		}

	}

	/**
	 * Wrapper for {@link PropertyReference}.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class PropertyReferenceWrapper<T, P> implements TypedPropertyPath<T, P>, Resolved {

		private final PropertyReference<T, P> property;
		private final List<PropertyPath> self;

		public PropertyReferenceWrapper(PropertyReference<T, P> property) {
			this.property = property;
			this.self = List.of(this);
		}

		@Override
		public @Nullable P get(T obj) {
			return property.get(obj);
		}

		@Override
		public @Nullable PropertyPath next() {
			return null;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public TypeInformation<T> getOwningType() {
			return property.getOwningType();
		}

		@Override
		public String getSegment() {
			return property.getName();
		}

		@Override
		@SuppressWarnings("unchecked")
		public TypeInformation<P> getTypeInformation() {
			return (TypeInformation<P>) property.getTypeInformation();
		}

		@Override
		public Iterator<PropertyPath> iterator() {
			return self.iterator();
		}

		@Override
		public Stream<PropertyPath> stream() {
			return self.stream();
		}

		@Override
		public List<PropertyPath> toList() {
			return self;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			return PropertyPathUtil.equals(this, obj);
		}

		@Override
		public int hashCode() {
			return PropertyPathUtil.hashCode(this);
		}

		@Override
		public String toString() {
			return property.toString();
		}

	}

	/**
	 * A {@link TypedPropertyPath} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class ResolvedPropertyReference<T, P> extends ResolvedTypedPropertyPathSupport<T, P> {

		private final PropertyReference<T, P> function;

		ResolvedPropertyReference(PropertyReference<T, P> function, PropertyMetadata metadata) {
			super(metadata);
			this.function = function;
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public @Nullable PropertyPath next() {
			return null;
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

		ResolvedTypedPropertyPath(TypedPropertyPath<T, P> function, PropertyMetadata metadata) {
			super(metadata);
			this.function = function;
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public @Nullable PropertyPath next() {
			return null;
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

		@SuppressWarnings("unchecked")
		ResolvedKPropertyPath(KPropertyPathMetadata metadata) {
			this((KProperty<P>) metadata.getProperty(), metadata);
		}

		ResolvedKPropertyPath(KProperty<P> property, PropertyMetadata metadata) {
			super(metadata);
			this.property = property;
		}

		@Override
		public @Nullable P get(T obj) {
			return property.call(obj);
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public @Nullable PropertyPath next() {
			return null;
		}
	}

	/**
	 * Forwarding implementation to compose a linked {@link TypedPropertyPath} graph.
	 *
	 * @param self
	 * @param nextSegment
	 * @param leaf cached leaf property.
	 * @param toStringRepresentation cached toString representation.
	 */
	record ForwardingPropertyPath<T, M, P>(TypedPropertyPath<T, M> self, TypedPropertyPath<M, P> nextSegment,
			PropertyPath leaf, String dotPath, String toStringRepresentation) implements TypedPropertyPath<T, P>, Resolved {

		public ForwardingPropertyPath(TypedPropertyPath<T, M> self, TypedPropertyPath<M, P> nextSegment) {
			this(self, nextSegment, nextSegment.getLeafProperty(), getDotPath(self, nextSegment),
					getToString(self, nextSegment));
		}

		private static String getToString(PropertyPath self, PropertyPath nextSegment) {
			return self.getOwningType().getType().getSimpleName() + "." + getDotPath(self, nextSegment);
		}

		private static String getDotPath(PropertyPath self, PropertyPath nextSegment) {
			return self.getSegment() + "." + nextSegment.toDotPath();
		}

		public static PropertyPath getSelf(PropertyPath path) {
			return path instanceof ForwardingPropertyPath<?, ?, ?> fwd ? fwd.self() : path;
		}

		@Override
		public @Nullable P get(T obj) {
			M intermediate = self.get(obj);
			return intermediate != null ? nextSegment.get(intermediate) : null;
		}

		@Override
		public TypeInformation<T> getOwningType() {
			return self.getOwningType();
		}

		@Override
		public String getSegment() {
			return self.getSegment();
		}

		@Override
		public PropertyPath getLeafProperty() {
			return leaf;
		}

		@Override
		public String toDotPath() {
			return self.getSegment() + "." + nextSegment.toDotPath();
		}

		@Override
		@SuppressWarnings("unchecked")
		public TypeInformation<P> getTypeInformation() {
			return (TypeInformation<P>) self.getTypeInformation();
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public PropertyPath next() {
			return nextSegment;
		}

		@Override
		public Iterator<PropertyPath> iterator() {

			CompositeIterator<PropertyPath> iterator = new CompositeIterator<>();
			iterator.add(List.of((PropertyPath) this).iterator());
			iterator.add(nextSegment.iterator());
			return iterator;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			return PropertyPathUtil.equals(this, o);
		}

		@Override
		public int hashCode() {
			return PropertyPathUtil.hashCode(this);
		}

		@Override
		public String toString() {
			return toStringRepresentation;
		}
	}

}
