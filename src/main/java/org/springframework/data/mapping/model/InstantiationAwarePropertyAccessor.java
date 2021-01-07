/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import java.util.function.Function;

import org.springframework.core.KotlinDetector;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link PersistentPropertyAccessor} that will use an entity's {@link PersistenceConstructor} to create a new
 * instance of it to apply a new value for a given {@link PersistentProperty}. Will only be used if the
 * {@link PersistentProperty} is to be applied on a completely immutable entity type exposing a persistence constructor.
 *
 * @author Oliver Drotbohm
 * @author Mark Paluch
 * @since 2.3
 */
public class InstantiationAwarePropertyAccessor<T> implements PersistentPropertyAccessor<T> {

	private static final String NO_SETTER_OR_CONSTRUCTOR = "Cannot set property %s because no setter, wither or copy constructor exists for %s!";
	private static final String NO_CONSTRUCTOR_PARAMETER = "Cannot set property %s because no setter, no wither and it's not part of the persistence constructor %s!";

	private final Function<T, PersistentPropertyAccessor<T>> delegateFunction;
	private final EntityInstantiators instantiators;

	private T bean;

	/**
	 * Creates an {@link InstantiationAwarePropertyAccessor} using the given delegate {@link PersistentPropertyAccessor}
	 * and {@link EntityInstantiators}.
	 *
	 * @param delegate must not be {@literal null}.
	 * @param instantiators must not be {@literal null}.
	 * @deprecated since 2.4. Using this constructor allows only setting a single property as
	 *             {@link PersistentPropertyAccessor} holds a reference to the initial bean state.
	 */
	@Deprecated
	public InstantiationAwarePropertyAccessor(PersistentPropertyAccessor<T> delegate, EntityInstantiators instantiators) {

		Assert.notNull(delegate, "Delegate PersistentPropertyAccessor must not be null!");
		Assert.notNull(instantiators, "EntityInstantiators must not be null!");

		this.instantiators = instantiators;
		this.delegateFunction = t -> delegate;
		this.bean = delegate.getBean();
	}

	/**
	 * Creates an {@link InstantiationAwarePropertyAccessor} using the given delegate {@code accessorFunction} and
	 * {@link EntityInstantiators}. {@code accessorFunction} is used to obtain a new {@link PersistentPropertyAccessor}
	 * for each property to set.
	 *
	 * @param bean must not be {@literal null}.
	 * @param accessorFunction must not be {@literal null}.
	 * @param instantiators must not be {@literal null}.
	 * @since 2.4
	 */
	public InstantiationAwarePropertyAccessor(T bean, Function<T, PersistentPropertyAccessor<T>> accessorFunction,
			EntityInstantiators instantiators) {

		Assert.notNull(bean, "Bean must not be null!");
		Assert.notNull(accessorFunction, "PersistentPropertyAccessor function must not be null!");
		Assert.notNull(instantiators, "EntityInstantiators must not be null!");

		this.delegateFunction = accessorFunction;
		this.instantiators = instantiators;
		this.bean = bean;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#setProperty(org.springframework.data.mapping.PersistentProperty, java.lang.Object)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setProperty(PersistentProperty<?> property, @Nullable Object value) {

		PersistentEntity<?, ?> owner = property.getOwner();
		PersistentPropertyAccessor<T> delegate = delegateFunction.apply(this.bean);

		if (!property.isImmutable() || property.getWither() != null || KotlinDetector.isKotlinType(owner.getType())) {

			delegate.setProperty(property, value);
			this.bean = delegate.getBean();

			return;
		}

		PreferredConstructor<?, ?> constructor = owner.getPersistenceConstructor();

		if (constructor == null) {
			throw new IllegalStateException(String.format(NO_SETTER_OR_CONSTRUCTOR, property.getName(), owner.getType()));
		}

		if (!constructor.isConstructorParameter(property)) {
			throw new IllegalStateException(
					String.format(NO_CONSTRUCTOR_PARAMETER, property.getName(), constructor.getConstructor()));
		}

		constructor.getParameters().forEach(it -> {

			if (it.getName() == null) {
				throw new IllegalStateException(
						String.format("Cannot detect parameter names of copy constructor of %s!", owner.getType()));
			}
		});

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(owner);

		this.bean = (T) instantiator.createInstance(owner, new ParameterValueProvider() {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
			 */
			@Override
			@Nullable
			@SuppressWarnings("null")
			public Object getParameterValue(Parameter parameter) {

				return property.getName().equals(parameter.getName()) //
						? value
						: delegate.getProperty(owner.getRequiredPersistentProperty(parameter.getName()));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	@Nullable
	@Override
	public Object getProperty(PersistentProperty<?> property) {
		return delegateFunction.apply(bean).getProperty(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getBean()
	 */
	@Override
	public T getBean() {
		return this.bean;
	}
}
