/*
 * Copyright 2017-2021 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
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
	static <T> RepositoryFragment<T> implemented(T implementation) {
		return new ImplementedRepositoryFragment<T>(Optional.empty(), implementation);
	}

	/**
	 * Create an implemented {@link RepositoryFragment} from a {@code interfaceClass} backed by the {@code implementation}
	 * object.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return
	 */
	static <T> RepositoryFragment<T> implemented(Class<T> interfaceClass, T implementation) {
		return new ImplementedRepositoryFragment<>(Optional.of(interfaceClass), implementation);
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

	class StructuralRepositoryFragment<T> implements RepositoryFragment<T> {

		private final Class<T> interfaceOrImplementation;

		public StructuralRepositoryFragment(Class<T> interfaceOrImplementation) {
			this.interfaceOrImplementation = interfaceOrImplementation;
		}

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

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("StructuralRepositoryFragment %s", ClassUtils.getShortName(interfaceOrImplementation));
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof StructuralRepositoryFragment)) {
				return false;
			}

			StructuralRepositoryFragment<?> that = (StructuralRepositoryFragment<?>) o;
			return ObjectUtils.nullSafeEquals(interfaceOrImplementation, that.interfaceOrImplementation);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(interfaceOrImplementation);
		}
	}

	class ImplementedRepositoryFragment<T> implements RepositoryFragment<T> {

		private final Optional<Class<T>> interfaceClass;
		private final T implementation;
		private final Optional<T> optionalImplementation;

		/**
		 * Creates a new {@link ImplementedRepositoryFragment} for the given interface class and implementation.
		 *
		 * @param interfaceClass must not be {@literal null}.
		 * @param implementation must not be {@literal null}.
		 */
		public ImplementedRepositoryFragment(Optional<Class<T>> interfaceClass, T implementation) {

			Assert.notNull(interfaceClass, "Interface class must not be null!");
			Assert.notNull(implementation, "Implementation object must not be null!");

			interfaceClass.ifPresent(it -> {

				Assert.isTrue(ClassUtils.isAssignableValue(it, implementation),
						() -> String.format("Fragment implementation %s does not implement %s!",
								ClassUtils.getQualifiedName(implementation.getClass()), ClassUtils.getQualifiedName(it)));
			});

			this.interfaceClass = interfaceClass;
			this.implementation = implementation;
			this.optionalImplementation = Optional.of(implementation);
		}

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
			return optionalImplementation;
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

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof ImplementedRepositoryFragment)) {
				return false;
			}

			ImplementedRepositoryFragment<?> that = (ImplementedRepositoryFragment<?>) o;

			if (!ObjectUtils.nullSafeEquals(interfaceClass, that.interfaceClass)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(implementation, that.implementation)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(optionalImplementation, that.optionalImplementation);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(interfaceClass);
			result = 31 * result + ObjectUtils.nullSafeHashCode(implementation);
			result = 31 * result + ObjectUtils.nullSafeHashCode(optionalImplementation);
			return result;
		}
	}
}
