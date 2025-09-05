/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.projection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.ReflectionUtils;

/**
 * Method interceptor to invoke default methods on the repository proxy.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Johannes Englmeier
 */
public class DefaultMethodInvokingMethodInterceptor implements MethodInterceptor {

	private static final Lookup LOOKUP = MethodHandles.lookup();
	private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();

	/**
	 * Returns whether the {@code interfaceClass} declares {@link Method#isDefault() default methods}.
	 *
	 * @param interfaceClass the {@link Class} to inspect.
	 * @return {@literal true} if {@code interfaceClass} declares a default method.
	 * @since 2.2
	 */
	public static boolean hasDefaultMethods(Class<?> interfaceClass) {

		AtomicBoolean atomicBoolean = new AtomicBoolean();
		ReflectionUtils.doWithMethods(interfaceClass, method -> atomicBoolean.set(true), Method::isDefault);

		return atomicBoolean.get();
	}

	@Override
	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (!method.isDefault()) {
			return invocation.proceed();
		}

		Object[] arguments = invocation.getArguments();
		Object proxy = ((ProxyMethodInvocation) invocation).getProxy();

		return getMethodHandle(method).bindTo(proxy).invokeWithArguments(arguments);
	}

	private MethodHandle getMethodHandle(Method method) throws Exception {

		MethodHandle handle = methodHandleCache.get(method);

		if (handle == null) {

			handle = lookup(method);
			methodHandleCache.put(method, handle);
		}

		return handle;
	}

	/**
	 * Lookup a {@link MethodHandle} given {@link Method} to look up.
	 *
	 * @param method must not be {@literal null}.
	 * @return the method handle.
	 * @throws ReflectiveOperationException in case of an error during method handle lookup.
	 */
	private static MethodHandle lookup(Method method) throws ReflectiveOperationException {

		Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
		MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
		Class<?> declaringClass = method.getDeclaringClass();

		return Modifier.isStatic(method.getModifiers())
				? lookup.findStatic(declaringClass, method.getName(), methodType)
				: lookup.findSpecial(declaringClass, method.getName(), methodType, declaringClass);
	}
}
