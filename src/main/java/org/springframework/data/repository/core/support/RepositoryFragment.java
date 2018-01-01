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
package org.springframework.data.repository.core.support;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Value object representing a repository fragment.
 * <p />
 * Repository fragments are individual parts that contribute method signatures. They are used to form a
 * {@link RepositoryComposition}. Fragments can be purely structural or backed with an implementation.
 * <p/>
 * {@link #structural(Class) Structural} fragments are not backed by an implementation and are primarily used to
 * discover the structure of a repository composition and to perform validations.
 * <p/>
 * {@link #implemented(Object) Implemented} repository fragments consist of a signature contributor and the implementing
 * object. A signature contributor may be {@link #implemented(Class, Object) an interface} or
 * {@link #implemented(Object) just the implementing object} providing the available signatures for a repository.
 * <p/>
 * Fragments are immutable.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see RepositoryComposition
 */
public interface RepositoryFragment<T> {

	/**
	 * Create an implemented {@link RepositoryFragment} backed by the {@code implementation} object.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return
	 */
	public static <T> RepositoryFragment<T> implemented(T implementation) {
		return new ImplementedRepositoryFragment<T>(Optional.empty(), implementation);
	}

	/**
	 * Create an implemented {@link RepositoryFragment} from a {@code interfaceClass} backed by the {@code implementation}
	 * object.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return
	 */
	public static <T> RepositoryFragment<T> implemented(Class<T> interfaceClass, T implementation) {
		return new ImplementedRepositoryFragment<>(Optional.of(interfaceClass), implementation);
	}

	/**
	 * Create a structural {@link RepositoryFragment} given {@code interfaceOrImplementation}.
	 *
	 * @param interfaceOrImplementation must not be {@literal null}.
	 * @return
	 */
	public static <T> RepositoryFragment<T> structural(Class<T> interfaceOrImplementation) {
		return new StructuralRepositoryFragment<>(interfaceOrImplementation);
	}

	/**
	 * Attempt to find the {@link Method} by name and exact parameters. Returns {@literal true} if the method was found or
	 * {@literal false} otherwise.
	 *
	 * @param method must not be {@literal null}.
	 * @return {@literal true} if the method was found or {@literal false} otherwise
	 */
	default boolean hasMethod(Method method) {

		Assert.notNull(method, "Method must not be null!");
		return ReflectionUtils.findMethod(getSignatureContributor(), method.getName(), method.getParameterTypes()) != null;
	}

	/**
	 * @return the optional implementation. Only available for implemented fragments. Structural fragments return always
	 *         {@link Optional#empty()}.
	 */
	default Optional<T> getImplementation() {
		return Optional.empty();
	}

	/**
	 * @return a {@link Stream} of methods exposed by this {@link RepositoryFragment}.
	 */
	default Stream<Method> methods() {
		return Arrays.stream(getSignatureContributor().getMethods());
	}

	/**
	 * @return the class/interface providing signatures for this {@link RepositoryFragment}.
	 */
	Class<?> getSignatureContributor();

	/**
	 * Implement a structural {@link RepositoryFragment} given its {@code implementation} object. Returns an implemented
	 * {@link RepositoryFragment}.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return a new implemented {@link RepositoryFragment} for {@code implementation}.
	 */
	RepositoryFragment<T> withImplementation(T implementation);

	@RequiredArgsConstructor
	@EqualsAndHashCode(callSuper = false)
	static class StructuralRepositoryFragment<T> implements RepositoryFragment<T> {

		private final @NonNull Class<T> interfaceOrImplementation;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFragment#getSignatureContributor()
		 */
		@Override
		public Class<?> getSignatureContributor() {
			return interfaceOrImplementation;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFragment#withImplementation(java.lang.Object)
		 */
		@Override
		public RepositoryFragment<T> withImplementation(T implementation) {
			return new ImplementedRepositoryFragment<>(Optional.of(interfaceOrImplementation), implementation);
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("StructuralRepositoryFragment %s", ClassUtils.getShortName(interfaceOrImplementation));
		}
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode(callSuper = false)
	static class ImplementedRepositoryFragment<T> implements RepositoryFragment<T> {

		private final @NonNull Optional<Class<T>> interfaceClass;
		private final @NonNull T implementation;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFragment#getSignatureContributor()
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class<?> getSignatureContributor() {
			return interfaceClass.orElse((Class) implementation.getClass());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFragment#getImplementation()
		 */
		@Override
		public Optional<T> getImplementation() {
			return Optional.of(implementation);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFragment#withImplementation(java.lang.Object)
		 */
		@Override
		public RepositoryFragment<T> withImplementation(T implementation) {
			return new ImplementedRepositoryFragment<>(interfaceClass, implementation);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {

			return String.format("ImplementedRepositoryFragment %s%s",
					interfaceClass.map(ClassUtils::getShortName).map(it -> it + ":").orElse(""),
					ClassUtils.getShortName(implementation.getClass()));
		}
	}
}
