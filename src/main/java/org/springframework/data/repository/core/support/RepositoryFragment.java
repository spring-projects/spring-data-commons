/*
 * Copyright 2017-2025 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Value object representing a repository fragment.
 * <p>
 * Repository fragments are individual parts that contribute method signatures. They are used to form a
 * {@link RepositoryComposition}. Fragments can be purely structural or backed with an implementation.
 * <p>
 * {@link #structural(Class) Structural} fragments are not backed by an implementation and are primarily used to
 * discover the structure of a repository composition and to perform validations.
 * <p>
 * {@link #implemented(Object) Implemented} repository fragments consist of a signature contributor and the implementing
 * object. A signature contributor may be {@link #implemented(Class, Object) an interface} or
 * {@link #implemented(Object) just the implementing object} providing the available signatures for a repository.
 * <p>
 * Fragments are immutable.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
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
	static <T> RepositoryFragment<T> implemented(T implementation) {
		return new ImplementedRepositoryFragment<>((Class<T>) null, implementation);
	}

	/**
	 * Create an implemented {@link RepositoryFragment} from a {@code interfaceClass} backed by the {@code implementation}
	 * object.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return
	 */
	static <T> RepositoryFragment<T> implemented(Class<T> interfaceClass, T implementation) {
		return new ImplementedRepositoryFragment<>(interfaceClass, implementation);
	}

	/**
	 * Create a structural {@link RepositoryFragment} given {@code interfaceOrImplementation}.
	 *
	 * @param interfaceOrImplementation must not be {@literal null}.
	 * @return
	 */
	static <T> RepositoryFragment<T> structural(Class<T> interfaceOrImplementation) {
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

		Assert.notNull(method, "Method must not be null");
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

	class StructuralRepositoryFragment<T> implements RepositoryFragment<T> {

		private final Class<T> interfaceOrImplementation;

		public StructuralRepositoryFragment(Class<T> interfaceOrImplementation) {
			this.interfaceOrImplementation = interfaceOrImplementation;
		}

		@Override
		public Class<?> getSignatureContributor() {
			return interfaceOrImplementation;
		}

		@Override
		public RepositoryFragment<T> withImplementation(T implementation) {
			return new ImplementedRepositoryFragment<>(interfaceOrImplementation, implementation);
		}

		@Override
		public String toString() {
			return String.format("StructuralRepositoryFragment %s", ClassUtils.getShortName(interfaceOrImplementation));
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof StructuralRepositoryFragment<?> that)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(interfaceOrImplementation, that.interfaceOrImplementation);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(interfaceOrImplementation);
		}
	}

	class ImplementedRepositoryFragment<T> implements RepositoryFragment<T> {

		private final @Nullable Class<T> interfaceClass;
		private final T implementation;

		/**
		 * Creates a new {@link ImplementedRepositoryFragment} for the given interface class and implementation.
		 *
		 * @param interfaceClass
		 * @param implementation
		 * @deprecated since 3.4 - use {@link ImplementedRepositoryFragment(Class, Object)} instead.
		 */
		@Deprecated(since = "3.4", forRemoval = true)
		public ImplementedRepositoryFragment(Optional<Class<T>> interfaceClass, T implementation) {
			this(interfaceClass.orElse(null), implementation);
		}

		/**
		 * Creates a new {@link ImplementedRepositoryFragment} for the given interface class and implementation.
		 *
		 * @param interfaceClass must not be {@literal null}.
		 * @param implementation must not be {@literal null}.
		 */
		public ImplementedRepositoryFragment(@Nullable Class<T> interfaceClass, T implementation) {

			Assert.notNull(implementation, "Implementation object must not be null");

			if (interfaceClass != null) {

				Assert.isTrue(ClassUtils.isAssignableValue(interfaceClass, implementation),
					() -> "Fragment implementation %s does not implement %s".formatted(
							ClassUtils.getQualifiedName(implementation.getClass()),
							ClassUtils.getQualifiedName(interfaceClass)));
			}

			this.interfaceClass = interfaceClass;
			this.implementation = implementation;
		}

		@Override
		public Class<?> getSignatureContributor() {

			if (interfaceClass != null) {
				return interfaceClass;
			}

			if (implementation instanceof Class<?> type) {
				return type;
			}
			return implementation.getClass();
		}

		@Override
		public Optional<T> getImplementation() {
			return Optional.of(implementation);
		}

		@Override
		public RepositoryFragment<T> withImplementation(T implementation) {
			return new ImplementedRepositoryFragment<>(interfaceClass, implementation);
		}

		@Override
		public String toString() {

			return String.format("ImplementedRepositoryFragment %s%s",
					interfaceClass != null ? (ClassUtils.getShortName(interfaceClass) + ":") : "",
					ClassUtils.getShortName(implementation.getClass()));
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof ImplementedRepositoryFragment<?> that)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(interfaceClass, that.interfaceClass)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(implementation, that.implementation);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(interfaceClass);
			result = 31 * result + ObjectUtils.nullSafeHashCode(implementation);
			return result;
		}
	}
}
