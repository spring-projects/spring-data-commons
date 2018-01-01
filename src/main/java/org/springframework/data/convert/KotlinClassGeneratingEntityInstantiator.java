/*
 * Copyright 2017-2018 the original author or authors.
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.MappingInstantiationException;
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

		if (ReflectionUtils.isSupportedKotlinClass(entity.getType()) && constructor != null) {

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

			Object[] params = extractInvocationArguments(entity.getPersistenceConstructor(), provider);

			try {
				return (T) instantiator.newInstance(params);
			} catch (Exception e) {
				throw new MappingInstantiationException(entity, Arrays.asList(params), e);
			}
		}

		private <P extends PersistentProperty<P>, T> Object[] extractInvocationArguments(
				@Nullable PreferredConstructor<? extends T, P> preferredConstructor, ParameterValueProvider<P> provider) {

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

				Parameter<Object, P> parameter = parameters.get(i);
				Class<Object> type = parameter.getType().getType();
				Object param = provider.getParameterValue(parameter);

				KParameter kParameter = kParameters.get(i);

				// what about null and parameter is mandatory? What if parameter is non-null?
				if (kParameter.isOptional() && param == null) {

					defaulting[slot] = defaulting[slot] | (1 << (i - offset));

					if (type.isPrimitive()) {
						param = getPrimitiveDefault(type);
					}
				}

				params[i] = param;
			}

			// append nullability masks to creation arguments
			for (int i = 0; i < defaulting.length; i++) {
				params[userParameterCount + i] = defaulting[i];
			}

			return params;
		}

		private static Object getPrimitiveDefault(Class<?> type) {

			if (type == Byte.TYPE || type == Byte.class) {
				return (byte) 0;
			}

			if (type == Short.TYPE || type == Short.class) {
				return (short) 0;
			}

			if (type == Integer.TYPE || type == Integer.class) {
				return 0;
			}

			if (type == Long.TYPE || type == Long.class) {
				return 0L;
			}

			if (type == Float.TYPE || type == Float.class) {
				return 0F;
			}

			if (type == Double.TYPE || type == Double.class) {
				return 0D;
			}

			if (type == Character.TYPE || type == Character.class) {
				return '\u0000';
			}

			if (type == Boolean.TYPE) {
				return Boolean.FALSE;
			}

			throw new IllegalArgumentException(String.format("Primitive type %s not supported!", type));
		}
	}
}
