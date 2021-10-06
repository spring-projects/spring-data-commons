/*
 * Copyright 2021 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.annotation.Factory;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.EntityCreator;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Mark Paluch
 */
class EntityCreatorDiscoverer {

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private static final Set<String> WELL_KNOWN_FACTORY_NAMES = Set.of("of", "create");

	public static <T, P extends PersistentProperty<P>> EntityCreator<T, P> discover(PersistentEntity<T, P> entity) {

		boolean hasAccessibleConstructors = false;

		Constructor<?>[] declaredConstructors = entity.getType().getDeclaredConstructors();
		Method[] declaredMethods = entity.getType().getDeclaredMethods();

		boolean hasAnnotatedFactoryMethod = findAnnotation(Factory.class, declaredMethods);
		boolean hasAnnotatedConstructor = findAnnotation(PersistenceConstructor.class, declaredConstructors);

		if (hasAccessibleConstructors && hasAnnotatedFactoryMethod) {
			throw new MappingException(
					"Invalid usage of @Factory and @PersistenceConstructor on %s. Only one annotation type permitted to indicate how entity instances should be created."
							.formatted(entity.getType().getName()));
		}

		if (!hasAnnotatedConstructor) {

			List<Method> candidates = new ArrayList<>();

			for (Method method : declaredMethods) {

				validateMethod(method);

				if (!isFactoryMethod(method, entity.getType())) {
					continue;
				}

				if (hasAnnotatedFactoryMethod) {
					if (method.isAnnotationPresent(Factory.class)) {
						candidates.add(method);
					}
				} else if (WELL_KNOWN_FACTORY_NAMES.contains(method.getName())) {
					candidates.add(method);
				}
			}

			if (candidates.size() == 1) {

				Method method = candidates.get(0);
				Parameter<Object, P>[] parameters = new Parameter[method.getParameterCount()];
				var parameterAnnotations = method.getParameterAnnotations();
				var types = entity.getTypeInformation().getParameterTypes(method);

				String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

				for (var i = 0; i < parameters.length; i++) {

					var name = parameterNames == null || parameterNames.length <= i ? null : parameterNames[i];
					var type = types.get(i);
					var annotations = parameterAnnotations[i];

					parameters[i] = new Parameter(name, type, annotations, entity);
				}

				return new FactoryMethod<>(method, parameters);
			}
		}

		return PreferredConstructorDiscoverer.discover(entity);
	}

	private static void validateMethod(Method method) {

		if (MergedAnnotations.from(method).isPresent(Factory.class)) {

			if (!Modifier.isStatic(method.getModifiers())) {
				throw new MappingException(
						"@Factory can only be used on static methods. Offending method: %s".formatted(method));
			}
		}
	}

	private static <T> boolean isFactoryMethod(Method method, Class<T> type) {

		if (Modifier.isStatic(method.getModifiers()) && method.getReturnType().equals(type)
				&& method.getParameterCount() > 0) {
			return true;
		}

		return false;
	}

	private static boolean findAnnotation(Class<? extends Annotation> annotationType, AnnotatedElement... elements) {

		for (var element : elements) {
			if (MergedAnnotations.from(element).isPresent(annotationType)) {
				return true;
			}
		}

		return false;
	}
}
