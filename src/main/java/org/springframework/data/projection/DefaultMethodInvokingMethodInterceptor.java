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
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.springframework.util.ReflectionUtils;

/**
 * Method interceptor to invoke default methods on the repository proxy.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
public class DefaultMethodInvokingMethodInterceptor implements MethodInterceptor {

	private final MethodHandleLookup methodHandleLookup = MethodHandleLookup.getMethodHandleLookup();
	private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentReferenceHashMap<Method, MethodHandle>(10,
			ReferenceType.WEAK);

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
		Object proxy = ((ProxyMethodInvocation) invocation).getProxy();

		return getMethodHandle(method).bindTo(proxy).invokeWithArguments(arguments);
	}

	private MethodHandle getMethodHandle(Method method) throws Exception {

		MethodHandle handle = methodHandleCache.get(method);

		if (handle == null) {

			handle = methodHandleLookup.lookup(method);
			methodHandleCache.put(method, handle);
		}

		return handle;
	}

	/**
	 * Strategies for {@link MethodHandle} lookup.
	 *
	 * @since 2.0
	 */
	enum MethodHandleLookup {

		/**
		 * Open (via reflection construction of {@link MethodHandles.Lookup}) method handle lookup. Works with Java 8 and
		 * with Java 9 permitting illegal access.
		 */
		OPEN {

			private final Constructor<Lookup> constructor = getLookupConstructor();

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#lookup(java.lang.reflect.Method)
			 */
			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {

				if (constructor == null) {
					throw new IllegalStateException("Could not obtain MethodHandles.lookup constructor");
				}

				return constructor.newInstance(method.getDeclaringClass()).unreflectSpecial(method, method.getDeclaringClass());
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#isAvailable()
			 */
			@Override
			boolean isAvailable() {
				return constructor != null;
			}
		},

		/**
		 * Encapsulated {@link MethodHandle} lookup working on Java 9.
		 */
		ENCAPSULATED {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#lookup(java.lang.reflect.Method)
			 */
			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {

				MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

				return MethodHandles.lookup().findSpecial(method.getDeclaringClass(), method.getName(), methodType,
						method.getDeclaringClass());
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#isAvailable()
			 */
			@Override
			boolean isAvailable() {
				return true;
			}
		};

		/**
		 * Lookup a {@link MethodHandle} given {@link Method} to look up.
		 *
		 * @param method must not be {@literal null}.
		 * @return the method handle.
		 * @throws ReflectiveOperationException
		 */
		abstract MethodHandle lookup(Method method) throws ReflectiveOperationException;

		/**
		 * @return {@literal true} if the lookup is available.
		 */
		abstract boolean isAvailable();

		/**
		 * Obtain the first available {@link MethodHandleLookup}.
		 *
		 * @return the {@link MethodHandleLookup}
		 * @throws IllegalStateException if no {@link MethodHandleLookup} is available.
		 */
		public static MethodHandleLookup getMethodHandleLookup() {

			for (MethodHandleLookup lookup : MethodHandleLookup.values()) {
				if (lookup.isAvailable()) {
					return lookup;
				}
			}

			throw new IllegalStateException("No MethodHandleLookup available!");
		}

		private static Constructor<Lookup> getLookupConstructor() {

			try {

				Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
				ReflectionUtils.makeAccessible(constructor);

				return constructor;

			} catch (Exception ex) {

				// this is the signal that we are on Java 9 (encapsulated) and can't use the accessible constructor approach.
				if (ex.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
					return null;
				}

				throw new IllegalStateException(ex);
			}
		}
	}
}
