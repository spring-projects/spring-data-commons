/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * Helper value to abstract an accessor.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @soundtrack Benny Greb - Soulfood (Live)
 * @since 1.13
 */
public final class Accessor {

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

		PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method);

		if (descriptor == null) {
			throw new IllegalArgumentException(String.format("Invoked method %s is no accessor method!", method));
		}

		this.descriptor = descriptor;
		this.method = method;
	}

	/**
	 * Returns whether the accessor is a getter.
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
