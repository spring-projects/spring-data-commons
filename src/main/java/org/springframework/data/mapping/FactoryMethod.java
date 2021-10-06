/*
 * Copyright 2011-2021 the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object to encapsulate the factory method to be used when mapping persistent data to objects.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public final class FactoryMethod<T, P extends PersistentProperty<P>> implements EntityCreator<T, P> {

	private final Method factoryMethod;
	private final List<Parameter<Object, P>> parameters;
	private final Map<PersistentProperty<?>, Boolean> isPropertyParameterCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link FactoryMethod} from the given {@link Constructor} and {@link Parameter}s.
	 *
	 * @param constructor must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	@SafeVarargs
	public FactoryMethod(Method factoryMethod, Parameter<Object, P>... parameters) {

		Assert.notNull(factoryMethod, "Factory method must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");

		ReflectionUtils.makeAccessible(factoryMethod);
		this.factoryMethod = factoryMethod;
		this.parameters = Arrays.asList(parameters);
	}

	/**
	 * Returns the underlying {@link Constructor}.
	 *
	 * @return
	 */
	public Method getFactoryMethod() {
		return factoryMethod;
	}

	/**
	 * Returns the {@link Parameter}s of the constructor.
	 *
	 * @return
	 */
	public List<Parameter<Object, P>> getParameters() {
		return parameters;
	}

	@Override
	public int getParameterCount() {
		return parameters.size();
	}

	@Override
	public boolean isConstructorParameter(PersistentProperty<?> property) {
		return isFactoryMethodParameter(property);
	}

	/**
	 * Returns whether the given {@link PersistentProperty} is referenced in a constructor argument of the
	 * {@link PersistentEntity} backing this {@link FactoryMethod}.
	 * <p>
	 * Results of this call are cached and reused on the next invocation. Calling this method for a
	 * {@link PersistentProperty} that was not yet added to its owning {@link PersistentEntity} will capture that state
	 * and return the same result after adding {@link PersistentProperty} to its entity.
	 *
	 * @param property must not be {@literal null}.
	 * @return {@literal true} if the {@link PersistentProperty} is used in the constructor.
	 */
	public boolean isFactoryMethodParameter(PersistentProperty<?> property) {

		Assert.notNull(property, "Property must not be null!");

		var cached = isPropertyParameterCache.get(property);

		if (cached != null) {
			return cached;
		}

		var result = false;
		for (Parameter<?, P> parameter : parameters) {
			if (parameter.maps(property)) {
				result = true;
				break;
			}
		}

		isPropertyParameterCache.put(property, result);

		return result;
	}

}
