/*
 * Copyright 2011-2015 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Helper class to find a {@link PreferredConstructor}.
 * 
 * @author Oliver Gierke
 */
public class PreferredConstructorDiscoverer<T, P extends PersistentProperty<P>> {

	private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

	private Optional<PreferredConstructor<T, P>> constructor;

	/**
	 * Creates a new {@link PreferredConstructorDiscoverer} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 */
	public PreferredConstructorDiscoverer(Class<T> type) {
		this(ClassTypeInformation.from(type), null);
	}

	/**
	 * Creates a new {@link PreferredConstructorDiscoverer} for the given {@link PersistentEntity}.
	 * 
	 * @param entity must not be {@literal null}.
	 */
	public PreferredConstructorDiscoverer(PersistentEntity<T, P> entity) {
		this(entity.getTypeInformation(), Optional.ofNullable(entity));
	}

	/**
	 * Creates a new {@link PreferredConstructorDiscoverer} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @param entity
	 */
	protected PreferredConstructorDiscoverer(TypeInformation<T> type, Optional<PersistentEntity<T, P>> entity) {

		boolean noArgConstructorFound = false;
		int numberOfArgConstructors = 0;
		Class<?> rawOwningType = type.getType();

		for (Constructor<?> candidate : rawOwningType.getDeclaredConstructors()) {

			PreferredConstructor<T, P> preferredConstructor = buildPreferredConstructor(candidate, type, entity);

			// Explicitly defined constructor trumps all
			if (preferredConstructor.isExplicitlyAnnotated()) {
				this.constructor = Optional.of(preferredConstructor);
				return;
			}

			// No-arg constructor trumps custom ones
			if (this.constructor == null || preferredConstructor.isNoArgConstructor()) {
				this.constructor = Optional.of(preferredConstructor);
			}

			if (preferredConstructor.isNoArgConstructor()) {
				noArgConstructorFound = true;
			} else {
				numberOfArgConstructors++;
			}
		}

		if (!noArgConstructorFound && numberOfArgConstructors > 1) {
			this.constructor = Optional.empty();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private PreferredConstructor<T, P> buildPreferredConstructor(Constructor<?> constructor,
			TypeInformation<T> typeInformation, Optional<PersistentEntity<T, P>> entity) {

		List<TypeInformation<?>> parameterTypes = typeInformation.getParameterTypes(constructor);

		if (parameterTypes.isEmpty()) {
			return new PreferredConstructor<T, P>((Constructor<T>) constructor);
		}

		String[] parameterNames = discoverer.getParameterNames(constructor);

		Parameter<Object, P>[] parameters = new Parameter[parameterTypes.size()];
		Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

		for (int i = 0; i < parameterTypes.size(); i++) {

			Optional<String> name = Optional.ofNullable(parameterNames == null ? null : parameterNames[i]);
			TypeInformation<?> type = parameterTypes.get(i);
			Annotation[] annotations = parameterAnnotations[i];

			parameters[i] = new Parameter(name, type, annotations, entity);
		}

		return new PreferredConstructor<T, P>((Constructor<T>) constructor, parameters);
	}

	/**
	 * Returns the discovered {@link PreferredConstructor}.
	 * 
	 * @return
	 */
	public Optional<PreferredConstructor<T, P>> getConstructor() {
		return constructor;
	}
}
