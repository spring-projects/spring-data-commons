/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.projection;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * Method interceptor to invoke default methods on the repository proxy.
 *
 * @author Oliver Gierke
 */
public class DefaultMethodInvokingMethodInterceptor implements MethodInterceptor {

	private final Constructor<MethodHandles.Lookup> constructor;

	/**
	 * Creates a new {@link DefaultMethodInvokingMethodInterceptor}.
	 */
	public DefaultMethodInvokingMethodInterceptor() {

		try {
			this.constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);

			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
		} catch (Exception o_O) {
			throw new IllegalStateException(o_O);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (!org.springframework.data.util.ReflectionUtils.isDefaultMethod(method)) {
			return invocation.proceed();
		}

		Object[] arguments = invocation.getArguments();
		Class<?> declaringClass = method.getDeclaringClass();
		Object proxy = ((ProxyMethodInvocation) invocation).getProxy();

		return constructor.newInstance(declaringClass).unreflectSpecial(method, declaringClass).bindTo(proxy)
				.invokeWithArguments(arguments);
	}
}
