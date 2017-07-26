/*
 * Copyright 2011-2017 the original author or authors.
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

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Helper class to find a {@link PreferredConstructor}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Roman Rodov
 * @author Mark Paluch
 * @since 2.0
 */
enum PreferredConstructorDiscoverers {

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
		public <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(TypeInformation<T> type,
				@Nullable PersistentEntity<T, P> entity) {

			boolean noArgConstructorFound = false;
			int numberOfArgConstructors = 0;
			Class<?> rawOwningType = type.getType();
			PreferredConstructor<T, P> constructor = null;

			for (Constructor<?> candidate : rawOwningType.getDeclaredConstructors()) {

				PreferredConstructor<T, P> preferredConstructor = buildPreferredConstructor(candidate, type, entity);

				// Synthetic constructors should not be considered
				if (preferredConstructor.getConstructor().isSynthetic()) {
					continue;
				}

				// Explicitly defined constructor trumps all
				if (preferredConstructor.isExplicitlyAnnotated()) {
					return preferredConstructor;
				}

				// No-arg constructor trumps custom ones
				if (constructor == null || preferredConstructor.isNoArgConstructor()) {
					constructor = preferredConstructor;
				}

				if (preferredConstructor.isNoArgConstructor()) {
					noArgConstructorFound = true;
				} else {
					numberOfArgConstructors++;
				}
			}

			if (!noArgConstructorFound && numberOfArgConstructors > 1) {
				constructor = null;
			}

			return constructor;
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
		public <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(TypeInformation<T> type,
				@Nullable PersistentEntity<T, P> entity) {

			Class<?> rawOwningType = type.getType();

			return Arrays.stream(rawOwningType.getDeclaredConstructors()) //
					.map(it -> buildPreferredConstructor(it, type, entity)) //
					.filter(it -> !it.getConstructor().isSynthetic()) // Synthetic constructors should not be considered
					.filter(PreferredConstructor::isExplicitlyAnnotated) // Explicitly defined constructor trumps all
					.findFirst() //
					.orElseGet(() -> {

						KFunction<T> primaryConstructor = KClasses
								.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(type.getType()));
						Constructor<T> javaConstructor = ReflectJvmMapping.getJavaConstructor(primaryConstructor);

						return javaConstructor != null ? buildPreferredConstructor(javaConstructor, type, entity) : null;
					});
		}
	};

	private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

	/**
	 * Find the appropriate discoverer for {@code type}.
	 *
	 * @param type must not be {@literal null}.
	 * @return the appropriate discoverer for {@code type}.
	 */
	public static PreferredConstructorDiscoverers findDiscoverer(Class<?> type) {
		return ReflectionUtils.isKotlinClass(type) ? KOTLIN : DEFAULT;
	}

	/**
	 * Discovers a constructor for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @param entity may be {@literal null}.
	 * @return the {@link PreferredConstructor} if found or {@literal null}.
	 */
	@Nullable
	public abstract <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(TypeInformation<T> type,
			@Nullable PersistentEntity<T, P> entity);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> buildPreferredConstructor(
			Constructor<?> constructor, TypeInformation<T> typeInformation, @Nullable PersistentEntity<T, P> entity) {

		List<TypeInformation<?>> parameterTypes = typeInformation.getParameterTypes(constructor);

		if (parameterTypes.isEmpty()) {
			return new PreferredConstructor<>((Constructor<T>) constructor);
		}

		String[] parameterNames = discoverer.getParameterNames(constructor);

		Parameter<Object, P>[] parameters = new Parameter[parameterTypes.size()];
		Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();

		for (int i = 0; i < parameterTypes.size(); i++) {

			String name = parameterNames == null ? null : parameterNames[i];
			TypeInformation<?> type = parameterTypes.get(i);
			Annotation[] annotations = parameterAnnotations[i];

			parameters[i] = new Parameter(name, type, annotations, entity);
		}

		return new PreferredConstructor<>((Constructor<T>) constructor, parameters);
	}
}
