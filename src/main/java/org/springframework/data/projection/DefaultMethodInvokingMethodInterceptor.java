/*
 * Copyright 2015-2017 the original author or authors.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * Method interceptor to invoke default methods on the repository proxy.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class DefaultMethodInvokingMethodInterceptor implements MethodInterceptor {

	private final Constructor<MethodHandles.Lookup> constructor;

	/**
	 * Creates a new {@link DefaultMethodInvokingMethodInterceptor}.
	 */
	public DefaultMethodInvokingMethodInterceptor() {
		constructor = tryToGetLookupConstructor();
	}

	private static Constructor<Lookup> tryToGetLookupConstructor() {

		try {

			Constructor<Lookup> accessibleConstructor = Lookup.class.getDeclaredConstructor(Class.class);

			if (!accessibleConstructor.isAccessible()) {
				accessibleConstructor.setAccessible(true);
			}

			return accessibleConstructor;
		} catch (Exception ex) {
			// this is the signal that we are on Java 9 and can't use the accessible constructor approach.
			if (ex.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
				return null;
			} else {
				throw new IllegalStateException(ex);
			}
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (!method.isDefault()) {
			return invocation.proceed();
		}

		Object[] arguments = invocation.getArguments();
		Class<?> declaringClass = method.getDeclaringClass();
		Object proxy = ((ProxyMethodInvocation) invocation).getProxy();

		return getMethodHandle(method, arguments, declaringClass, proxy).bindTo(proxy).invokeWithArguments(arguments);
	}

	private MethodHandle getMethodHandle(Method method, Object[] arguments, Class<?> declaringClass, Object proxy)
			throws Throwable {

		if (constructor != null) {
			// java 8 variant
			return constructor.newInstance(declaringClass).unreflectSpecial(method, declaringClass);
		}

		// java 9 variant
		return MethodHandles.lookup() //
				.findSpecial( //
						method.getDeclaringClass(), //
						method.getName(), //
						MethodType.methodType(method.getReturnType(), method.getParameterTypes()), //
						method.getDeclaringClass() //
		);
	}
}
