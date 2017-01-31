/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.util.Assert;

/**
 * {@link ParameterValueProvider} based on a {@link PersistentEntity} to use a {@link PropertyValueProvider} to lookup
 * the value of the property referenced by the given {@link Parameter}. Additionally a
 * {@link DefaultSpELExpressionEvaluator} can be configured to get property value resolution trumped by a SpEL
 * expression evaluation.
 * 
 * @author Oliver Gierke
 */
public class PersistentEntityParameterValueProvider<P extends PersistentProperty<P>> implements
		ParameterValueProvider<P> {

	private final PersistentEntity<?, P> entity;
	private final PropertyValueProvider<P> provider;
	private final Object parent;

	/**
	 * Creates a new {@link PersistentEntityParameterValueProvider} for the given {@link PersistentEntity} and
	 * {@link PropertyValueProvider}.
	 * 
	 * @param entity must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param parent the parent object being created currently, can be {@literal null}.
	 */
	public PersistentEntityParameterValueProvider(PersistentEntity<?, P> entity, PropertyValueProvider<P> provider,
			Object parent) {

		Assert.notNull(entity, "Entity must not be null!");
		Assert.notNull(provider, "Provider must not be null!");

		this.entity = entity;
		this.provider = provider;
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T, P> parameter) {

		PreferredConstructor<?, P> constructor = entity.getPersistenceConstructor();

		if (constructor.isEnclosingClassParameter(parameter)) {
			return (T) parent;
		}

		P property = entity.getPersistentProperty(parameter.getName());

		if (property == null) {
			throw new MappingException(String.format("No property %s found on entity %s to bind constructor parameter to!",
					parameter.getName(), entity.getType()));
		}

		return provider.getPropertyValue(property);
	}
}
