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

import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link MethodInterceptor} to support accessor methods to store and retrieve values from a {@link Map}.
 *
 * @author Oliver Gierke
 * @author Johannes Englmeier
 * @since 1.10
 */
class MapAccessingMethodInterceptor implements MethodInterceptor {

	private final Map<String, Object> map;

	MapAccessingMethodInterceptor(Map<String, Object> map) {

		Assert.notNull(map, "Map must not be null");

		this.map = map;
	}

	@Override
	public @Nullable Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

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

		throw new IllegalStateException("Should never get here");
	}
}
