/*
 * Copyright 2017 the original author or authors.
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

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;

/**
 * Kotlin-specific extension to {@link ClassGeneratingEntityInstantiator} that adapts Kotlin constructors with
 * defaulting.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.0
 */
public class KotlinClassGeneratingEntityInstantiator extends ClassGeneratingEntityInstantiator {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.ClassGeneratingEntityInstantiator#doCreateEntityInstantiator(org.springframework.data.mapping.PersistentEntity)
	 */
	@Override
	protected EntityInstantiator doCreateEntityInstantiator(PersistentEntity<?, ?> entity) {

		PreferredConstructor<?, ?> constructor = entity.getPersistenceConstructor();

		if (ReflectionUtils.isKotlinClass(entity.getType()) && constructor != null) {

			PreferredConstructor<?, ?> defaultConstructor = new DefaultingKotlinConstructorResolver(entity)
					.getDefaultConstructor();

			if (defaultConstructor != null) {

				ObjectInstantiator instantiator = createObjectInstantiator(entity, defaultConstructor);

				return new DefaultingKotlinClassInstantiatorAdapter(instantiator, constructor);
			}
		}

		return super.doCreateEntityInstantiator(entity);
	}

	/**
	 * Resolves a {@link PreferredConstructor} to a synthetic Kotlin constructor accepting the same user-space parameters
	 * suffixed by Kotlin-specifics required for defaulting and the {@code kotlin.jvm.internal.DefaultConstructorMarker}.
	 *
	 * @since 2.0
	 * @author Mark Paluch
	 */
	static class DefaultingKotlinConstructorResolver {

		private final @Nullable PreferredConstructor<?, ?> defaultConstructor;

		@SuppressWarnings("unchecked")
		DefaultingKotlinConstructorResolver(PersistentEntity<?, ?> entity) {

			Constructor<?> hit = resolveDefaultConstructor(entity);
			PreferredConstructor<?, ?> persistenceConstructor = entity.getPersistenceConstructor();

			if (hit != null && persistenceConstructor != null) {
				this.defaultConstructor = new PreferredConstructor<>(hit,
						persistenceConstructor.getParameters().toArray(new Parameter[0]));
			} else {
				this.defaultConstructor = null;
			}
		}

		@Nullable
		private static Constructor<?> resolveDefaultConstructor(PersistentEntity<?, ?> entity) {

			PreferredConstructor<?, ?> persistenceConstructor = entity.getPersistenceConstructor();

			if (persistenceConstructor == null) {
				return null;
			}

			Constructor<?> hit = null;
			Constructor<?> constructor = persistenceConstructor.getConstructor();

			for (Constructor<?> candidate : entity.getType().getDeclaredConstructors()) {

				// use only synthetic constructors
				if (!candidate.isSynthetic()) {
					continue;
				}

				// with a parameter count greater zero
				if (constructor.getParameterCount() == 0) {
					continue;
				}

				// candidates must contain at least two additional parameters (int, DefaultConstructorMarker)
				if (constructor.getParameterCount() + 2 > candidate.getParameterCount()) {
					continue;
				}

				java.lang.reflect.Parameter[] constructorParameters = constructor.getParameters();
				java.lang.reflect.Parameter[] candidateParameters = candidate.getParameters();

				if (!candidateParameters[candidateParameters.length - 1].getType().getName()
						.equals("kotlin.jvm.internal.DefaultConstructorMarker")) {
					continue;
				}

				if (parametersMatch(constructorParameters, candidateParameters)) {
					hit = candidate;
					break;
				}
			}

			return hit;
		}

		private static boolean parametersMatch(java.lang.reflect.Parameter[] constructorParameters,
				java.lang.reflect.Parameter[] candidateParameters) {

			return IntStream.range(0, constructorParameters.length)
					.allMatch(i -> constructorParameters[i].getType().equals(candidateParameters[i].getType()));
		}

		@Nullable
		PreferredConstructor<?, ?> getDefaultConstructor() {
			return defaultConstructor;
		}
	}

	/**
	 * Entity instantiator for Kotlin constructors that apply parameter defaulting. Kotlin constructors that apply
	 * argument defaulting are marked with {@link kotlin.jvm.internal.DefaultConstructorMarker} and accept additional
	 * parameters besides the regular (user-space) parameters. Additional parameters are:
	 * <ul>
	 * <li>defaulting bitmask ({@code int}), a bit mask slot for each 32 parameters</li>
	 * <li>{@code DefaultConstructorMarker} (usually null)</li>
	 * </ul>
	 * <strong>Defaulting bitmask</strong>
	 * <p/>
	 * The defaulting bitmask is a 32 bit integer representing which positional argument should be defaulted. Defaulted
	 * arguments are passed as {@literal null} and require the appropriate positional bit set ( {@code 1 << 2} for the 2.
	 * argument)). Since the bitmask represents only 32 bit states, it requires additional masks (slots) if more than 32
	 * arguments are represented.
	 *
	 * @author Mark Paluch
	 * @since 2.0
	 */
	static class DefaultingKotlinClassInstantiatorAdapter implements EntityInstantiator {

		private final ObjectInstantiator instantiator;
		private final List<KParameter> kParameters;
		private final Constructor<?> synthetic;

		DefaultingKotlinClassInstantiatorAdapter(ObjectInstantiator instantiator, PreferredConstructor<?, ?> constructor) {

			KFunction<?> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(constructor.getConstructor());

			if (kotlinConstructor == null) {
				throw new IllegalArgumentException(
						"No corresponding Kotlin constructor found for " + constructor.getConstructor());
			}

			this.instantiator = instantiator;
			this.kParameters = kotlinConstructor.getParameters();
			this.synthetic = constructor.getConstructor();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.convert.EntityInstantiator#createInstance(org.springframework.data.mapping.PersistentEntity, org.springframework.data.mapping.model.ParameterValueProvider)
		 */
		@Override
		@SuppressWarnings("unchecked")
		public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
				ParameterValueProvider<P> provider) {

			PreferredConstructor<? extends T, P> preferredConstructor = entity.getPersistenceConstructor();

			if (preferredConstructor == null) {
				throw new IllegalArgumentException("PreferredConstructor must not be null!");
			}

			int[] defaulting = new int[(synthetic.getParameterCount() / 32) + 1];

			Object[] params = allocateArguments(
					synthetic.getParameterCount() + defaulting.length + /* DefaultConstructorMarker */1);
			int userParameterCount = kParameters.size();

			List<Parameter<Object, P>> parameters = preferredConstructor.getParameters();

			// Prepare user-space arguments
			for (int i = 0; i < userParameterCount; i++) {

				int slot = i / 32;
				int offset = slot * 32;

				Object param = provider.getParameterValue(parameters.get(i));

				KParameter kParameter = kParameters.get(i);

				// what about null and parameter is mandatory? What if parameter is non-null?
				if (kParameter.isOptional()) {

					if (param == null) {
						defaulting[slot] = defaulting[slot] | (1 << (i - offset));
					}
				}

				params[i] = param;
			}

			// append nullability masks to creation arguments
			for (int i = 0; i < defaulting.length; i++) {
				params[userParameterCount + i] = defaulting[i];
			}

			try {
				return (T) instantiator.newInstance(params);
			} finally {
				deallocateArguments(params);
			}
		}
	}
}
