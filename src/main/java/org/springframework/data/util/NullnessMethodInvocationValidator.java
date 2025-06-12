/*
 * Copyright 2017-2025 the original author or authors.
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

import kotlin.reflect.KFunction;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.Nullness;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Interceptor enforcing required return value and method parameter constraints declared on repository query methods.
 * Supports Kotlin nullness markers, JSpecify, and JSR-305 Non-null annotations. Originally implemented via
 * {@link org.springframework.data.repository.core.support.MethodInvocationValidator}.
 *
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @author Christoph Strobl
 * @since 3.5
 * @see org.jspecify.annotations.NonNull
 * @see org.springframework.core.Nullness
 * @see ReflectionUtils#isNullable(MethodParameter)
 * @link <a href="https://www.thedictionaryofobscuresorrows.com/word/nullness">Nullness</a>
 */
public class NullnessMethodInvocationValidator implements MethodInterceptor {

	private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
	private final Map<Method, MethodNullness> nullabilityCache = new ConcurrentHashMap<>(16);

	/**
	 * Returns {@literal true} if the {@code type} is supported by this interceptor.
	 *
	 * @param type the interface class.
	 * @return {@literal true} if the {@code type} is supported by this interceptor.
	 */
	public static boolean supports(Class<?> type) {

		if (type.getPackage() != null && type.getPackage().isAnnotationPresent(NullMarked.class)) {
			return true;
		}

		return KotlinDetector.isKotlinPresent() && KotlinReflectionUtils.isSupportedKotlinClass(type)
				|| NullableUtils.isNonNull(type, ElementType.METHOD) || NullableUtils.isNonNull(type, ElementType.PARAMETER);
	}

	@Override
	public @Nullable Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		MethodNullness nullness = nullabilityCache.get(method);

		if (nullness == null) {

			nullness = MethodNullness.of(method, discoverer);
			nullabilityCache.put(method, nullness);
		}

		Object[] arguments = invocation.getArguments();

		for (int i = 0; i < method.getParameterCount(); i++) {

			if (nullness.isNullableParameter(i)) {
				continue;
			}

			if ((arguments.length < i) || (arguments[i] == null)) {
				throw argumentIsNull(method, nullness.getMethodParameterName(i));
			}
		}

		Object result = invocation.proceed();

		if ((result == null) && !nullness.isNullableReturn()) {
			throw returnValueIsNull(method);
		}

		return result;
	}

	/**
	 * Template method to construct a {@link RuntimeException} indicating failure to provide a non-{@literal null} value
	 * for a method parameter.
	 *
	 * @param method
	 * @param parameterName
	 * @return
	 */
	protected RuntimeException argumentIsNull(Method method, String parameterName) {
		return new IllegalArgumentException(String.format("Parameter %s in %s.%s must not be null", parameterName,
				ClassUtils.getShortName(method.getDeclaringClass()), method.getName()));
	}

	/**
	 * Template method to construct a {@link RuntimeException} indicating failure to return a non-{@literal null} return
	 * value.
	 *
	 * @param method
	 * @return
	 */
	protected RuntimeException returnValueIsNull(Method method) {
		return new NullPointerException("Return value is null but must not be null");
	}

	static final class MethodNullness {

		private final boolean nullableReturn;
		private final boolean[] nullableParameters;
		private final MethodParameter[] methodParameters;

		private MethodNullness(boolean nullableReturn, boolean[] nullableParameters, MethodParameter[] methodParameters) {
			this.nullableReturn = nullableReturn;
			this.nullableParameters = nullableParameters;
			this.methodParameters = methodParameters;
		}

		static MethodNullness of(Method method, ParameterNameDiscoverer discoverer) {

			boolean nullableReturn = isNullableParameter(new MethodParameter(method, -1));
			boolean[] nullableParameters = new boolean[method.getParameterCount()];
			MethodParameter[] methodParameters = new MethodParameter[method.getParameterCount()];

			for (int i = 0; i < method.getParameterCount(); i++) {

				MethodParameter parameter = new MethodParameter(method, i);
				parameter.initParameterNameDiscovery(discoverer);
				nullableParameters[i] = isNullableParameter(parameter);
				methodParameters[i] = parameter;
			}

			return new MethodNullness(nullableReturn, nullableParameters, methodParameters);
		}

		String getMethodParameterName(int index) {

			String parameterName = methodParameters[index].getParameterName();

			if (parameterName == null) {
				parameterName = String.format("of type %s at index %d",
						ClassUtils.getShortName(methodParameters[index].getParameterType()), index);
			}

			return parameterName;
		}

		boolean isNullableReturn() {
			return nullableReturn;
		}

		boolean isNullableParameter(int index) {
			return nullableParameters[index];
		}

		private static boolean isNullableParameter(MethodParameter parameter) {

			Nullness nullness = Nullness.forMethodParameter(parameter);

			if (nullness == Nullness.NON_NULL) {
				return false;
			}

			return nullness == Nullness.NULLABLE || requiresNoValue(parameter) || NullableUtils.isExplicitNullable(parameter)
					|| (KotlinReflectionUtils.isSupportedKotlinClass(parameter.getDeclaringClass())
							&& ReflectionUtils.isNullable(parameter));
		}

		/**
		 * Check method return nullability
		 *
		 * @param method
		 * @return
		 */
		private static boolean allowNullableReturn(@Nullable Method method) {

			if (method == null) {
				return false;
			}

			KFunction<?> function = KotlinDetector.isKotlinType(method.getDeclaringClass())
					? KotlinReflectionUtils.findKotlinFunction(method)
					: null;
			return function != null && function.getReturnType().isMarkedNullable();
		}

		private static boolean requiresNoValue(MethodParameter parameter) {
			return ReflectionUtils.isVoid(parameter.getParameterType());
		}

		public boolean[] getNullableParameters() {
			return this.nullableParameters;
		}

		public MethodParameter[] getMethodParameters() {
			return this.methodParameters;
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof MethodNullness that)) {
				return false;
			}

			if (nullableReturn != that.nullableReturn) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(nullableParameters, that.nullableParameters)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(methodParameters, that.methodParameters);
		}

		@Override
		public int hashCode() {
			int result = (nullableReturn ? 1 : 0);
			result = (31 * result) + Arrays.hashCode(nullableParameters);
			result = (31 * result) + Arrays.hashCode(methodParameters);
			return result;
		}

		@Override
		public String toString() {
			return "MethodInvocationValidator.Nullability(nullableReturn=" + this.isNullableReturn() + ", nullableParameters="
					+ java.util.Arrays.toString(this.getNullableParameters()) + ", methodParameters="
					+ java.util.Arrays.deepToString(this.getMethodParameters()) + ")";
		}
	}
}
