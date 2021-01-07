/*
 * Copyright 2015-2021 the original author or authors.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
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
	private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentReferenceHashMap<>(10, ReferenceType.WEAK);

	/**
	 * Returns whether the {@code interfaceClass} declares {@link Method#isDefault() default methods}.
	 *
	 * @param interfaceClass the {@link Class} to inspect.
	 * @return {@literal true} if {@code interfaceClass} declares a default method.
	 * @since 2.2
	 */
	public static boolean hasDefaultMethods(Class<?> interfaceClass) {

		Method[] methods = ReflectionUtils.getAllDeclaredMethods(interfaceClass);

		for (Method method : methods) {
			if (method.isDefault()) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

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
		 * Encapsulated {@link MethodHandle} lookup working on Java 9.
		 */
		ENCAPSULATED {

			private final @Nullable Method privateLookupIn = ReflectionUtils.findMethod(MethodHandles.class,
					"privateLookupIn", Class.class, Lookup.class);

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#lookup(java.lang.reflect.Method)
			 */
			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {

				if (privateLookupIn == null) {
					throw new IllegalStateException("Could not obtain MethodHandles.privateLookupIn!");
				}

				return doLookup(method, getLookup(method.getDeclaringClass(), privateLookupIn));
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#isAvailable()
			 */
			@Override
			boolean isAvailable() {
				return privateLookupIn != null;
			}

			private Lookup getLookup(Class<?> declaringClass, Method privateLookupIn) {

				Lookup lookup = MethodHandles.lookup();

				try {
					return (Lookup) privateLookupIn.invoke(MethodHandles.class, declaringClass, lookup);
				} catch (ReflectiveOperationException e) {
					return lookup;
				}
			}
		},

		/**
		 * Open (via reflection construction of {@link MethodHandles.Lookup}) method handle lookup. Works with Java 8 and
		 * with Java 9 permitting illegal access.
		 */
		OPEN {

			private final Lazy<Constructor<Lookup>> constructor = Lazy.of(MethodHandleLookup::getLookupConstructor);

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#lookup(java.lang.reflect.Method)
			 */
			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {

				if (!isAvailable()) {
					throw new IllegalStateException("Could not obtain MethodHandles.lookup constructor!");
				}

				Constructor<Lookup> constructor = this.constructor.get();

				return constructor.newInstance(method.getDeclaringClass()).unreflectSpecial(method, method.getDeclaringClass());
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#isAvailable()
			 */
			@Override
			boolean isAvailable() {
				return constructor.orElse(null) != null;
			}
		},

		/**
		 * Fallback {@link MethodHandle} lookup using {@link MethodHandles#lookup() public lookup}.
		 *
		 * @since 2.1
		 */
		FALLBACK {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.MethodHandleLookup#lookup(java.lang.reflect.Method)
			 */
			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {
				return doLookup(method, MethodHandles.lookup());
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

		private static MethodHandle doLookup(Method method, Lookup lookup)
				throws NoSuchMethodException, IllegalAccessException {

			MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

			if (Modifier.isStatic(method.getModifiers())) {
				return lookup.findStatic(method.getDeclaringClass(), method.getName(), methodType);
			}

			return lookup.findSpecial(method.getDeclaringClass(), method.getName(), methodType, method.getDeclaringClass());
		}

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

			for (MethodHandleLookup it : MethodHandleLookup.values()) {

				if (it.isAvailable()) {
					return it;
				}
			}

			throw new IllegalStateException("No MethodHandleLookup available!");
		}

		@Nullable
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
