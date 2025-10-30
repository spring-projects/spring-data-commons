/*
 * Copyright 2023-2025 the original author or authors.
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

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.KotlinValueUtils.ValueBoxing;
import org.springframework.data.util.ReflectionUtils;

/**
 * Delegate to allocate instantiation arguments and to resolve the actual constructor to call for inline/value class
 * instantiation. This class captures all Kotlin-specific quirks, from default constructor resolution up to mangled
 * inline-type handling so instantiator-components can delegate to this class for constructor translation and
 * constructor argument extraction.
 *
 * @author Mark Paluch
 * @author Yohan Lee
 * @since 3.1
 */
class KotlinInstantiationDelegate {

	private final PreferredConstructor<?, ?> constructor;
	private final KFunction<?> constructorFunction;
	private final List<KParameter> kParameters;
	private final Map<KParameter, Integer> indexByKParameter;
	private final List<Function<@Nullable Object, @Nullable Object>> wrappers;
	private final boolean hasDefaultConstructorMarker;

	private KotlinInstantiationDelegate(PreferredConstructor<?, ?> constructor, KFunction<?> constructorFunction) {

		this.constructor = constructor;
		this.hasDefaultConstructorMarker = hasDefaultConstructorMarker(getConstructor().getParameters());

		this.constructorFunction = constructorFunction;
		this.kParameters = constructorFunction.getParameters();
		this.indexByKParameter = new IdentityHashMap<>(kParameters.size());

		for (int i = 0; i < kParameters.size(); i++) {
			indexByKParameter.put(kParameters.get(i), i);
		}

		this.wrappers = new ArrayList<>(kParameters.size());

		for (KParameter kParameter : kParameters) {

			ValueBoxing valueBoxing = KotlinValueUtils.getConstructorValueHierarchy(kParameter);
			wrappers.add(valueBoxing::wrap);
		}
	}

	/**
	 * @return the constructor to invoke. {@link PreferredConstructor#getParameters() Constructor parameters} describe the
	 *         detected (i.e. user-facing) constructor parameters and not {@link PreferredConstructor#getConstructor()}
	 *         parameters and therefore do not contain any synthetic parameters.
	 * @since 4.0
	 */
	public InstanceCreatorMetadata<?> getInstanceCreator() {
		return constructor;
	}

	/**
	 * @return the constructor to invoke. {@link PreferredConstructor#getParameters() Constructor parameters} describe the
	 *         detected (i.e. user-facing) constructor parameters and not {@link PreferredConstructor#getConstructor()}
	 *         parameters and therefore do not contain any synthetic parameters.
	 * @since 4.0
	 */
	public Constructor<?> getConstructor() {
		return constructor.getConstructor();
	}

	/**
	 * @return number of actual constructor arguments.
	 * @see #getConstructor()
	 */
	public int getRequiredParameterCount() {
		return getConstructor().getParameterCount();
	}

