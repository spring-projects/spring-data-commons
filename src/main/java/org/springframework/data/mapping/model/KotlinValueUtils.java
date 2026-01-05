/*
 * Copyright 2023-present the original author or authors.
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
package org.springframework.data.mapping.model;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KClassifier;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty;
import kotlin.reflect.KType;
import kotlin.reflect.KTypeParameter;
import kotlin.reflect.KTypeProjection;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Utilities for Kotlin Value class support.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.2
 */
class KotlinValueUtils {

	/**
	 * Creates a value hierarchy across value types from a given {@link KParameter} for COPY method usage.
	 *
	 * @param parameter the parameter that references the value class hierarchy.
	 * @return
	 */
	public static ValueBoxing getCopyValueHierarchy(KParameter parameter) {
		return new ValueBoxing(BoxingRules.COPY, parameter);
	}

	/**
	 * Creates a value hierarchy across value types from a given {@link KParameter} for constructor usage.
	 *
	 * @param parameter the parameter that references the value class hierarchy.
	 * @return the {@link ValueBoxing} type hierarchy.
	 */
	public static ValueBoxing getConstructorValueHierarchy(KParameter parameter) {
		return new ValueBoxing(BoxingRules.CONSTRUCTOR, parameter);
	}

	/**
	 * Creates a value hierarchy across value types from a given {@link KParameter} for constructor usage.
	 *
	 * @param cls the entrypoint of the type hierarchy.
	 * @return the {@link ValueBoxing} type hierarchy.
	 */
	public static ValueBoxing getConstructorValueHierarchy(Class<?> cls) {

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(cls);
		return new ValueBoxing(BoxingRules.CONSTRUCTOR, typeOf(kotlinClass), kotlinClass, false);
	}

	/**
	 * Get the {@link KType} for a given {@link KClass} and potentially fill missing generic type arguments with
	 * {@link KTypeProjection#star} to prevent Kotlin internal checks to fail.
	 */
	private static KType typeOf(KClass<?> kotlinClass) {

		return kotlinClass.getTypeParameters().isEmpty() ? Reflection.typeOf(kotlinClass)
				: Reflection.typeOf(JvmClassMappingKt.getJavaClass(kotlinClass), stubKTypeProjections(kotlinClass));
	}

	private static KTypeProjection[] stubKTypeProjections(KClass<?> kotlinClass) {

		KTypeProjection[] kTypeProjections = new KTypeProjection[kotlinClass.getTypeParameters().size()];
		Arrays.fill(kTypeProjections, KTypeProjection.star);
		return kTypeProjections;
	}

	/**
	 * Boxing rules for value class wrappers.
	 */
	enum BoxingRules {

		/**
		 * When used in the constructor. Constructor boxing depends on nullability of the declared property, whether the
		 * component uses defaulting, nullability of the value component and whether the component is a primitive.
		 */
		CONSTRUCTOR {
			@Override
			public boolean shouldApplyBoxing(KType type, boolean optional, KParameter component) {

				Type javaType = ReflectJvmMapping.getJavaType(component.getType());

				if (type.isMarkedNullable() || optional) {

					boolean isPrimitive = javaType instanceof Class<?> c && c.isPrimitive();
					return (isPrimitive && type.isMarkedNullable()) || component.getType().isMarkedNullable();
				}

				return false;
			}
		},

