/*
 * Copyright 2011-2021 the original author or authors.
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

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper class to find a {@link PreferredConstructor}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Roman Rodov
 * @author Mark Paluch
 */
public interface PreferredConstructorDiscoverer<T, P extends PersistentProperty<P>> {

	/**
	 * Discovers the {@link PreferredConstructor} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return the {@link PreferredConstructor} if found or {@literal null}.
	 */
	@Nullable
	static <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(Class<T> type) {

		Assert.notNull(type, "Type must not be null!");

		return Discoverers.findDiscoverer(type) //
				.discover(ClassTypeInformation.from(type), null);
	}

	/**
	 * Discovers the {@link PreferredConstructorDiscoverer} for the given {@link PersistentEntity}.
	 *
	 * @param entity must not be {@literal null}.
	 * @return the {@link PreferredConstructor} if found or {@literal null}.
	 */
	@Nullable
	static <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(PersistentEntity<T, P> entity) {

		Assert.notNull(entity, "PersistentEntity must not be null!");

		return Discoverers.findDiscoverer(entity.getType()) //
				.discover(entity.getTypeInformation(), entity);
	}

	/**
	 * Helper class to find a {@link PreferredConstructor}.
	 *
	 * @author Oliver Gierke
	 * @author Christoph Strobl
	 * @author Roman Rodov
	 * @author Mark Paluch
	 * @since 2.0
	 */
	enum Discoverers {

		/**
		 * Discovers a {@link PreferredConstructor} for Java types.
		 */
		DEFAULT {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.model.PreferredConstructorDiscoverers#discover(org.springframework.data.util.TypeInformation, org.springframework.data.mapping.PersistentEntity)
			 */
			@Nullable
			@Override
			<T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(TypeInformation<T> type,
					@Nullable PersistentEntity<T, P> entity) {

				Class<?> rawOwningType = type.getType();

				List<Constructor<?>> candidates = new ArrayList<>();
				Constructor<?> noArg = null;
				for (Constructor<?> candidate : rawOwningType.getDeclaredConstructors()) {

					// Synthetic constructors should not be considered
					if (candidate.isSynthetic()) {
						continue;
					}

					if (candidate.isAnnotationPresent(PersistenceConstructor.class)) {
						return buildPreferredConstructor(candidate, type, entity);
					}

					if (candidate.getParameterCount() == 0) {
						noArg = candidate;
					} else {
						candidates.add(candidate);
					}
				}

				if (noArg != null) {
					return buildPreferredConstructor(noArg, type, entity);
				}

				return candidates.size() > 1 || candidates.isEmpty() ? null
						: buildPreferredConstructor(candidates.iterator().next(), type, entity);
			}
		},

		/**
		 * Discovers a {@link PreferredConstructor} for Kotlin types.
		 */
		KOTLIN {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.mapping.model.PreferredConstructorDiscoverers#discover(org.springframework.data.util.TypeInformation, org.springframework.data.mapping.PersistentEntity)
			 */
			@Nullable
			@Override
			<T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(TypeInformation<T> type,
					@Nullable PersistentEntity<T, P> entity) {

				Class<?> rawOwningType = type.getType();

				return Arrays.stream(rawOwningType.getDeclaredConstructors()) //
						.filter(it -> !it.isSynthetic()) // Synthetic constructors should not be considered
						.filter(it -> it.isAnnotationPresent(PersistenceConstructor.class)) // Explicitly defined constructor trumps
																																								// all
						.map(it -> buildPreferredConstructor(it, type, entity)) //
						.findFirst() //
						.orElseGet(() -> {

							KFunction<T> primaryConstructor = KClasses
									.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(type.getType()));

							if (primaryConstructor == null) {
								return DEFAULT.discover(type, entity);
							}

							Constructor<T> javaConstructor = ReflectJvmMapping.getJavaConstructor(primaryConstructor);

							return javaConstructor != null ? buildPreferredConstructor(javaConstructor, type, entity) : null;
						});
			}
		};

		private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

		/**
		 * Find the appropriate discoverer for {@code type}.
		 *
		 * @param type must not be {@literal null}.
		 * @return the appropriate discoverer for {@code type}.
		 */
		private static Discoverers findDiscoverer(Class<?> type) {
			return KotlinReflectionUtils.isSupportedKotlinClass(type) ? KOTLIN : DEFAULT;
		}

		/**
		 * Discovers a constructor for the given type.
		 *
		 * @param type must not be {@literal null}.
		 * @param entity may be {@literal null}.
		 * @return the {@link PreferredConstructor} if found or {@literal null}.
		 */
		@Nullable
		abstract <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(TypeInformation<T> type,
				@Nullable PersistentEntity<T, P> entity);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> buildPreferredConstructor(
				Constructor<?> constructor, TypeInformation<T> typeInformation, @Nullable PersistentEntity<T, P> entity) {

			if (constructor.getParameterCount() == 0) {
				return new PreferredConstructor<>((Constructor<T>) constructor);
			}

			List<TypeInformation<?>> parameterTypes = typeInformation.getParameterTypes(constructor);
			String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(constructor);

			Parameter<Object, P>[] parameters = new Parameter[parameterTypes.size()];
			Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

			for (int i = 0; i < parameterTypes.size(); i++) {

				String name = parameterNames == null || parameterNames.length <= i ? null : parameterNames[i];
				TypeInformation<?> type = parameterTypes.get(i);
				Annotation[] annotations = parameterAnnotations[i];

				parameters[i] = new Parameter(name, type, annotations, entity);
			}

			return new PreferredConstructor<>((Constructor<T>) constructor, parameters);
		}
	}
}
