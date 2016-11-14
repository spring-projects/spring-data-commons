/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.springframework.core.GenericTypeResolver.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.util.Assert;

/**
 * This {@link RepositoryInformation} uses a {@link ConversionService} to check whether method arguments can be
 * converted for invocation of implementation methods.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.0
 */
public class ReactiveRepositoryInformation extends DefaultRepositoryInformation {

	/**
	 * Creates a new {@link ReactiveRepositoryInformation} for the given {@link RepositoryMetadata}, repository base
	 * class, custom implementation and {@link ConversionService}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param repositoryBaseClass must not be {@literal null}.
	 * @param customImplementationClass can be {@literal null}.
	 */
	public ReactiveRepositoryInformation(RepositoryMetadata metadata, Class<?> repositoryBaseClass,
			Optional<Class<?>> customImplementationClass) {
		super(metadata, repositoryBaseClass, customImplementationClass);
	}

	/**
	 * Returns the given target class' method if the given method (declared in the repository interface) was also declared
	 * at the target class. Returns the given method if the given base class does not declare the method given. Takes
	 * generics into account.
	 *
	 * @param method must not be {@literal null}.
	 * @param baseClass can be {@literal null}.
	 * @return
	 */
	@Override
	Method getTargetClassMethod(Method method, Optional<Class<?>> baseClass) {

		return baseClass.map(it -> {

			if (usesParametersWithReactiveWrappers(method)) {

				Method candidate = getMethodCandidate(method, it, new AssignableWrapperMatch(method.getParameterTypes()));

				if (candidate != null) {
					return candidate;
				}

				candidate = getMethodCandidate(method, it, WrapperConversionMatch.of(method.getParameterTypes()));

				if (candidate != null) {
					return candidate;
				}
			}

			Method candidate = getMethodCandidate(method, it,
					MatchParameterOrComponentType.of(method, getRepositoryInterface()));

			return candidate != null ? candidate : method;

		}).orElse(method);
	}

	/**
	 * Checks whether the type is a wrapper without unwrapping support. Reactive wrappers don't like to be unwrapped.
	 *
	 * @param parameterType must not be {@literal null}.
	 * @return
	 */
	static boolean isNonUnwrappingWrapper(Class<?> parameterType) {

		Assert.notNull(parameterType, "Parameter type must not be null!");

		return QueryExecutionConverters.supports(parameterType)
				&& !QueryExecutionConverters.supportsUnwrapping(parameterType);
	}

	/**
	 * Returns whether the given {@link Method} uses a reactive wrapper type as parameter.
	 * 
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private static boolean usesParametersWithReactiveWrappers(Method method) {

		Assert.notNull(method, "Method must not be null!");

		return Arrays.stream(method.getParameterTypes())//
				.anyMatch(ReactiveRepositoryInformation::isNonUnwrappingWrapper);
	}

	/**
	 * Returns a candidate method from the base class for the given one or the method given in the first place if none one
	 * the base class matches.
	 * 
	 * @param method must not be {@literal null}.
	 * @param baseClass must not be {@literal null}.
	 * @param predicate must not be {@literal null}.
	 * @return
	 */
	private static Method getMethodCandidate(Method method, Class<?> baseClass,
			BiPredicate<Class<?>, Integer> predicate) {

		for (Method baseClassMethod : baseClass.getMethods()) {

			// Wrong name
			if (!method.getName().equals(baseClassMethod.getName())) {
				continue;
			}

			// Wrong number of arguments
			if (!(method.getParameterTypes().length == baseClassMethod.getParameterTypes().length)) {
				continue;
			}

			// Check whether all parameters match
			if (!parametersMatch(method, baseClassMethod, predicate)) {
				continue;
			}

			return baseClassMethod;
		}

		return null;
	}

	/**
	 * Checks the given method's parameters to match the ones of the given base class method. Matches generic arguments
	 * against the ones bound in the given repository interface.
	 *
	 * @param method must not be {@literal null}.
	 * @param baseClassMethod must not be {@literal null}.
	 * @param predicate must not be {@literal null}.
	 * @return
	 */
	private static boolean parametersMatch(Method method, Method baseClassMethod,
			BiPredicate<Class<?>, Integer> predicate) {

		Type[] genericTypes = baseClassMethod.getGenericParameterTypes();
		Class<?>[] types = baseClassMethod.getParameterTypes();

		for (int i = 0; i < genericTypes.length; i++) {
			if (!predicate.test(types[i], i)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * {@link BiPredicate} to check whether a method parameter is a {@link #isNonUnwrappingWrapper(Class)} and can be
	 * converted into a different wrapper. Usually {@link rx.Observable} to {@link org.reactivestreams.Publisher}
	 * conversion.
	 */
	@RequiredArgsConstructor(staticName = "of")
	static class WrapperConversionMatch implements BiPredicate<Class<?>, Integer> {

		private final Class<?>[] declaredParameterTypes;

		/*
		 * (non-Javadoc)
		 * @see java.util.function.BiPredicate#test(java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean test(Class<?> candidateParameterType, Integer index) {

			if (!isNonUnwrappingWrapper(candidateParameterType)) {
				return false;
			}

			if (!isNonUnwrappingWrapper(declaredParameterTypes[index])) {
				return false;
			}

			return ReactiveWrappers.isAvailable()
					&& ReactiveWrapperConverters.canConvert(declaredParameterTypes[index], candidateParameterType);
		}
	}

	/**
	 * {@link BiPredicate} to check parameter assignability between a {@link #isNonUnwrappingWrapper(Class)} parameter and
	 * a declared parameter. Usually {@link reactor.core.publisher.Flux} vs. {@link org.reactivestreams.Publisher}
	 * conversion.
	 */
	@RequiredArgsConstructor(staticName = "of")
	static class AssignableWrapperMatch implements BiPredicate<Class<?>, Integer> {

		private final Class<?>[] declaredParameterTypes;

		/*
		 * (non-Javadoc)
		 * @see java.util.function.BiPredicate#test(java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean test(Class<?> candidateParameterType, Integer index) {

			if (!isNonUnwrappingWrapper(candidateParameterType)) {
				return false;
			}

			if (!isNonUnwrappingWrapper(declaredParameterTypes[index])) {
				return false;
			}

			return declaredParameterTypes[index].isAssignableFrom(candidateParameterType);
		}
	}

	/**
	 * {@link BiPredicate} to check parameter assignability between a parameters in which the declared parameter may be
	 * wrapped but supports unwrapping. Usually types like {@link java.util.Optional} or {@link java.util.stream.Stream}.
	 * 
	 * @see QueryExecutionConverters
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	static class MatchParameterOrComponentType implements BiPredicate<Class<?>, Integer> {

		private final Method declaredMethod;
		private final Class<?>[] declaredParameterTypes;
		private final Class<?> repositoryInterface;

		public static MatchParameterOrComponentType of(Method declaredMethod, Class<?> repositoryInterface) {
			return new MatchParameterOrComponentType(declaredMethod, declaredMethod.getParameterTypes(), repositoryInterface);
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.function.BiPredicate#test(java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean test(Class<?> candidateParameterType, Integer index) {

			MethodParameter parameter = new MethodParameter(declaredMethod, index);
			Class<?> parameterType = resolveParameterType(parameter, repositoryInterface);

			return candidateParameterType.isAssignableFrom(parameterType)
					&& candidateParameterType.equals(declaredParameterTypes[index]);
		}
	}
}