		/**
		 * When used in a copy method. Copy method boxing depends on nullability of the declared property, nullability of
		 * the value component and whether the component is a primitive.
		 */
		COPY {
			@Override
			public boolean shouldApplyBoxing(KType type, boolean optional, KParameter component) {

				KType copyType = expandUnderlyingType(type);

				if (copyType.getClassifier() instanceof KClass<?> kc && kc.isValue() || copyType.isMarkedNullable()) {
					return true;
				}

				return false;
			}

			private static KType expandUnderlyingType(KType kotlinType) {

				if (!(kotlinType.getClassifier() instanceof KClass<?> kc) || !kc.isValue()) {
					return kotlinType;
				}

				List<KProperty<?>> properties = getProperties(kc);
				if (properties.isEmpty()) {
					return kotlinType;
				}

				KType underlyingType = properties.get(0).getReturnType();
				KType componentType = ValueBoxing.resolveType(underlyingType);
				KType expandedUnderlyingType = expandUnderlyingType(componentType);

				if (!kotlinType.isMarkedNullable()) {
					return expandedUnderlyingType;
				}

				if (expandedUnderlyingType.isMarkedNullable()) {
					return kotlinType;
				}

				Type javaType = ReflectJvmMapping.getJavaType(expandedUnderlyingType);
				boolean isPrimitive = javaType instanceof Class<?> c && c.isPrimitive();

				if (isPrimitive) {
					return kotlinType;
				}

				return expandedUnderlyingType;
			}

			static List<KProperty<?>> getProperties(KClass<?> kClass) {

				if (kClass.isValue()) {

					for (KCallable<?> member : kClass.getMembers()) {
						if (member instanceof KProperty<?> kp) {
							return Collections.singletonList(kp);
						}
					}
				}

				List<KProperty<?>> properties = new ArrayList<>();
				for (KCallable<?> member : kClass.getMembers()) {
					if (member instanceof KProperty<?> kp) {
						properties.add(kp);
					}
				}

				return properties;
			}
		};

		public abstract boolean shouldApplyBoxing(KType type, boolean optional, KParameter component);

	}

	/**
	 * Utility to represent Kotlin value class boxing.
	 */
	static class ValueBoxing {

		private final KClass<?> kClass;

		private final @Nullable KFunction<?> wrapperConstructor;

		private final @Nullable KProperty<?> valueProperty;

		private final boolean applyBoxing;

		private final @Nullable ValueBoxing next;

		/**
		 * Creates a new {@link ValueBoxing} for a {@link KParameter}.
		 *
		 * @param rules boxing rules to apply.
		 * @param parameter the copy or constructor parameter.
		 */
		@SuppressWarnings("ConstantConditions")
		private ValueBoxing(BoxingRules rules, KParameter parameter) {
			this(rules, parameter.getType(), resolveClass(parameter.getType()), parameter.isOptional());
		}

		private static KClass<?> resolveClass(KType type) {

			if (type instanceof KClass<?> kc) {
				return kc;
			}

			if (type instanceof KTypeParameter ktp) {
				return resolveClass(ktp.getUpperBounds().get(0));
			}

			KClassifier classifier = type.getClassifier();

			if (classifier != null) {
				return resolveClass(classifier);
			}

			return JvmClassMappingKt.getKotlinClass(Object.class);
		}

		private static KClass<?> resolveClass(KClassifier classifier) {

			if (classifier instanceof KClass<?> kc) {
				return kc;
			}

			if (classifier instanceof KTypeParameter ktp) {
				return resolveClass(ktp.getUpperBounds().get(0));
			}

			if (classifier instanceof KType ktp) {
				return resolveClass(ktp);
			}

			throw new UnsupportedOperationException(String.format("Unsupported KClassifier: %s", classifier));
		}

		private ValueBoxing(BoxingRules rules, KType type, KClass<?> kClass, boolean optional) {

			KFunction<?> wrapperConstructor = null;
			ValueBoxing next = null;
			boolean applyBoxing;

			if (kClass.isValue()) {
				wrapperConstructor = kClass.getConstructors().iterator().next();
				KParameter nested = wrapperConstructor.getParameters().get(0);
				KType nestedType = nested.getType();

				applyBoxing = rules.shouldApplyBoxing(type, optional, nested);

				KClass<?> nestedClass;

				// bound flattening
				if (nestedType.getClassifier() instanceof KTypeParameter ktp) {
					nestedClass = getUpperBound(ktp);
				} else {
					nestedClass = (KClass<?>) nestedType.getClassifier();
				}

				Assert.notNull(nestedClass, () -> String.format("Cannot resolve nested class from type %s", nestedType));
				this.valueProperty = kClass.getMembers().stream().filter(it -> it instanceof KProperty<?>)
						.map(KProperty.class::cast).findFirst().get();
				next = new ValueBoxing(rules, nestedType, nestedClass, nested.isOptional());
			} else {
				applyBoxing = false;
				this.valueProperty = null;
			}

			this.kClass = kClass;
			this.wrapperConstructor = wrapperConstructor;
			this.next = next;
			this.applyBoxing = applyBoxing;
		}