	/**
	 * Extract the actual construction arguments for a direct constructor call.
	 */
	@SuppressWarnings("NullAway")
	public <P extends PersistentProperty<P>> void extractInvocationArguments(@Nullable Object[] params,
			@Nullable InstanceCreatorMetadata<P> entityCreator, ParameterValueProvider<P> provider) {

		if (entityCreator == null) {
			throw new IllegalArgumentException("EntityCreator must not be null");
		}

		int userParameterCount = kParameters.size();
		List<Parameter<Object, P>> parameters = entityCreator.getParameters();

		// Prepare user-space arguments
		for (int i = 0; i < userParameterCount; i++) {

			Parameter<Object, P> parameter = parameters.get(i);
			params[i] = provider.getParameterValue(parameter);
		}

		// late rewrapping to indicate potential absence of parameters for defaulting
		for (int i = 0; i < userParameterCount; i++) {
			params[i] = wrappers.get(i).apply(params[i]);
		}

		if (hasDefaultConstructorMarker) {

			KotlinDefaultMask defaultMask = KotlinDefaultMask.forConstructor(constructorFunction, it -> {

				int index = indexByKParameter.get(it);

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

			int[] defaulting = defaultMask.getDefaulting();
			// append nullability masks to creation arguments
			for (int i = 0; i < defaulting.length; i++) {
				params[userParameterCount + i] = defaulting[i];
			}
		}
	}

	/**
	 * Try to resolve {@code KotlinInstantiationDelegate} from a {@link PreferredConstructor}. Resolution attempts to find
	 * a JVM constructor equivalent considering value class mangling, Kotlin defaulting and potentially synthetic
	 * constructors generated by the Kotlin compile including the lookup of a {@link KFunction} from the given
	 * {@link PreferredConstructor}.
	 *
	 * @return the {@code KotlinInstantiationDelegate} if resolution was successful; {@literal null} otherwise.
	 * @since 4.0
	 */
	public static @Nullable KotlinInstantiationDelegate resolve(PreferredConstructor<?, ?> preferredConstructor) {

		KFunction<?> constructorFunction = ReflectJvmMapping.getKotlinFunction(preferredConstructor.getConstructor());

		if (constructorFunction == null) {
			return null;
		}

		PreferredConstructor<?, ?> resolved = resolveKotlinJvmConstructor(preferredConstructor, constructorFunction);
		return resolved != null ? new KotlinInstantiationDelegate(resolved, constructorFunction) : null;
	}

	/**
	 * Resolves a {@link PreferredConstructor} to the constructor to be invoked. This can be a synthetic Kotlin
	 * constructor accepting the same user-space parameters suffixed by Kotlin-specifics required for defaulting and the
	 * {@code kotlin.jvm.internal.DefaultConstructorMarker} or an actual non-synthetic constructor (i.e. private
	 * constructor).
	 * <p>
	 * Constructor resolution may return {@literal null} indicating that no matching constructor could be found.
	 * <p>
	 * The resulting constructor {@link PreferredConstructor#getParameters()} (and parameter count) reflect user-facing
	 * parameters and do not contain any synthetic parameters.
	 *
	 * @return the resolved constructor or {@literal null} if the constructor could not be resolved.
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static PreferredConstructor<?, ?> resolveKotlinJvmConstructor(PreferredConstructor<?, ?> preferredConstructor,
			KFunction<?> constructorFunction) {

		Constructor<?> hit = findKotlinConstructor(preferredConstructor.getConstructor(), constructorFunction);

		if (preferredConstructor.getConstructor().equals(hit)) {
			return preferredConstructor;
		}

		if (hit != null) {
			return new PreferredConstructor<>(hit, preferredConstructor.getParameters().toArray(new Parameter[0]));
		}

		return null;
	}

	@Nullable
	private static Constructor<?> findKotlinConstructor(Constructor<?> preferredConstructor,
			KFunction<?> constructorFunction) {

		Class<?> entityType = preferredConstructor.getDeclaringClass();
		Constructor<?> hit = null;
		Constructor<?> privateFallback = null;
		java.lang.reflect.Parameter[] detectedParameters = preferredConstructor.getParameters();
		boolean hasDefaultConstructorMarker = KotlinInstantiationDelegate.hasDefaultConstructorMarker(detectedParameters);

		for (Constructor<?> candidate : entityType.getDeclaredConstructors()) {

			java.lang.reflect.Parameter[] candidateParameters = preferredConstructor.equals(candidate)
					? detectedParameters
					: candidate.getParameters();

			if (Modifier.isPrivate(candidate.getModifiers())) {
				if (preferredConstructor.equals(candidate)) {
					privateFallback = candidate;
				}
			}

			// introspect only synthetic constructors
			if (!candidate.isSynthetic()) {
				continue;
			}

			if (!hasDefaultConstructorMarker) {

				// candidates must contain at least two additional parameters (int, DefaultConstructorMarker).
				// Number of defaulting masks derives from the original constructor arg count
				int syntheticParameters = KotlinDefaultMask.getMaskCount(detectedParameters.length)
						+ /* DefaultConstructorMarker */ 1;

				if ((detectedParameters.length + syntheticParameters) != candidate.getParameterCount()) {
					continue;
				}
			} else {

				int optionalParameterCount = getOptionalParameterCount(constructorFunction);
				int syntheticParameters = KotlinDefaultMask.getExactMaskCount(optionalParameterCount);

				if ((detectedParameters.length + syntheticParameters) != candidate.getParameterCount()) {
					continue;
				}
			}

			if (!KotlinInstantiationDelegate.hasDefaultConstructorMarker(candidateParameters)) {
				continue;
			}

			int userParameterCount = constructorFunction.getParameters().size();
			if (parametersMatch(detectedParameters, candidateParameters, userParameterCount)) {
				hit = candidate;
			}
		}

		if (hit == null) {
			return privateFallback;
		}

		return hit;
	}

	private static int getOptionalParameterCount(KFunction<?> function) {

		int count = 0;

		for (KParameter parameter : function.getParameters()) {
			if (parameter.isOptional()) {
				count++;
			}
		}

		return count;
	}

	private static boolean parametersMatch(java.lang.reflect.Parameter[] constructorParameters,
			java.lang.reflect.Parameter[] candidateParameters, int userParameterCount) {

		for (int i = 0; i < userParameterCount; i++) {
			if (!parametersMatch(constructorParameters[i].getType(), candidateParameters[i].getType())) {
				return false;
			}
		}
		return true;
	}

	private static boolean parametersMatch(Class<?> constructorParameter, Class<?> candidateParameter) {

		if (constructorParameter.equals(candidateParameter)) {
			return true;
		}

		// candidate can be also a wrapper
		Class<?> componentOrWrapperType = KotlinValueUtils.getConstructorValueHierarchy(candidateParameter).getActualType();

		return constructorParameter.equals(componentOrWrapperType);
	}

	private static boolean hasDefaultConstructorMarker(java.lang.reflect.Parameter[] parameters) {

		return parameters.length > 0 && isDefaultConstructorMarker(parameters[parameters.length - 1].getType());
	}

	private static boolean isDefaultConstructorMarker(Class<?> cls) {
		return cls.getName().equals("kotlin.jvm.internal.DefaultConstructorMarker");
	}
}
