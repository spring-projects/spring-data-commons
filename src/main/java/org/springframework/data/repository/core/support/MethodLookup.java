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
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Strategy interface providing {@link MethodPredicate predicates} to resolve a method called on a composite to its
 * implementation method.
 * <p>
 * {@link MethodPredicate Predicates} are ordered by filtering priority and applied individually. If a predicate does
 * not yield any positive match, the next predicate is applied.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see RepositoryComposition
 */
@FunctionalInterface
public interface MethodLookup {

	/**
	 * Return an ordered {@link List} of {@link MethodPredicate}. Each predicate is applied individually. If any
	 * {@link MethodPredicate} matches, the tested candidate {@link Method} passes the filter.
	 *
	 * @return {@link List} of {@link MethodPredicate}.
	 */
	List<MethodPredicate> getLookups();

	/**
	 * Returns a composed {@link MethodLookup} that represents a concatenation of this predicate and another. When
	 * evaluating the composed method lookup, if this lookup evaluates {@code true}, then the {@code other} method lookup
	 * is not evaluated.
	 *
	 * @param other must not be {@literal null}.
	 * @return the composed {@link MethodLookup}.
	 */
	default MethodLookup and(MethodLookup other) {

		Assert.notNull(other, "Other method lookup must not be null!");

		return () -> Stream.concat(getLookups().stream(), other.getLookups().stream()).collect(Collectors.toList());
	}

	/**
	 * A method predicate to be applied on the {@link InvokedMethod} and {@link Method method candidate}.
	 */
	@FunctionalInterface
	interface MethodPredicate extends BiPredicate<InvokedMethod, Method> {

		@Override
		@SuppressWarnings("null")
		boolean test(InvokedMethod invokedMethod, Method candidate);
	}

	/**
	 * Value object representing an invoked {@link Method}.
	 */
	final
	class InvokedMethod {

		private final Method method;

		private InvokedMethod(Method method) {
			this.method = method;
		}

		public static InvokedMethod of(Method method) {
			return new InvokedMethod(method);
		}

		public Class<?> getDeclaringClass() {
			return method.getDeclaringClass();
		}

		public String getName() {
			return method.getName();
		}

		public Class<?>[] getParameterTypes() {
			return method.getParameterTypes();
		}

		public int getParameterCount() {
			return method.getParameterCount();
		}

		public Method getMethod() {
			return this.method;
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

			if (!(o instanceof InvokedMethod)) {
				return false;
			}

			InvokedMethod that = (InvokedMethod) o;
			return ObjectUtils.nullSafeEquals(method, that.method);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(method);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "MethodLookup.InvokedMethod(method=" + this.getMethod() + ")";
		}
	}
}
