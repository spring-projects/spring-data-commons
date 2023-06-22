/*
 * Copyright 2023 the original author or authors.
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
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KProperty;
import kotlin.reflect.KType;
import kotlin.reflect.KTypeParameter;

import org.springframework.core.KotlinDetector;
import org.springframework.lang.Nullable;

/**
 * Utilities for Kotlin Value class support.
 *
 * @author Mark Paluch
 * @since 3.2
 */
class KotlinValueUtils {

	/**
	 * Returns whether the given {@link KType} is a {@link KClass#isValue() value} class.
	 *
	 * @param type the kotlin type to inspect.
	 * @return {@code true} the type is a value class.
	 */
	public static boolean isValueClass(KType type) {
		return type.getClassifier()instanceof KClass<?> kc && kc.isValue();
	}

	/**
	 * Returns whether the given class makes uses Kotlin {@link KClass#isValue() value} classes.
	 *
	 * @param type the kotlin type to inspect.
	 * @return {@code true} when at least one property uses Kotlin value classes.
	 */
	public static boolean hasValueClassProperty(Class<?> type) {

		if (!KotlinDetector.isKotlinType(type)) {
			return false;
		}

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(type);

		for (KCallable<?> member : kotlinClass.getMembers()) {
			if (member instanceof KProperty<?> kp && isValueClass(kp.getReturnType())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Creates a value hierarchy across value types from a given {@link KParameter}.
	 *
	 * @param parameter the parameter that references the value class hierarchy.
	 * @return
	 */
	public static ValueBoxing getValueHierarchy(KParameter parameter) {
		return ValueBoxing.of(parameter);
	}

	/**
	 * Creates a value hierarchy across value types from a given {@link KParameter}.
	 *
	 * @param cls the entrypoint of the type hierarchy
	 * @return
	 */
	public static ValueBoxing getValueHierarchy(Class<?> cls) {

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(cls);
		return ValueBoxing.of(kotlinClass);
	}

	/**
	 * Utility to represent Kotlin value class boxing.
	 */
	static class ValueBoxing {

		private final KClass<?> kClass;

		private final KFunction<?> wrapperConstructor;

		private final boolean applyBoxing;

		private final @Nullable ValueBoxing next;

		/**
		 * @param type the referenced type.
		 * @param optional whether the type is optional.
		 */
		private ValueBoxing(KType type, boolean optional) {
			this((KClass<?>) type.getClassifier(), optional, true);
		}

		private ValueBoxing(KClass<?> kClass, boolean optional, boolean domainTypeUsage) {

			KFunction<?> wrapperConstructor = null;
			ValueBoxing next = null;

			boolean applyBoxing = (optional || !domainTypeUsage);
			if (kClass.isValue()) {

				wrapperConstructor = kClass.getConstructors().iterator().next();
				KParameter nested = wrapperConstructor.getParameters().get(0);
				KType nestedType = nested.getType();
				KClass<?> nestedClass;

				// bound flattening
				if (nestedType.getClassifier()instanceof KTypeParameter ktp) {
					nestedClass = getUpperBound(ktp);
				} else {
					nestedClass = (KClass<?>) nestedType.getClassifier();
				}

				next = new ValueBoxing(nestedClass, nested.isOptional(), false);
			}

			this.kClass = kClass;
			this.wrapperConstructor = wrapperConstructor;
			this.next = next;
			this.applyBoxing = applyBoxing;
		}

		private static KClass<?> getUpperBound(KTypeParameter typeParameter) {

			for (KType upperBound : typeParameter.getUpperBounds()) {
				if (upperBound.getClassifier()instanceof KClass<?> kc) {
					return kc;
				}
			}

			throw new IllegalArgumentException("No upper bounds found");
		}

		/**
		 * Creates a new {@link ValueBoxing} for a {@link KParameter}.
		 *
		 * @param parameter
		 * @return
		 */
		static ValueBoxing of(KParameter parameter) {
			return new ValueBoxing(parameter.getType(), parameter.isOptional());
		}

		/**
		 * Creates a new {@link ValueBoxing} for a {@link KClass} assuming the class is the uppermost entrypoint and not a
		 * value class.
		 *
		 * @param kotlinClass
		 * @return
		 */
		public static ValueBoxing of(KClass<?> kotlinClass) {
			return new ValueBoxing(kotlinClass, false, !kotlinClass.isValue());
		}

		public Class<?> getActualType() {

			if (isValueClass() && hasNext()) {
				return next.getActualType();
			}

			return JvmClassMappingKt.getJavaClass(kClass);
		}

		public boolean isValueClass() {
			return kClass.isValue();
		}

		public boolean hasNext() {
			return next != null;
		}

		@Nullable
		public ValueBoxing getNext() {
			return next;
		}

		@Nullable
		public Object wrap(@Nullable Object o) {

			if (applyBoxing) {
				return next == null || kClass.isInstance(o) ? o : wrapperConstructor.call(next.wrap(o));
			}

			return o;
		}

		@Override
		public String toString() {

			StringBuilder sb = new StringBuilder();

			ValueBoxing hierarchy = this;
			while (hierarchy != null) {

				if (sb.length() != 0) {
					sb.append(" -> ");
				}

				sb.append(hierarchy.kClass.getSimpleName());
				hierarchy = hierarchy.next;
			}

			return sb.toString();
		}

	}

}
