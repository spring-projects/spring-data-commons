/*
 * Copyright 2019-2024 the original author or authors.
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

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KMutableProperty;
import kotlin.reflect.KProperty;
import kotlin.reflect.KType;
import kotlin.reflect.jvm.KTypesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

/**
 * Reflection utility methods specific to Kotlin reflection. Requires Kotlin classes to be present to avoid linkage
 * errors - ensure to guard usage with {@link KotlinDetector#isKotlinPresent()}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Johannes Englmeier
 * @since 2.3
 * @see org.springframework.core.KotlinDetector#isKotlinReflectPresent()
 */
public final class KotlinReflectionUtils {

	private KotlinReflectionUtils() {}

	/**
	 * Return {@literal true} if the specified class is a supported Kotlin class. Currently supported are only regular
	 * Kotlin classes. Other class types (synthetic, SAM, lambdas) are not supported via reflection.
	 *
	 * @return {@literal true} if {@code type} is a supported Kotlin class.
	 */
	public static boolean isSupportedKotlinClass(Class<?> type) {

		if (!KotlinDetector.isKotlinType(type)) {
			return false;
		}

		return Arrays.stream(type.getDeclaredAnnotations()) //
				.filter(annotation -> annotation.annotationType().getName().equals("kotlin.Metadata")) //
				.map(annotation -> AnnotationUtils.getValue(annotation, "k")) //
				.anyMatch(it -> Integer.valueOf(KotlinClassHeaderKind.CLASS.id).equals(it));
	}

	/**
	 * Return {@literal true} if the specified class is a Kotlin data class.
	 *
	 * @return {@literal true} if {@code type} is a Kotlin data class.
	 * @since 2.5.1
	 */
	public static boolean isDataClass(Class<?> type) {

		if (!KotlinDetector.isKotlinType(type)) {
			return false;
		}

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(type);
		return kotlinClass.isData();
	}

	/**
	 * Returns a {@link KFunction} instance corresponding to the given Java {@link Method} instance, or {@code null} if
	 * this method cannot be represented by a Kotlin function.
	 *
	 * @param method the method to look up.
	 * @return the {@link KFunction} or {@code null} if the method cannot be looked up.
	 */
	@Nullable
	public static KFunction<?> findKotlinFunction(Method method) {

		KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(method);

		// Fallback to own lookup because there's no public Kotlin API for that kind of lookup until
		// https://youtrack.jetbrains.com/issue/KT-20768 gets resolved.
		return kotlinFunction == null ? findKFunction(method).orElse(null) : kotlinFunction;
	}

	/**
	 * Returns whether the {@link Method} is declared as suspend (Kotlin Coroutine).
	 *
	 * @param method the method to inspect.
	 * @return {@literal true} if the method is declared as suspend.
	 * @see KFunction#isSuspend()
	 */
	public static boolean isSuspend(Method method) {

		KFunction<?> invokedFunction = KotlinDetector.isKotlinType(method.getDeclaringClass()) ? findKotlinFunction(method)
				: null;

		return invokedFunction != null && invokedFunction.isSuspend();
	}

	/**
	 * Returns the {@link Class return type} of a Kotlin {@link Method}. Supports regular and suspended methods.
	 *
	 * @param method the method to inspect, typically any synthetic JVM {@link Method}.
	 * @return return type of the method.
	 */
	public static Class<?> getReturnType(Method method) {

		KFunction<?> kotlinFunction = KotlinReflectionUtils.findKotlinFunction(method);

		if (kotlinFunction == null) {
			throw new IllegalArgumentException(String.format("Cannot resolve %s to a KFunction", method));
		}

		return JvmClassMappingKt.getJavaClass(KTypesJvm.getJvmErasure(kotlinFunction.getReturnType()));
	}

	/**
	 * Returns whether the given {@link KType} is a {@link KClass#isValue() value} class.
	 *
	 * @param type the kotlin type to inspect.
	 * @return {@literal true} the type is a value class.
	 * @since 3.2
	 */
	public static boolean isValueClass(KType type) {

		return type.getClassifier() instanceof KClass<?> kc && kc.isValue();
	}

	/**
	 * Returns whether the given class makes uses Kotlin {@link KClass#isValue() value} classes.
	 *
	 * @param type the kotlin type to inspect.
	 * @return {@literal true} when at least one property uses Kotlin value classes.
	 * @since 3.2
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
	 * Returns {@literal} whether the given {@link MethodParameter} is nullable. Its declaring method can reference a
	 * Kotlin function, property or interface property.
	 *
	 * @return {@literal true} if {@link MethodParameter} is nullable.
	 * @since 2.0.1
	 */
	static boolean isNullable(MethodParameter parameter) {

		Method method = parameter.getMethod();

		if (method == null) {
			throw new IllegalStateException(String.format("Cannot obtain method from parameter %s", parameter));
		}

		KFunction<?> kotlinFunction = findKotlinFunction(method);

		if (kotlinFunction == null) {
			throw new IllegalArgumentException(String.format("Cannot resolve %s to a Kotlin function", parameter));
		}

		// Special handling for Coroutines
		if (kotlinFunction.isSuspend() && isLast(parameter)) {
			return false;
		}

		// see https://github.com/spring-projects/spring-framework/issues/23991
		if (kotlinFunction.getParameters().size() > parameter.getParameterIndex() + 1) {

			KType type = parameter.getParameterIndex() == -1 //
					? kotlinFunction.getReturnType() //
					: kotlinFunction.getParameters().get(parameter.getParameterIndex() + 1).getType();

			return type.isMarkedNullable();
		}

		return true;
	}

	private static boolean isLast(MethodParameter parameter) {

		Method method = parameter.getMethod();

		return method != null && parameter.getParameterIndex() == method.getParameterCount() - 1;
	}

	/**
	 * Lookup a {@link Method} to a {@link KFunction}.
	 *
	 * @param method the JVM {@link Method} to look up.
	 * @return {@link Optional} wrapping a possibly existing {@link KFunction}.
	 */
	private static Optional<? extends KFunction<?>> findKFunction(Method method) {

		KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(method.getDeclaringClass());

		return kotlinClass.getMembers() //
				.stream() //
				.flatMap(KotlinReflectionUtils::toKFunctionStream) //
				.filter(it -> isSame(it, method)) //
				.findFirst();
	}

	private static Stream<? extends KFunction<?>> toKFunctionStream(KCallable<?> it) {

		if (it instanceof KMutableProperty<?> property) {

			return Stream.of(property.getGetter(), property.getSetter());
		}

		if (it instanceof KProperty<?> property) {

			return Stream.of(property.getGetter());
		}

		if (it instanceof KFunction<?>) {
			return Stream.of((KFunction<?>) it);
		}

		return Stream.empty();
	}

	private static boolean isSame(KFunction<?> function, Method method) {

		Method javaMethod = ReflectJvmMapping.getJavaMethod(function);
		return javaMethod != null && javaMethod.equals(method);
	}

	private enum KotlinClassHeaderKind {

		CLASS(1), FILE(2), SYNTHETIC_CLASS(3), MULTI_FILE_CLASS_FACADE(4), MULTI_FILE_CLASS_PART(5);

		int id;

		KotlinClassHeaderKind(int val) {
			this.id = val;
		}
	}
}
