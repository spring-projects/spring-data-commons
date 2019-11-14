/*
 * Copyright 2019 the original author or authors.
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
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * Reflection utility methods specific to Kotlin reflection. Requires Kotlin classes to be present to avoid linkage
 * errors.
 *
 * @author Mark Paluch
 * @since 2.3
 */
public final class KotlinReflectionUtils {

	private KotlinReflectionUtils() {}

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

		if (kotlinFunction == null) {

			// Fallback to own lookup because there's no public Kotlin API for that kind of lookup until
			// https://youtrack.jetbrains.com/issue/KT-20768 gets resolved.
			return findKFunction(method).orElse(null);
		}

		return kotlinFunction;
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
			throw new IllegalStateException(String.format("Cannot obtain method from parameter %s!", parameter));
		}

		KFunction<?> kotlinFunction = findKotlinFunction(method);

		if (kotlinFunction == null) {
			throw new IllegalArgumentException(String.format("Cannot resolve %s to a Kotlin function!", parameter));
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
		return parameter.getParameterIndex() == parameter.getMethod().getParameterCount() - 1;
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

		if (it instanceof KMutableProperty<?>) {

			KMutableProperty<?> property = (KMutableProperty<?>) it;
			return Stream.of(property.getGetter(), property.getSetter());
		}

		if (it instanceof KProperty<?>) {

			KProperty<?> property = (KProperty<?>) it;
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
}
