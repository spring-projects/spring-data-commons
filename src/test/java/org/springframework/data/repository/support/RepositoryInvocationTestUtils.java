/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

/**
 * Utility methods to create {@link RepositoryInvoker} instances that get a verifying proxy attached so that the
 * invocation of a given target methods or type can be verified.
 * 
 * @author Oliver Gierke
 */
class RepositoryInvocationTestUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getVerifyingRepositoryProxy(T target, VerifyingMethodInterceptor interceptor) {

		ProxyFactory factory = new ProxyFactory();
		factory.setInterfaces(target.getClass().getInterfaces());
		factory.setTarget(target);
		factory.addAdvice(interceptor);

		return (T) factory.getProxy();
	}

	public static VerifyingMethodInterceptor expectInvocationOnType(Class<?> type) {
		return new VerifyingMethodInterceptor(type, new Method[0]);
	}

	public static VerifyingMethodInterceptor expectInvocationOf(Method... methods) {
		return new VerifyingMethodInterceptor(null, methods);
	}

	/**
	 * {@link MethodInterceptor} to verify the invocation was triggered on the given type.
	 * 
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("rawtypes")
	public static final class VerifyingMethodInterceptor implements MethodInterceptor {

		private final Class expectedInvocationTarget;
		private final List<Method> methods;

		private VerifyingMethodInterceptor(Class<?> expectedInvocationTarget, Method... methods) {
			this.expectedInvocationTarget = expectedInvocationTarget;
			this.methods = Arrays.asList(methods);
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			if (!methods.isEmpty()) {
				assertThat(methods).contains(invocation.getMethod());
			} else {

				Class<?> type = invocation.getMethod().getDeclaringClass();

				assertThat(type).as("Expected methods invocation on %s but was invoked on %s!", expectedInvocationTarget, type)
						.isEqualTo(expectedInvocationTarget);
			}

			return invocation.proceed();
		}
	}
}
