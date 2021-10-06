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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object to encapsulate the constructor to be used when mapping persistent data to objects.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Myeonghyeon Lee
 * @author Xeno Amess
 */
public final class PreferredConstructor<T, P extends PersistentProperty<P>> implements EntityCreator<T, P> {

	private final Constructor<T> constructor;
	private final List<Parameter<Object, P>> parameters;
	private final Map<PersistentProperty<?>, Boolean> isPropertyParameterCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link PreferredConstructor} from the given {@link Constructor} and {@link Parameter}s.
	 *
	 * @param constructor must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	@SafeVarargs
	public PreferredConstructor(Constructor<T> constructor, Parameter<Object, P>... parameters) {

		Assert.notNull(constructor, "Constructor must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");

		ReflectionUtils.makeAccessible(constructor);
		this.constructor = constructor;
		this.parameters = Arrays.asList(parameters);
	}

	/**
	 * Returns the underlying {@link Constructor}.
	 *
	 * @return
	 */
	public Constructor<T> getConstructor() {
		return constructor;
	}

	/**
	 * Returns the {@link Parameter}s of the constructor.
	 *
	 * @return
	 */
	public List<Parameter<Object, P>> getParameters() {
		return parameters;
	}

	/**
	 * Returns whether the constructor has {@link Parameter}s.
	 *
	 * @see #isNoArgConstructor()
	 * @return
	 */
	public boolean hasParameters() {
		return !parameters.isEmpty();
	}

	@Override
	public int getParameterCount() {
		return parameters.size();
	}

	/**
	 * Returns whether the constructor does not have any arguments.
	 *
	 * @see #hasParameters()
	 * @return
	 */
	public boolean isNoArgConstructor() {
		return parameters.isEmpty();
	}

	/**
	 * Returns whether the constructor was explicitly selected (by {@link PersistenceConstructor}).
	 *
	 * @return
	 */
	public boolean isExplicitlyAnnotated() {
		return MergedAnnotations.from(constructor).isPresent(PersistenceConstructor.class);
	}

	/**
	 * Returns whether the given {@link PersistentProperty} is referenced in a constructor argument of the
	 * {@link PersistentEntity} backing this {@link PreferredConstructor}.
	 * <p>
	 * Results of this call are cached and reused on the next invocation. Calling this method for a
	 * {@link PersistentProperty} that was not yet added to its owning {@link PersistentEntity} will capture that state
	 * and return the same result after adding {@link PersistentProperty} to its entity.
	 *
	 * @param property must not be {@literal null}.
	 * @return {@literal true} if the {@link PersistentProperty} is used in the constructor.
	 */
	@Override
	public boolean isConstructorParameter(PersistentProperty<?> property) {

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

	/**
	 * Returns whether the given {@link Parameter} is one referring to an enclosing class. That is in case the class this
	 * {@link PreferredConstructor} belongs to is a member class actually. If that's the case the compiler creates a first
	 * constructor argument of the enclosing class type.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return {@literal true} if the {@link PersistentProperty} maps to the enclosing class.
	 */
	public boolean isEnclosingClassParameter(Parameter<?, P> parameter) {

		Assert.notNull(parameter, "Parameter must not be null!");

		if (parameters.isEmpty() || !parameter.isEnclosingClassParameter()) {
			return false;
		}

		return parameters.get(0).equals(parameter);
	}

}
