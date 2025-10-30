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
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utility class to parse and resolve {@link TypedPropertyPath} instances.
 */
class TypedPropertyPaths {

	private static final Map<ClassLoader, Map<Object, PropertyPathInformation>> lambdas = new WeakHashMap<>();
	private static final Map<ClassLoader, Map<TypedPropertyPath<?, ?>, ResolvedTypedPropertyPath<?, ?>>> resolved = new WeakHashMap<>();

	private static final SamParser samParser = new SamParser(PropertyPath.class, TypedPropertyPath.class,
			TypedPropertyPaths.class);

	/**
	 * Retrieve {@link PropertyPathInformation} for a given {@link TypedPropertyPath}.
	 */
	public static PropertyPathInformation getPropertyPathInformation(TypedPropertyPath<?, ?> lambda) {

		Map<Object, PropertyPathInformation> cache;
		synchronized (lambdas) {
			cache = lambdas.computeIfAbsent(lambda.getClass().getClassLoader(), k -> new ConcurrentReferenceHashMap<>());
		}
		Map<Object, PropertyPathInformation> lambdaMap = cache;

		return lambdaMap.computeIfAbsent(lambda, o -> extractPath(lambda.getClass().getClassLoader(), lambda));
	}

	private static PropertyPathInformation extractPath(ClassLoader classLoader, TypedPropertyPath<?, ?> lambda) {

		SamParser.MemberReference reference = samParser.parse(classLoader, lambda);

		if (reference instanceof SamParser.MethodInformation method) {
			return PropertyPathInformation.ofMethod(method);
		}

		return PropertyPathInformation.ofField((SamParser.FieldInformation) reference);
	}

	/**
	 * Compose a {@link TypedPropertyPath} by appending {@code next}.
	 */
	public static <T, M, R> TypedPropertyPath<T, R> compose(TypedPropertyPath<T, M> owner, TypedPropertyPath<M, R> next) {
		return new ComposedPropertyPath<>(owner, next);
	}

	/**
	 * Resolve a {@link TypedPropertyPath} into a {@link ResolvedTypedPropertyPath}.
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
				o -> new ResolvedTypedPropertyPath(o, getPropertyPathInformation(lambda)));
	}

	/**
	 * Value object holding information about a property path segment.
	 *
	 * @param owner
	 * @param propertyType
	 * @param property
	 */
	record PropertyPathInformation(TypeInformation<?> owner, TypeInformation<?> propertyType, String property) {

		public static PropertyPathInformation ofMethod(SamParser.MethodInformation method) {

			PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method.method());
			String methodName = method.getMember().getName();

			if (descriptor == null) {
				String propertyName;

				if (methodName.startsWith("is")) {
					propertyName = Introspector.decapitalize(methodName.substring(2));
				} else if (methodName.startsWith("get")) {
					propertyName = Introspector.decapitalize(methodName.substring(3));
				} else {
					propertyName = methodName;
				}

				TypeInformation<?> owner = TypeInformation.of(method.owner());
				TypeInformation<?> fallback = owner.getProperty(propertyName);
				if (fallback != null) {
					return new PropertyPathInformation(owner, fallback,
								propertyName);
				}

				throw new IllegalArgumentException(
						"Cannot find PropertyDescriptor from method %s.%s".formatted(method.owner().getName(), methodName));
			}

			return new PropertyPathInformation(TypeInformation.of(method.getOwner()), TypeInformation.of(method.getType()),
					descriptor.getName());
		}

		public static PropertyPathInformation ofField(SamParser.FieldInformation field) {
			return new PropertyPathInformation(TypeInformation.of(field.owner()), TypeInformation.of(field.getType()),
					field.getMember().getName());
		}
	}


	static class ResolvedTypedPropertyPath<T, P> implements TypedPropertyPath<T, P> {

		private final TypedPropertyPath<T, P> function;
		private final PropertyPathInformation information;
		private final List<PropertyPath> list;

		ResolvedTypedPropertyPath(TypedPropertyPath<T, P> function, PropertyPathInformation information) {
			this.function = function;
			this.information = information;
			this.list = List.of(this);
		}

		@Override
		public @Nullable P get(T obj) {
			return function.get(obj);
		}

		@Override
		public TypeInformation<?> getOwningType() {
			return information.owner();
		}

		@Override
		public String getSegment() {
			return information.property();
		}

		@Override
		public TypeInformation<?> getTypeInformation() {
			return information.propertyType();
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
			var that = (ResolvedTypedPropertyPath) obj;
			return Objects.equals(this.function, that.function) && Objects.equals(this.information, that.information);
		}

		@Override
		public int hashCode() {
			return Objects.hash(function, information);
		}

		@Override
		public String toString() {
			return information.owner().getType().getSimpleName() + "." + toDotPath();
		}
	}



	record ComposedPropertyPath<T, M, R>(TypedPropertyPath<T, M> first, TypedPropertyPath<M, R> second,
			String dotPath) implements TypedPropertyPath<T, R> {

		ComposedPropertyPath(TypedPropertyPath<T, M> first, TypedPropertyPath<M, R> second) {
			this(first, second, first.toDotPath() + "." + second.toDotPath());
		}

		@Override
		public @Nullable R get(T obj) {
			M intermediate = first.get(obj);
			return intermediate != null ? second.get(intermediate) : null;
		}

		@Override
		public TypeInformation<?> getOwningType() {
			return first.getOwningType();
		}

		@Override
		public String getSegment() {
			return first().getSegment();
		}

		@Override
		public PropertyPath getLeafProperty() {
			return second.getLeafProperty();
		}

		@Override
		public TypeInformation<?> getTypeInformation() {
			return first.getTypeInformation();
		}

		@Override
		public PropertyPath next() {
			return second;
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
			return second.stream();
		}

		@Override
		public Iterator<PropertyPath> iterator() {
			return second.iterator();
		}

		@Override
		public String toString() {
			return getOwningType().getType().getSimpleName() + "." + toDotPath();
		}
	}
}
