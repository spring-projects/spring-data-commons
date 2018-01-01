/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.core.support;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.util.NullableUtils;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Interceptor enforcing required return value and method parameter constraints declared on repository query methods.
 * Supports Kotlin nullability markers and JSR-305 Non-null annotations.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see javax.annotation.Nonnull
 * @see ReflectionUtils#isNullable(MethodParameter)
 * @see NullableUtils
 */
public class MethodInvocationValidator implements MethodInterceptor {

	private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
	private final Map<Method, Nullability> nullabilityCache = new ConcurrentReferenceHashMap<>(16, ReferenceType.WEAK);

	/**
	 * Returns {@literal true} if the {@code repositoryInterface} is supported by this interceptor.
	 *
	 * @param repositoryInterface the interface class.
	 * @return {@literal true} if the {@code repositoryInterface} is supported by this interceptor.
	 */
	public static boolean supports(Class<?> repositoryInterface) {

		return ReflectionUtils.isSupportedKotlinClass(repositoryInterface)
				|| NullableUtils.isNonNull(repositoryInterface, ElementType.METHOD)
				|| NullableUtils.isNonNull(repositoryInterface, ElementType.PARAMETER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
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

			if (arguments.length < i || arguments[i] == null) {
				throw new IllegalArgumentException(
						String.format("Parameter %s in %s.%s must not be null!", nullability.getMethodParameterName(i),
								ClassUtils.getShortName(method.getDeclaringClass()), method.getName()));
			}
		}

		Object result = invocation.proceed();

		if (result == null && !nullability.isNullableReturn()) {
			throw new EmptyResultDataAccessException("Result must not be null!", 1);
		}

		return result;
	}

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	static class Nullability {

		boolean nullableReturn;
		boolean[] nullableParameters;
		MethodParameter methodParameters[];

		static Nullability of(Method method, ParameterNameDiscoverer discoverer) {

			boolean nullableReturn = isNullableParameter(new MethodParameter(method, -1));
			boolean[] nullableParameters = new boolean[method.getParameterCount()];
			MethodParameter methodParameters[] = new MethodParameter[method.getParameterCount()];

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
					|| (ReflectionUtils.isSupportedKotlinClass(parameter.getDeclaringClass())
							&& ReflectionUtils.isNullable(parameter));
		}

		private static boolean requiresNoValue(MethodParameter parameter) {
			return parameter.getParameterType().equals(Void.class) || parameter.getParameterType().equals(Void.TYPE);
		}
	}
}
