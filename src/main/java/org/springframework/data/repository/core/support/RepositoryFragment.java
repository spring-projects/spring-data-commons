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
package org.springframework.data.repository.core.support;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object representing a repository fragment. A fragment can be purely structural or implemented.
 * <p/>
 * Structural fragments are not backed by an implementation and are primarily used to discover the structure of a
 * repository composition and to perform validations.
 * <p/>
 * Implemented repository fragments consist of a signature contributor and the implementing object. A signature
 * contributor may be an interface or just the implementing object providing the available signatures for a repository.
 * <p/>
 * Fragment objects are immutable and thread-safe.
 *
 * @author Mark Paluch
 * @since 2.0
 */
public abstract class RepositoryFragment<T> {

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
	public boolean hasMethod(Method method) {

		Assert.notNull(method, "Method must not be null!");
		return ReflectionUtils.findMethod(getSignatureContributor(), method.getName(), method.getParameterTypes()) != null;
	}

	/**
	 * @return the class/interface providing signatures for this {@link RepositoryFragment}.
	 */
	protected abstract Class<?> getSignatureContributor();

	/**
	 * @return the optional implementation. Only available for implemented fragments. Structural fragments return always
	 *         {@link Optional#empty()}.
	 */
	public Optional<T> getImplementation() {
		return Optional.empty();
	}

	/**
	 * Implement a structural {@link RepositoryFragment} given its {@code implementation} object. Returns an implemented
	 * {@link RepositoryFragment}.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return a new implemented {@link RepositoryFragment} for {@code implementation}.
	 */
	public abstract RepositoryFragment<T> withImplementation(T implementation);

	/**
	 * @return a {@link Stream} of methods exposed by this {@link RepositoryFragment}.
	 */
	public Stream<Method> methods() {
		return Arrays.stream(getSignatureContributor().getMethods());
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode(callSuper = false)
	static class StructuralRepositoryFragment<T> extends RepositoryFragment<T> {

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
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode(callSuper = false)
	static class ImplementedRepositoryFragment<T> extends RepositoryFragment<T> {

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
	}
}
