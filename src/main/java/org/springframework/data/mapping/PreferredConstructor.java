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
import java.util.Arrays;
import java.util.List;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.PersistenceCreator;
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
@SuppressWarnings("deprecation")
public final class PreferredConstructor<T, P extends PersistentProperty<P>>
		extends InstanceCreatorMetadataSupport<T, P> {

	private final List<Parameter<Object, P>> parameters;

	/**
	 * Creates a new {@link PreferredConstructor} from the given {@link Constructor} and {@link Parameter}s.
	 *
	 * @param constructor must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	@SafeVarargs
	public PreferredConstructor(Constructor<T> constructor, Parameter<Object, P>... parameters) {

		super(constructor, parameters);

		ReflectionUtils.makeAccessible(constructor);
		this.parameters = Arrays.asList(parameters);
	}

	/**
	 * Returns the underlying {@link Constructor}.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Constructor<T> getConstructor() {
		return (Constructor<T>) getExecutable();
	}

	/**
	 * Returns whether the constructor does not have any arguments.
	 *
	 * @see #hasParameters()
	 * @return
	 */
	public boolean isNoArgConstructor() {
		return !hasParameters();
	}

	/**
	 * Returns whether the constructor was explicitly selected (by {@link PersistenceConstructor}).
	 *
	 * @return
	 */
	public boolean isExplicitlyAnnotated() {

		var annotations = MergedAnnotations.from(getExecutable());

		return annotations.isPresent(PersistenceConstructor.class)
				|| annotations.isPresent(PersistenceCreator.class);
	}

	/**
	 * @param property
	 * @return
	 * @deprecated since 3.0, use {@link #isCreatorParameter(PersistentProperty)} instead.
	 */
	@Deprecated
	public boolean isConstructorParameter(PersistentProperty<?> property) {
		return isCreatorParameter(property);
	}

	@Override
	public boolean isParentParameter(Parameter<?, P> parameter) {
		return isEnclosingClassParameter(parameter);
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

		Assert.notNull(parameter, "Parameter must not be null");

		if (parameters.isEmpty() || !parameter.isEnclosingClassParameter()) {
			return false;
		}

		return parameters.get(0).equals(parameter);
	}
}
