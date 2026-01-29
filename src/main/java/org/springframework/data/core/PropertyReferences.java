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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;
import org.springframework.data.core.MemberDescriptor.FieldDescriptor;
import org.springframework.data.core.MemberDescriptor.MethodDescriptor;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utility class to read metadata and resolve {@link PropertyReference} instances.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class PropertyReferences {

	private static final Map<ClassLoader, Map<PropertyReference<?, ?>, ResolvedPropertyReference<?, ?>>> resolved = new WeakHashMap<>();

	private static final SerializableLambdaReader reader = new SerializableLambdaReader(PropertyReference.class,
			TypedPropertyPath.class, TypedPropertyPaths.class, PropertyReferences.class);

	/**
	 * Introspect {@link PropertyReference} and return an introspected {@link ResolvedPropertyReference} variant.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <P, T> PropertyReference<T, P> of(PropertyReference<T, P> lambda) {

		if (lambda instanceof ResolvedPropertyReferenceSupport<?, ?>) {
			return lambda;
		}

		Map<PropertyReference<?, ?>, ResolvedPropertyReference<?, ?>> cache;
		synchronized (resolved) {
			cache = resolved.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}

		return (PropertyReference<T, P>) cache.computeIfAbsent(lambda,
				o -> new ResolvedPropertyReference(o, read(lambda)));
	}

	/**
	 * Retrieve {@link PropertyMetadata} for a given {@link PropertyReference}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T, P> PropertyReference<T, P> of(PropertyReference<T, P> delegate,
			PropertyMetadata metadata) {

		if (KotlinDetector.isKotlinReflectPresent() && metadata instanceof KPropertyMetadata kmp) {
			return new ResolvedKPropertyReference(kmp.getProperty(), metadata);
		}

		return new ResolvedPropertyReference<>(delegate, metadata);
	}

	private static PropertyMetadata read(PropertyReference<?, ?> lambda) {

		MemberDescriptor reference = reader.read(lambda);

		if (KotlinDetector.isKotlinReflectPresent()
				&& reference instanceof MemberDescriptor.KotlinMemberDescriptor kProperty) {

			if (kProperty instanceof MemberDescriptor.KPropertyPathDescriptor) {
				throw new IllegalArgumentException("PropertyReference " + kProperty.getKotlinProperty().getName()
						+ " is a property path. Use a single property reference.");
			}

			return KPropertyMetadata.of(kProperty);
		}

		if (reference instanceof MethodDescriptor method) {
			return PropertyMetadata.ofMethod(method);
		}

		return PropertyMetadata.ofField((FieldDescriptor) reference);
	}

	/**
	 * Metadata describing a property reference including its owner type, property type, and name.
	 */
	static class PropertyMetadata {

		private final TypeInformation<?> owner;
		private final String property;
		private final TypeInformation<?> propertyType;

		PropertyMetadata(Class<?> owner, String property, ResolvableType propertyType) {
			this(TypeInformation.of(owner), property, TypeInformation.of(propertyType));
		}

		PropertyMetadata(TypeInformation<?> owner, String property, TypeInformation<?> propertyType) {
			this.owner = owner;
			this.property = property;
			this.propertyType = propertyType;
		}

		/**
		 * Create a new {@code PropertyMetadata} from a method.
		 */
		public static PropertyMetadata ofMethod(MethodDescriptor descriptor) {

			return resolveProperty(descriptor,
					() -> new IllegalArgumentException("Cannot find PropertyDescriptor from method '%s.%s()'"
							.formatted(descriptor.owner().getName(), descriptor.getMember().getName())));
		}

		/**
		 * Create a new {@code PropertyMetadata} from a field.
		 */
		public static PropertyMetadata ofField(FieldDescriptor field) {
			return new PropertyMetadata(field.owner(), field.getMember().getName(), field.getType());
		}

		/**
		 * Resolve {@code PropertyMetadata} from a method descriptor by introspecting bean metadata and return metadata if
		 * available otherwise throw an exception supplied by {@code exceptionSupplier}.
		 *
		 * @param method the method descriptor.
		 * @param exceptionSupplier supplier for exception to be thrown when property cannot be resolved.
		 * @return metadata for the resolved property.
		 * @see BeanUtils
		 */
		public static PropertyMetadata resolveProperty(MethodDescriptor method,
				Supplier<? extends RuntimeException> exceptionSupplier) {

			PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method.method());
			String methodName = method.method().getName();

			if (descriptor == null) {

				String propertyName = getPropertyName(methodName);
				TypeInformation<?> owner = TypeInformation.of(method.owner());
				TypeInformation<?> fallback = owner.getProperty(propertyName);

				if (fallback != null) {
					return new PropertyMetadata(owner, propertyName, fallback);
				}

				throw exceptionSupplier.get();
			}

			return new PropertyMetadata(method.owner(), descriptor.getName(), method.getType());
		}

		private static String getPropertyName(String methodName) {

			if (methodName.startsWith("is")) {
				return Introspector.decapitalize(methodName.substring(2));
			} else if (methodName.startsWith("get")) {
				return Introspector.decapitalize(methodName.substring(3));
			}

			return methodName;
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
	 * Kotlin-specific {@link PropertyMetadata} implementation.
	 */
	static class KPropertyMetadata extends PropertyMetadata {

		private final KProperty<?> property;

		KPropertyMetadata(Class<?> owner, KProperty<?> property, ResolvableType propertyType) {
			super(owner, property.getName(), propertyType);
			this.property = property;
		}

		/**
		 * Create a new {@code KPropertyMetadata}.
		 */
		public static KPropertyMetadata of(MemberDescriptor.KotlinMemberDescriptor descriptor) {
			return new KPropertyMetadata(descriptor.getOwner(), descriptor.getKotlinProperty(),
					descriptor.getType());
		}

		public KProperty<?> getProperty() {
			return property;
		}
	}

	/**
	 * A {@link PropertyReference} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static abstract class ResolvedPropertyReferenceSupport<T, P> implements PropertyReference<T, P> {

		private final PropertyMetadata metadata;
		private final String toString;

		ResolvedPropertyReferenceSupport(PropertyMetadata metadata) {
			this.metadata = metadata;
			this.toString = metadata.owner().getType().getSimpleName() + "." + getName();
		}

		@Override
		@SuppressWarnings("unchecked")
		public TypeInformation<T> getOwningType() {
			return (TypeInformation<T>) metadata.owner();
		}

		@Override
		public String getName() {
			return metadata.property();
		}

		@Override
		@SuppressWarnings("unchecked")
		public TypeInformation<P> getTypeInformation() {
			return (TypeInformation<P>) metadata.propertyType();
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
	 * A {@link PropertyReference} implementation that caches resolved metadata to avoid repeated introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class ResolvedPropertyReference<T, P> extends ResolvedPropertyReferenceSupport<T, P> {

		private final PropertyReference<T, P> function;

		ResolvedPropertyReference(PropertyReference<T, P> function, PropertyMetadata metadata) {
			super(metadata);
			this.function = function;
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

	}

	/**
	 * A Kotlin-based {@link PropertyReference} implementation that caches resolved metadata to avoid repeated
	 * introspection.
	 *
	 * @param <T> the owning type.
	 * @param <P> the property type.
	 */
	static class ResolvedKPropertyReference<T, P> extends ResolvedPropertyReferenceSupport<T, P> {

		private final KProperty<P> property;

		@SuppressWarnings("unchecked")
		ResolvedKPropertyReference(KPropertyMetadata metadata) {
			this((KProperty<P>) metadata.getProperty(), metadata);
		}

		ResolvedKPropertyReference(KProperty<P> property, PropertyMetadata metadata) {
			super(metadata);
			this.property = property;
		}

		@Override
		public @Nullable P get(T obj) {
			return property.call(obj);
		}


	}

}
