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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link MethodInterceptor} to support accessor methods to store and retrieve values from a {@link Map}.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
class MapAccessingMethodInterceptor implements MethodInterceptor {

	private final Map<String, Object> map;

	/**
	 * Creates a new {@link MapAccessingMethodInterceptor} for the given {@link Map}.
	 * 
	 * @param map must not be {@literal null}.
	 */
	public MapAccessingMethodInterceptor(Map<String, Object> map) {

		Assert.notNull(map, "Map must not be null!");
		this.map = map;
	}

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();

		if (ReflectionUtils.isObjectMethod(method)) {
			return invocation.proceed();
		}

		Accessor accessor = new Accessor(method);

		if (accessor.isGetter()) {
			return map.get(accessor.getPropertyName());
		} else if (accessor.isSetter()) {
			map.put(accessor.getPropertyName(), invocation.getArguments()[0]);
			return null;
		}

		throw new IllegalStateException("Should never get here!");
	}

	/**
	 * Helper value to abstract an accessor.
	 *
	 * @author Oliver Gierke
	 */
	private static final class Accessor {

		private final PropertyDescriptor descriptor;
		private final Method method;

		/**
		 * Creates an {@link Accessor} for the given {@link Method}.
		 * 
		 * @param method must not be {@literal null}.
		 * @throws IllegalArgumentException in case the given method is not an accessor method.
		 */
		public Accessor(Method method) {

			Assert.notNull(method, "Method must not be null!");

			this.descriptor = BeanUtils.findPropertyForMethod(method);
			this.method = method;

			Assert.notNull(descriptor, String.format("Invoked method %s is no accessor method!", method));
		}

		/**
		 * Returns whether the acessor is a getter.
		 * 
		 * @return
		 */
		public boolean isGetter() {
			return method.equals(descriptor.getReadMethod());
		}

		/**
		 * Returns whether the accessor is a setter.
		 * 
		 * @return
		 */
		public boolean isSetter() {
			return method.equals(descriptor.getWriteMethod());
		}

		/**
		 * Returns the name of the property this accessor handles.
		 * 
		 * @return will never be {@literal null}.
		 */
		public String getPropertyName() {
			return descriptor.getName();
		}
	}
}
