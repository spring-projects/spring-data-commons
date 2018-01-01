/*
 * Copyright 2012-2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.lang.Nullable;

/**
 * {@link ParameterValueProvider} based on a {@link PersistentEntity} to use a {@link PropertyValueProvider} to lookup
 * the value of the property referenced by the given {@link Parameter}. Additionally a
 * {@link DefaultSpELExpressionEvaluator} can be configured to get property value resolution trumped by a SpEL
 * expression evaluation.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class PersistentEntityParameterValueProvider<P extends PersistentProperty<P>>
		implements ParameterValueProvider<P> {

	private final @NonNull PersistentEntity<?, P> entity;
	private final @NonNull PropertyValueProvider<P> provider;
	private final @Nullable Object parent;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T, P> parameter) {

		PreferredConstructor<?, P> constructor = entity.getPersistenceConstructor();

		if (constructor != null && constructor.isEnclosingClassParameter(parameter)) {
			return (T) parent;
		}

		String name = parameter.getName();

		if (name == null) {
			throw new MappingException(String.format("Parameter %s does not have a name!", parameter));
		}

		P property = entity.getPersistentProperty(name);

		if (property == null) {
			throw new MappingException(
					String.format("No property %s found on entity %s to bind constructor parameter to!", name, entity.getType()));
		}

		return provider.getPropertyValue(property);
	}
}
