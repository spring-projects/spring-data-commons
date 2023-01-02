/*
 * Copyright 2021-2023 the original author or authors.
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

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Discoverer for factory methods and persistence constructors.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class InstanceCreatorMetadataDiscoverer {

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	/**
	 * Discover an entity creator
	 *
	 * @param entity
	 * @param <T>
	 * @param <P>
	 * @return
	 */
	@Nullable
	public static <T, P extends PersistentProperty<P>> InstanceCreatorMetadata<P> discover(PersistentEntity<T, P> entity) {

		Constructor<?>[] declaredConstructors = entity.getType().getDeclaredConstructors();
		Method[] declaredMethods = entity.getType().getDeclaredMethods();

		boolean hasAnnotatedFactoryMethod = findAnnotation(PersistenceCreator.class, declaredMethods);
		boolean hasAnnotatedConstructor = findAnnotation(PersistenceCreator.class, declaredConstructors);

		if (hasAnnotatedConstructor && hasAnnotatedFactoryMethod) {
			throw new MappingException(
					"Invalid usage of @Factory and @PersistenceConstructor on %s; Only one annotation type permitted to indicate how entity instances should be created"
							.formatted(entity.getType().getName()));
		}

		if (hasAnnotatedFactoryMethod) {

			List<Method> candidates = discoverFactoryMethods(entity, declaredMethods);

			if (candidates.size() == 1) {
				return getFactoryMethod(entity, candidates.get(0));
			}
		}

		return PreferredConstructorDiscoverer.discover(entity);
	}

	private static <T, P extends PersistentProperty<P>> List<Method> discoverFactoryMethods(PersistentEntity<T, P> entity,
			Method[] declaredMethods) {

		List<Method> candidates = new ArrayList<>();

		for (Method method : declaredMethods) {

			validateMethod(method);

			if (!isFactoryMethod(method, entity.getType())) {
				continue;
			}

			if (findAnnotation(PersistenceCreator.class, method)) {
				candidates.add(method);
			}
		}

		return candidates;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T, P extends PersistentProperty<P>> FactoryMethod<Object, P> getFactoryMethod(
			PersistentEntity<T, P> entity, Method method) {

		Parameter<Object, P>[] parameters = new Parameter[method.getParameterCount()];
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		List<TypeInformation<?>> types = entity.getTypeInformation().getParameterTypes(method);

		String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

		for (int i = 0; i < parameters.length; i++) {

			String name = parameterNames == null || parameterNames.length <= i ? null : parameterNames[i];
			TypeInformation<?> type = types.get(i);
			Annotation[] annotations = parameterAnnotations[i];

			parameters[i] = new Parameter(name, type, annotations, entity);
		}

		return new FactoryMethod<>(method, parameters);
	}

	private static void validateMethod(Method method) {

		if (MergedAnnotations.from(method).isPresent(PersistenceCreator.class)) {

			if (!Modifier.isStatic(method.getModifiers())) {
				throw new MappingException(
						"@PersistenceCreator can only be used on static methods; Offending method: %s".formatted(method));
			}
		}
	}

	private static <T> boolean isFactoryMethod(Method method, Class<T> type) {

		// private methods not supported
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}

		// synthetic methods not supported
		if (method.isSynthetic()) {
			return false;
		}

		return Modifier.isStatic(method.getModifiers()) && method.getReturnType().isAssignableFrom(type);
	}

	private static boolean findAnnotation(Class<? extends Annotation> annotationType, AnnotatedElement... elements) {

		for (AnnotatedElement element : elements) {
			if (MergedAnnotations.from(element).isPresent(annotationType)) {
				return true;
			}
		}

		return false;
	}
}
