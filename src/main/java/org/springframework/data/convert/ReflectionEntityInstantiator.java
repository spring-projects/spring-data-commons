/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.convert;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.MappingInstantiationException;
import org.springframework.data.mapping.model.ParameterValueProvider;

/**
 * {@link EntityInstantiator} that uses the {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an
 * instance of the entity via reflection.
 * 
 * @author Oliver Gierke
 */
public enum ReflectionEntityInstantiator implements EntityInstantiator {

	INSTANCE;

	@SuppressWarnings("unchecked")
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		return entity.getPersistenceConstructor().map(constructor -> {

			List<Object> params = Optional.ofNullable(provider)//
					.map(it -> constructor.getParameters().stream()//
							.map(parameter -> it.getParameterValue(parameter).orElse(null))//
							.collect(Collectors.toList()))//
					.orElseGet(() -> Collections.emptyList());

			try {
				return (T) BeanUtils.instantiateClass(constructor.getConstructor(), params.toArray());
			} catch (BeanInstantiationException e) {
				throw new MappingInstantiationException(Optional.of(entity), params, e);
			}

		}).orElseGet(() -> {

			try {

				Class<? extends T> clazz = entity.getType();

				if (clazz.isArray()) {

					Class<?> ctype = clazz;
					int dims = 0;

					while (ctype.isArray()) {
						ctype = ctype.getComponentType();
						dims++;
					}

					return (T) Array.newInstance(clazz, dims);

				} else {
					return BeanUtils.instantiateClass(clazz);
				}

			} catch (BeanInstantiationException e) {
				throw new MappingInstantiationException(Optional.of(entity), Collections.emptyList(), e);
			}
		});
	}
}
