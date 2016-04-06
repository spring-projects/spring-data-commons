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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiPredicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.util.Assert;

/**
 * This {@link RepositoryInformation} uses a {@link ConversionService} to check whether method arguments can be
 * converted for invocation of implementation methods.
 * 
 * @author Mark Paluch
 */
public class ReactiveRepositoryInformation extends DefaultRepositoryInformation {

	private final ConversionService conversionService;

	/**
	 * Creates a new {@link ReactiveRepositoryInformation} for the given repository interface and repository base class
	 * using a {@link ConversionService}.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @param repositoryBaseClass must not be {@literal null}.
	 * @param customImplementationClass
	 * @param conversionService must not be {@literal null}.
	 */
	public ReactiveRepositoryInformation(RepositoryMetadata metadata, Class<?> repositoryBaseClass,
			Class<?> customImplementationClass, ConversionService conversionService) {

		super(metadata, repositoryBaseClass, customImplementationClass);

		Assert.notNull(conversionService, "Conversion service must not be null!");

		this.conversionService = conversionService;
	}

	/**
	 * Returns the given target class' method if the given method (declared in the repository interface) was also declared
	 * at the target class. Returns the given method if the given base class does not declare the method given. Takes
	 * generics into account.
	 *
	 * @param method must not be {@literal null}
	 * @param baseClass
	 * @return
	 */
	Method getTargetClassMethod(Method method, Class<?> baseClass) {

		if (baseClass == null) {
			return method;
		}

		boolean wantsWrappers = wantsMethodUsingReactiveWrapperParameters(method);

		if (wantsWrappers) {
			Method candidate = getMethodCandidate(method, baseClass, new ExactWrapperMatch(method));

			if (candidate != null) {
				return candidate;
			}

			candidate = getMethodCandidate(method, baseClass, new WrapperConversionMatch(method, conversionService));

			if (candidate != null) {
				return candidate;
			}
		}

		Method candidate = getMethodCandidate(method, baseClass,
				new MatchParameterOrComponentType(method, getRepositoryInterface()));

		if (candidate != null) {
			return candidate;
		}

		return method;
	}

	private boolean wantsMethodUsingReactiveWrapperParameters(Method method) {

		boolean wantsWrappers = false;

		for (Class<?> parameterType : method.getParameterTypes()) {
			if (isNonunwrappingWrapper(parameterType)) {
				wantsWrappers = true;
				break;
			}
		}

		return wantsWrappers;
	}

	private Method getMethodCandidate(Method method, Class<?> baseClass, BiPredicate<Class<?>, Integer> predicate) {

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
	 * @param method
	 * @param baseClassMethod
	 * @param predicate
	 * @return
	 */
	private boolean parametersMatch(Method method, Method baseClassMethod, BiPredicate<Class<?>, Integer> predicate) {

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
	 * Checks whether the type is a wrapper without unwrapping support. Reactive wrappers don't like to be unwrapped.
	 *
	 * @param parameterType
	 * @return
	 */
	static boolean isNonunwrappingWrapper(Class<?> parameterType) {
		return QueryExecutionConverters.supports(parameterType)
				&& !QueryExecutionConverters.supportsUnwrapping(parameterType);
	}

	static class WrapperConversionMatch implements BiPredicate<Class<?>, Integer> {

		final Method declaredMethod;
		final Class<?>[] declaredParameterTypes;
		final ConversionService conversionService;

		public WrapperConversionMatch(Method declaredMethod, ConversionService conversionService) {

			this.declaredMethod = declaredMethod;
			this.declaredParameterTypes = declaredMethod.getParameterTypes();
			this.conversionService = conversionService;
		}

		@Override
		public boolean test(Class<?> candidateParameterType, Integer index) {

			// TODO: should check for component type
			if (isNonunwrappingWrapper(candidateParameterType) && isNonunwrappingWrapper(declaredParameterTypes[index])) {

				if (conversionService.canConvert(declaredParameterTypes[index], candidateParameterType)) {
					return true;
				}
			}

			return false;
		}

	}

	static class ExactWrapperMatch implements BiPredicate<Class<?>, Integer> {

		final Method declaredMethod;
		final Class<?>[] declaredParameterTypes;

		public ExactWrapperMatch(Method declaredMethod) {

			this.declaredMethod = declaredMethod;
			this.declaredParameterTypes = declaredMethod.getParameterTypes();
		}

		@Override
		public boolean test(Class<?> candidateParameterType, Integer index) {

			// TODO: should check for component type
			if (isNonunwrappingWrapper(candidateParameterType) && isNonunwrappingWrapper(declaredParameterTypes[index])) {

				if (declaredParameterTypes[index].isAssignableFrom(candidateParameterType)) {
					return true;
				}
			}

			return false;
		}

	}

	static class MatchParameterOrComponentType implements BiPredicate<Class<?>, Integer> {

		final Method declaredMethod;
		final Class<?>[] declaredParameterTypes;
		final Class<?> repositoryInterface;

		public MatchParameterOrComponentType(Method declaredMethod, Class<?> repositoryInterface) {

			this.declaredMethod = declaredMethod;
			this.declaredParameterTypes = declaredMethod.getParameterTypes();
			this.repositoryInterface = repositoryInterface;
		}

		@Override
		public boolean test(Class<?> candidateParameterType, Integer index) {

			MethodParameter parameter = new MethodParameter(declaredMethod, index);
			Class<?> parameterType = resolveParameterType(parameter, repositoryInterface);

			if (!candidateParameterType.isAssignableFrom(parameterType)
					|| !candidateParameterType.equals(declaredParameterTypes[index])) {
				return false;
			}

			return true;
		}

	}

}
