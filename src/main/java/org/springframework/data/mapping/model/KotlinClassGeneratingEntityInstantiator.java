/*
 * Copyright 2017-2023 the original author or authors.
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

import static org.springframework.data.mapping.model.KotlinClassGeneratingEntityInstantiator.DefaultingKotlinConstructorResolver.*;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.lang.Nullable;

/**
 * Kotlin-specific extension to {@link ClassGeneratingEntityInstantiator} that adapts Kotlin constructors with
 * defaulting.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Johannes Englmeier
 * @since 2.0
 */
class KotlinClassGeneratingEntityInstantiator extends ClassGeneratingEntityInstantiator {

	@Override
	protected EntityInstantiator doCreateEntityInstantiator(PersistentEntity<?, ?> entity) {

		InstanceCreatorMetadata<? extends PersistentProperty<?>> creator = entity.getInstanceCreatorMetadata();

		if (KotlinReflectionUtils.isSupportedKotlinClass(entity.getType())
				&& creator instanceof PreferredConstructor<?, ?> constructor) {

			PreferredConstructor<?, ? extends PersistentProperty<?>> defaultConstructor = new DefaultingKotlinConstructorResolver(
					entity).getDefaultConstructor();

			if (defaultConstructor != null) {

				ObjectInstantiator instantiator = createObjectInstantiator(entity, defaultConstructor);

				return new DefaultingKotlinClassInstantiatorAdapter(instantiator, constructor,
						defaultConstructor.getConstructor());
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
			InstanceCreatorMetadata<? extends PersistentProperty<?>> creator = entity.getInstanceCreatorMetadata();

			if ((hit != null) && creator instanceof PreferredConstructor<?, ?> persistenceConstructor) {
				this.defaultConstructor = new PreferredConstructor<>(hit,
						persistenceConstructor.getParameters().toArray(new Parameter[0]));
			} else {
				this.defaultConstructor = null;
			}
		}

		@Nullable
		private static Constructor<?> resolveDefaultConstructor(PersistentEntity<?, ?> entity) {

			if (!(entity.getInstanceCreatorMetadata()instanceof PreferredConstructor<?, ?> persistenceConstructor)) {
				return null;
			}

			Constructor<?> hit = null;
			Constructor<?> detectedConstructor = persistenceConstructor.getConstructor();
			KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(detectedConstructor);

			for (Constructor<?> candidate : entity.getType().getDeclaredConstructors()) {

				// use only synthetic constructors
				if (!candidate.isSynthetic()) {
					continue;
				}

				java.lang.reflect.Parameter[] detectedConstructorParameters = detectedConstructor.getParameters();
				java.lang.reflect.Parameter[] candidateParameters = candidate.getParameters();

				if (!hasDefaultConstructorMarker(detectedConstructorParameters)) {

					// candidates must contain at least two additional parameters (int, DefaultConstructorMarker).
					// Number of defaulting masks derives from the original constructor arg count
					int syntheticParameters = KotlinDefaultMask.getMaskCount(detectedConstructor.getParameterCount())
							+ /* DefaultConstructorMarker */ 1;

					if ((detectedConstructor.getParameterCount() + syntheticParameters) != candidate.getParameterCount()) {
						continue;
					}
				} else {

					int optionalParameterCount = (int) kotlinFunction.getParameters().stream().filter(it -> it.isOptional())
							.count();
					int syntheticParameters = KotlinDefaultMask.getExactMaskCount(optionalParameterCount);

					if ((detectedConstructor.getParameterCount() + syntheticParameters) != candidate.getParameterCount()) {
						continue;
					}
				}

				if (!hasDefaultConstructorMarker(candidateParameters)) {
					continue;
				}

				int userParameterCount = kotlinFunction != null ? kotlinFunction.getParameters().size()
						: detectedConstructor.getParameterCount();
				if (parametersMatch(detectedConstructorParameters, candidateParameters, userParameterCount)) {
					hit = candidate;
				}
			}

			return hit;
		}

		static boolean hasDefaultConstructorMarker(java.lang.reflect.Parameter[] parameters) {

			return parameters.length > 0 && parameters[parameters.length - 1].getType().getName()
					.equals("kotlin.jvm.internal.DefaultConstructorMarker");
		}

		private static boolean parametersMatch(java.lang.reflect.Parameter[] constructorParameters,
				java.lang.reflect.Parameter[] candidateParameters, int userParameterCount) {

			return IntStream.range(0, userParameterCount)
					.allMatch(i -> parametersMatch(constructorParameters[i], candidateParameters[i]));
		}

		static boolean parametersMatch(java.lang.reflect.Parameter constructorParameter,
				java.lang.reflect.Parameter candidateParameter) {

			if (constructorParameter.getType().equals(candidateParameter.getType())) {
				return true;
			}

			// candidate can be also a wrapper

			Class<?> aClass = unwrapValueClass(candidateParameter.getType());

			return constructorParameter.getType().equals(aClass);
		}

		private static Class<?> unwrapValueClass(Class<?> type) {

			KClass<?> kotlinClass = JvmClassMappingKt.getKotlinClass(type);
			if (kotlinClass != null && kotlinClass.isValue()) {

				KFunction<?> next = kotlinClass.getConstructors().iterator().next();
				KParameter kParameter = next.getParameters().get(0);
				Type javaType = ReflectJvmMapping.getJavaType(kParameter.getType());
				if (javaType instanceof Class<?>) {
					return unwrapValueClass((Class<?>) javaType);
				}
			}

			return type;
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
	 * <p>
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
		private final KFunction<?> constructor;
		private final List<KParameter> kParameters;
		private final List<Function<Object, Object>> wrappers = new ArrayList<>();
		private final Constructor<?> constructorToInvoke;
		private final boolean hasDefaultConstructorMarker;

		DefaultingKotlinClassInstantiatorAdapter(ObjectInstantiator instantiator,
				PreferredConstructor<?, ?> defaultConstructor, Constructor<?> constructorToInvoke) {

			KFunction<?> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(defaultConstructor.getConstructor());

			if (kotlinConstructor == null) {
				throw new IllegalArgumentException(
						"No corresponding Kotlin constructor found for " + defaultConstructor.getConstructor());
			}

			this.instantiator = instantiator;
			this.constructor = kotlinConstructor;
			this.hasDefaultConstructorMarker = hasDefaultConstructorMarker(constructorToInvoke.getParameters());
			this.kParameters = kotlinConstructor.getParameters();
			this.constructorToInvoke = constructorToInvoke;

			for (KParameter kParameter : kParameters) {

				if (kParameter.getType().getClassifier()instanceof KClass<?> kc && kc.isValue() && kParameter.isOptional()) {

					// using reflection to construct a value class wrapper. Everything
					// else would require too many levels of indirections.
					wrappers.add(o -> {
						return kc.getConstructors().iterator().next().call(o);
					});
				} else {
					wrappers.add(Function.identity());
				}
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
				ParameterValueProvider<P> provider) {

			Object[] params = extractInvocationArguments(entity.getInstanceCreatorMetadata(), provider);

			try {
				return (T) instantiator.newInstance(params);
			} catch (Exception e) {
				throw new MappingInstantiationException(entity, Arrays.asList(params), e);
			}
		}

		private <P extends PersistentProperty<P>, T> Object[] extractInvocationArguments(
				@Nullable InstanceCreatorMetadata<P> entityCreator, ParameterValueProvider<P> provider) {

			if (entityCreator == null) {
				throw new IllegalArgumentException("EntityCreator must not be null");
			}

			Object[] params = allocateArguments(hasDefaultConstructorMarker ? constructorToInvoke.getParameterCount()
					: (constructorToInvoke.getParameterCount()
							+ KotlinDefaultMask.getMaskCount(constructorToInvoke.getParameterCount())
							+ /* DefaultConstructorMarker */1));
			int userParameterCount = kParameters.size();

			List<Parameter<Object, P>> parameters = entityCreator.getParameters();

			// Prepare user-space arguments
			for (int i = 0; i < userParameterCount; i++) {

				Parameter<Object, P> parameter = parameters.get(i);
				params[i] = provider.getParameterValue(parameter);
			}

			KotlinDefaultMask defaultMask = KotlinDefaultMask.forConstructor(constructor, it -> {

				int index = kParameters.indexOf(it);

				Parameter<Object, P> parameter = parameters.get(index);
				Class<Object> type = parameter.getType().getType();

				if (it.isOptional() && (params[index] == null)) {
					if (type.isPrimitive()) {

						// apply primitive defaulting to prevent NPE on primitive downcast
						params[index] = ReflectionUtils.getPrimitiveDefault(type);
					}
					return false;
				}

				return true;
			});

			// late rewrapping to indicate potential absence of parameters for defaulting
			for (int i = 0; i < userParameterCount; i++) {
				params[i] = wrappers.get(i).apply(params[i]);
			}

			int[] defaulting = defaultMask.getDefaulting();
			// append nullability masks to creation arguments
			for (int i = 0; i < defaulting.length; i++) {
				params[userParameterCount + i] = defaulting[i];
			}

			return params;
		}
	}
}
