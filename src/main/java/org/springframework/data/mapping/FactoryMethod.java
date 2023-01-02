/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

/**
 * Value object to encapsulate the factory method to be used when mapping persistent data to objects.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public final class FactoryMethod<T, P extends PersistentProperty<P>> extends InstanceCreatorMetadataSupport<T, P> {

	/**
	 * Creates a new {@link FactoryMethod} from the given {@link Constructor} and {@link Parameter}s.
	 *
	 * @param factoryMethod must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	@SafeVarargs
	public FactoryMethod(Method factoryMethod, Parameter<Object, P>... parameters) {

		super(factoryMethod, parameters);
		ReflectionUtils.makeAccessible(factoryMethod);
	}

	/**
	 * Returns the underlying {@link Constructor}.
	 *
	 * @return
	 */
	public Method getFactoryMethod() {
		return (Method) getExecutable();
	}
}