		private static KClass<?> getUpperBound(KTypeParameter typeParameter) {

			for (KType upperBound : typeParameter.getUpperBounds()) {

				if (upperBound.getClassifier() instanceof KClass<?> kc) {
					return kc;
				}
			}

			throw new IllegalArgumentException("No upper bounds found");
		}

		static KType resolveType(KType type) {

			if (type.getClassifier() instanceof KTypeParameter ktp) {

				for (KType upperBound : ktp.getUpperBounds()) {

					if (upperBound.getClassifier() instanceof KClass<?> kc) {
						return upperBound;
					}
				}
			}

			return type;
		}

		/**
		 * @return the expanded component type that is used as value.
		 */
		public Class<?> getActualType() {

			if (isValueClass() && hasNext()) {
				return getNext().getActualType();
			}

			return JvmClassMappingKt.getJavaClass(kClass);
		}

		/**
		 * @return the component or wrapper type to be used.
		 */
		public Class<?> getParameterType() {

			if (hasNext() && getNext().appliesBoxing()) {
				return getNext().getParameterType();
			}

			return JvmClassMappingKt.getJavaClass(kClass);
		}

		/**
		 * @return {@code true} if the value hierarchy applies boxing.
		 */
		public boolean appliesBoxing() {
			return applyBoxing;
		}

		public boolean isValueClass() {
			return kClass.isValue();
		}

		/**
		 * @return whether there is another item in the value hierarchy.
		 */
		public boolean hasNext() {
			return next != null;
		}

		/**
		 * Returns the next {@link ValueBoxing} or throws {@link IllegalStateException} if there is no next. Make sure to
		 * check {@link #hasNext()} prior to calling this method.
		 *
		 * @return the next {@link ValueBoxing}.
		 * @throws IllegalStateException if there is no next item.
		 */
		public ValueBoxing getNext() {

			if (next == null) {
				throw new IllegalStateException("No next ValueBoxing available");
			}

			return next;
		}

		/**
		 * Wrap the value into the boxing wrapper type if requested. Already wrapped values are left unchanged.
		 *
		 * @param o
		 * @return
		 */
		public @Nullable Object wrap(@Nullable Object o) {
			return doWrap(o, false, ValueBoxing::wrap);
		}

		/**
		 * Apply wrapping into the boxing wrapper type if applicable. For types, that do not require wrapping but are
		 * wrapped, the component type is being unwrapped.
		 *
		 * @param o
		 * @return
		 * @since 3.2.6
		 */
		@Nullable
		Object applyWrapping(@Nullable Object o) {
			return doWrap(o, true, ValueBoxing::applyWrapping);
		}

		/**
		 * Apply staged wrapping into the boxing wrapper type if value boxing is requested. Otherwise, apply unwrapping and
		 * pass on the result into {@code nextWrapStage}.
		 */
		@Nullable
		Object doWrap(@Nullable Object o, boolean unwrap,
				BiFunction<ValueBoxing, @Nullable Object, @Nullable Object> nextWrapStage) {

			if (applyBoxing) {
				return o == null || kClass.isInstance(o) || wrapperConstructor == null ? o
						: wrapperConstructor.call(nextWrapStage.apply(getNext(), o));
			} else if (unwrap && kClass.isValue()) {
				if (o != null && kClass.isInstance(o) && valueProperty != null) {
					o = valueProperty.getGetter().call(o);
				}
			}

			if (hasNext()) {
				return nextWrapStage.apply(getNext(), o);
			}

			return o;
		}

		@Override
		public String toString() {

			StringBuilder sb = new StringBuilder();

			ValueBoxing hierarchy = this;
			while (hierarchy != null) {

				if (!sb.isEmpty()) {
					sb.append(" -> ");
				}

				sb.append(hierarchy.kClass.getSimpleName());
				hierarchy = hierarchy.next;
			}

			return sb.toString();
		}

	}

}
