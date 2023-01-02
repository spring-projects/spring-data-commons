/*
 * Copyright 2012-2023 the original author or authors.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.util.ReflectionUtils;

/**
 * {@link EntityInstantiator} that uses the {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an
 * instance of the entity via reflection.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
enum ReflectionEntityInstantiator implements EntityInstantiator {

	INSTANCE;

	private static final Object[] EMPTY_ARGS = new Object[0];

	@SuppressWarnings("unchecked")
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		InstanceCreatorMetadata<P> creator = entity.getInstanceCreatorMetadata();

		if (creator == null) {
			return instantiateClass(entity);
		}
		int parameterCount = creator.getParameterCount();

		Object[] params = parameterCount == 0 ? EMPTY_ARGS : new Object[parameterCount];
		int i = 0;
		for (Parameter<?, P> parameter : creator.getParameters()) {
			params[i++] = provider.getParameterValue(parameter);
		}

		if (creator instanceof FactoryMethod<?, ?> method) {

			try {
				T t = (T) ReflectionUtils.invokeMethod(method.getFactoryMethod(), null, params);

				if (t == null) {
					throw new IllegalStateException("Method %s returned null".formatted(method.getFactoryMethod()));
				}
				return t;
			} catch (Exception e) {
				throw new MappingInstantiationException(entity, new ArrayList<>(Arrays.asList(params)), e);
			}
		}

		try {
			return BeanUtils.instantiateClass(((PreferredConstructor<T, ?>) creator).getConstructor(), params);
		} catch (BeanInstantiationException e) {
			throw new MappingInstantiationException(entity, new ArrayList<>(Arrays.asList(params)), e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T instantiateClass(
			E entity) {

		try {
			Class<?> clazz = entity.getType();
			if (clazz.isArray()) {
				Class<?> ctype = clazz;
				int dims = 0;
				while (ctype.isArray()) {
					ctype = ctype.getComponentType();
					dims++;
				}
				return (T) Array.newInstance(clazz, dims);
			} else {
				return BeanUtils.instantiateClass(entity.getType());
			}
		} catch (BeanInstantiationException e) {
			throw new MappingInstantiationException(entity, Collections.emptyList(), e);
		}
	}
}
