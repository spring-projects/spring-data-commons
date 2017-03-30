/*
 * Copyright 2016-2017 the original author or authors.
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
import static org.springframework.util.ReflectionUtils.*;

import lombok.Value;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Streamable;
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
	 * @param baseClass must not be {@literal null}.
	 * @return
	 */
	@Override
	Method getTargetClassMethod(Method method, Optional<Class<?>> baseClass) {

		Supplier<Optional<Method>> directMatch = () -> baseClass
				.map(it -> findMethod(it, method.getName(), method.getParameterTypes()));

		Supplier<Optional<Method>> detailedComparison = () -> baseClass.flatMap(it -> {

			List<Supplier<Optional<Method>>> suppliers = new ArrayList<>();

			if (usesParametersWithReactiveWrappers(method)) {
				suppliers.add(() -> getMethodCandidate(method, it, assignableWrapperMatch())); //
				suppliers.add(() -> getMethodCandidate(method, it, wrapperConversionMatch()));
			}

			suppliers.add(() -> getMethodCandidate(method, it, matchParameterOrComponentType(getRepositoryInterface())));

			return Optionals.firstNonEmpty(Streamable.of(suppliers));
		});

		return Optionals.firstNonEmpty(directMatch, detailedComparison).orElse(method);
	}

	/**
	 * {@link Predicate} to check parameter assignability between a parameters in which the declared parameter may be
	 * wrapped but supports unwrapping. Usually types like {@link java.util.Optional} or {@link java.util.stream.Stream}.
	 *
	 * @param repositoryInterface
	 * @return
	 * @see QueryExecutionConverters
	 * @see #matchesGenericType
	 */
	private Predicate<ParameterOverrideCriteria> matchParameterOrComponentType(Class<?> repositoryInterface) {

		return (parameterCriteria) -> {

			Class<?> parameterType = resolveParameterType(parameterCriteria.getDeclared(), repositoryInterface);
			Type genericType = parameterCriteria.getGenericBaseType();

			if (genericType instanceof TypeVariable<?>) {

				if (!matchesGenericType((TypeVariable<?>) genericType,
						ResolvableType.forMethodParameter(parameterCriteria.getDeclared()))) {
					return false;
				}
			}

			return parameterCriteria.getBaseType().isAssignableFrom(parameterType)
					&& parameterCriteria.isAssignableFromDeclared();
		};
	}

	/**
	 * Checks whether the type is a wrapper without unwrapping support. Reactive wrappers don't like to be unwrapped.
	 *
	 * @param parameterType must not be {@literal null}.
	 * @return
	 */
	private static boolean isNonUnwrappingWrapper(Class<?> parameterType) {

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
	private static Optional<Method> getMethodCandidate(Method method, Class<?> baseClass,
			Predicate<ParameterOverrideCriteria> predicate) {

		return Arrays.stream(baseClass.getMethods())//
				.filter(it -> method.getName().equals(it.getName()))//
				.filter(it -> method.getParameterCount() == it.getParameterCount())//
				.filter(it -> parametersMatch(it, method, predicate))//
				.findFirst();
	}

	/**
	 * Checks the given method's parameters to match the ones of the given base class method. Matches generic arguments
	 * against the ones bound in the given repository interface.
	 *
	 * @param baseClassMethod must not be {@literal null}.
	 * @param declaredMethod must not be {@literal null}.
	 * @param predicate must not be {@literal null}.
	 * @return
	 */
	private static boolean parametersMatch(Method baseClassMethod, Method declaredMethod,
			Predicate<ParameterOverrideCriteria> predicate) {

		return methodParameters(baseClassMethod, declaredMethod).allMatch(predicate);
	}

	/**
	 * {@link Predicate} to check whether a method parameter is a {@link #isNonUnwrappingWrapper(Class)} and can be
	 * converted into a different wrapper. Usually {@link rx.Observable} to {@link org.reactivestreams.Publisher}
	 * conversion.
	 *
	 * @return
	 */
	private static Predicate<ParameterOverrideCriteria> wrapperConversionMatch() {

		return (parameterCriteria) -> isNonUnwrappingWrapper(parameterCriteria.getBaseType()) //
				&& isNonUnwrappingWrapper(parameterCriteria.getDeclaredType()) //
				&& ReactiveWrapperConverters.canConvert(parameterCriteria.getDeclaredType(), parameterCriteria.getBaseType());
	}

	/**
	 * {@link Predicate} to check parameter assignability between a {@link #isNonUnwrappingWrapper(Class)} parameter and a
	 * declared parameter. Usually {@link reactor.core.publisher.Flux} vs. {@link org.reactivestreams.Publisher}
	 * conversion.
	 *
	 * @return
	 */
	private static Predicate<ParameterOverrideCriteria> assignableWrapperMatch() {

		return (parameterCriteria) -> isNonUnwrappingWrapper(parameterCriteria.getBaseType()) //
				&& isNonUnwrappingWrapper(parameterCriteria.getDeclaredType()) //
				&& parameterCriteria.getBaseType().isAssignableFrom(parameterCriteria.getDeclaredType());
	}

	private static Stream<ParameterOverrideCriteria> methodParameters(Method first, Method second) {

		Assert.isTrue(first.getParameterCount() == second.getParameterCount(), "Method parameter count must be equal!");

		return IntStream.range(0, first.getParameterCount()) //
				.mapToObj(index -> ParameterOverrideCriteria.of(new MethodParameter(first, index),
						new MethodParameter(second, index)));

	}

	/**
	 * Criterion to represent {@link MethodParameter}s from a base method and its declared (overriden) method.
	 * <p>
	 * Method parameters indexes are correlated so {@link ParameterOverrideCriteria} applies only to methods with same
	 * parameter count.
	 */
	@Value(staticConstructor = "of")
	private static class ParameterOverrideCriteria {

		private final MethodParameter base;
		private final MethodParameter declared;

		/**
		 * @return base method parameter type.
		 */
		public Class<?> getBaseType() {
			return base.getParameterType();
		}

		/**
		 * @return generic base method parameter type.
		 */
		public Type getGenericBaseType() {
			return base.getGenericParameterType();
		}

		/**
		 * @return declared method parameter type.
		 */
		public Class<?> getDeclaredType() {
			return declared.getParameterType();
		}

		public boolean isAssignableFromDeclared() {
			return getBaseType().isAssignableFrom(getDeclaredType());
		}
	}
}
