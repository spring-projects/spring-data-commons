/*
 * Copyright (c) 2011 by the original author(s).
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
package org.springframework.data.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.mapping.model.PreferredConstructor;
import org.springframework.data.mapping.model.PreferredConstructor.Parameter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;


/**
 * Helper class to find a {@link PreferredConstructor}.
 *
 * @author Oliver Gierke
 */
public class PreferredConstructorDiscoverer<T> {

	private final ParameterNameDiscoverer nameDiscoverer =
			new LocalVariableTableParameterNameDiscoverer();

	private PreferredConstructor<T> constructor;

	public PreferredConstructorDiscoverer(Class<T> type) {
		this(ClassTypeInformation.from(type));
	}

	/**
	 * Creates a new {@link PreferredConstructorDiscoverer} for the given type.
	 *
	 * @param owningType
	 */
	protected PreferredConstructorDiscoverer(TypeInformation<T> owningType) {

		boolean noArgConstructorFound = false;
		int numberOfArgConstructors = 0;
		Class<?> rawOwningType = owningType.getType();

		for (Constructor<?> constructor : rawOwningType.getDeclaredConstructors()) {

			PreferredConstructor<T> preferredConstructor =
					buildPreferredConstructor(constructor, owningType);

			// Explicitly defined constructor trumps all
			if (preferredConstructor.isExplicitlyAnnotated()) {
				this.constructor = preferredConstructor;
				return;
			}

			// No-arg constructor trumps custom ones
			if (this.constructor == null || preferredConstructor.isNoArgConstructor()) {
				this.constructor = preferredConstructor;
			}

			if (preferredConstructor.isNoArgConstructor()) {
				noArgConstructorFound = true;
			} else {
				numberOfArgConstructors++;
			}
		}

		if (!noArgConstructorFound && numberOfArgConstructors > 1) {
			throw new IllegalArgumentException(
					String.format("Multiple constructors with arguments found in class %s! Annotate " +
							"one with @PersistenceConstructor explicitly to select it to be used in " +
							"persistence operations.", rawOwningType.getName()));
		}
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	private PreferredConstructor<T> buildPreferredConstructor(
			Constructor<?> constructor, TypeInformation<T> typeInformation) {

		List<TypeInformation<?>> parameterTypes = typeInformation.getParameterTypes(constructor);

		if (parameterTypes.isEmpty()) {
			return new PreferredConstructor<T>((Constructor<T>) constructor);
		}

		String[] parameterNames = nameDiscoverer.getParameterNames(constructor);
		Parameter<?>[] parameters = new Parameter[parameterTypes.size()];
		Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

		for (int i = 0; i < parameterTypes.size(); i++) {

			String name = parameterNames == null ? null : parameterNames[i];
			TypeInformation<?> type = parameterTypes.get(i);
			Annotation[] annotations = parameterAnnotations[i];

			parameters[i] = new Parameter(name, type, annotations);
		}

		return new PreferredConstructor<T>((Constructor<T>) constructor,
				parameters);
	}


	public PreferredConstructor<T> getConstructor() {
		return constructor;
	}
}