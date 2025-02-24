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

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import kotlin.reflect.KFunction;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Interceptor enforcing required return value and method parameter constraints declared on repository query methods.
 * Supports Kotlin nullability markers and JSR-305 Non-null annotations.
 * Originally implemented via {@link org.springframework.data.repository.core.support.MethodInvocationValidator}.
 *
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @author Christoph Strobl
 * @since 3.5
 * @see org.springframework.lang.NonNull
 * @see ReflectionUtils#isNullable(MethodParameter)
 * @see NullableUtils
 */
public class NullabilityMethodInvocationValidator implements MethodInterceptor {

	private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
	private final Map<Method, Nullability> nullabilityCache = new ConcurrentHashMap<>(16);
	private final Function<MethodInvocation, RuntimeException> errorFunction;

	public NullabilityMethodInvocationValidator() {
		this((invocation) ->  new NullPointerException("Method marked non nullable used with null value. If this is by design consider providing additional metadata using @Nullable annotations."));
	}

	/**
	 * @param errorFunction custom function creating the error in case of failure.
	 */
	protected NullabilityMethodInvocationValidator(Function<MethodInvocation, RuntimeException> errorFunction) {
		this.errorFunction = errorFunction;
	}

	/**
	 * Returns {@literal true} if the {@code type} is supported by this interceptor.
	 *
	 * @param type the interface class.
	 * @return {@literal true} if the {@code type} is supported by this interceptor.
	 */
	public static boolean supports(Class<?> type) {

		return KotlinDetector.isKotlinPresent() && KotlinReflectionUtils.isSupportedKotlinClass(type)
				|| NullableUtils.isNonNull(type, ElementType.METHOD)
				|| NullableUtils.isNonNull(type, ElementType.PARAMETER);
	}

	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		Nullability nullability = nullabilityCache.get(method);

		if (nullability == null) {

			nullability = Nullability.of(method, discoverer);
			nullabilityCache.put(method, nullability);
		}

		Object[] arguments = invocation.getArguments();

		for (int i = 0; i < method.getParameterCount(); i++) {

			if (nullability.isNullableParameter(i)) {
				continue;
			}

			if ((arguments.length < i) || (arguments[i] == null)) {
				throw new IllegalArgumentException(
						String.format("Parameter %s in %s.%s must not be null", nullability.getMethodParameterName(i),
								ClassUtils.getShortName(method.getDeclaringClass()), method.getName()));
			}
		}

		Object result = invocation.proceed();

		if ((result == null) && !nullability.isNullableReturn()) {
			throw errorFunction.apply(invocation);
		}

		return result;
	}

	static final class Nullability {

		private final boolean nullableReturn;
		private final boolean[] nullableParameters;
		private final MethodParameter[] methodParameters;

		private Nullability(boolean nullableReturn, boolean[] nullableParameters, MethodParameter[] methodParameters) {
			this.nullableReturn = nullableReturn;
			this.nullableParameters = nullableParameters;
			this.methodParameters = methodParameters;
		}

		static Nullability of(Method method, ParameterNameDiscoverer discoverer) {

			boolean nullableReturn = isNullableParameter(new MethodParameter(method, -1));
			boolean[] nullableParameters = new boolean[method.getParameterCount()];
			MethodParameter[] methodParameters = new MethodParameter[method.getParameterCount()];

			for (int i = 0; i < method.getParameterCount(); i++) {

				MethodParameter parameter = new MethodParameter(method, i);
				parameter.initParameterNameDiscovery(discoverer);
				nullableParameters[i] = isNullableParameter(parameter);
				methodParameters[i] = parameter;
			}

			return new Nullability(nullableReturn, nullableParameters, methodParameters);
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

			return requiresNoValue(parameter) || NullableUtils.isExplicitNullable(parameter)
					|| (KotlinReflectionUtils.isSupportedKotlinClass(parameter.getDeclaringClass())
							&& (ReflectionUtils.isNullable(parameter) || allowNullableReturn(parameter.getMethod())));
		}

		/**
		 * Check method return nullability
		 * @param method
		 * @return
		 */
		private static boolean allowNullableReturn(@Nullable Method method) {

			if(method == null) {
				return false;
			}

			KFunction<?> function = KotlinDetector.isKotlinType(method.getDeclaringClass()) ?
				KotlinReflectionUtils.findKotlinFunction(method) : null;
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

			if (!(o instanceof Nullability that)) {
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
			result = (31 * result) + ObjectUtils.nullSafeHashCode(nullableParameters);
			result = (31 * result) + ObjectUtils.nullSafeHashCode(methodParameters);
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
