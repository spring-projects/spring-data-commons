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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.ReactiveWrappers;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.MethodLookup.InvokedMethod;
import org.springframework.data.repository.core.support.RepositoryInvocationMulticaster.NoOpRepositoryInvocationMulticaster;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.Streamable;
import org.springframework.lang.CheckReturnValue;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Composite implementation to back repository method implementations.
 * <p>
 * A {@link RepositoryComposition} represents an ordered collection of {@link RepositoryFragment fragments}. Each
 * fragment contributes executable method signatures that are used by this composition to route method calls into the
 * according {@link RepositoryFragment}.
 * <p>
 * Fragments are allowed to contribute multiple implementations for a single method signature exposed through the
 * repository interface. {@link #withMethodLookup(MethodLookup) MethodLookup} selects the first matching method for
 * invocation. A composition also supports argument conversion between the repository method signature and fragment
 * implementation method through {@link #withArgumentConverter(BiFunction)}. Use argument conversion with a single
 * implementation method that can be exposed accepting convertible types.
 * <p>
 * Composition objects are immutable and thread-safe.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Johannes Englmeier
 * @soundtrack Masterboy - Anybody (Fj Gauder Mix)
 * @since 2.0
 * @see RepositoryFragment
 */
public class RepositoryComposition {

	private static final BiFunction<Method, Object[], Object[]> PASSTHRU_ARG_CONVERTER = (methodParameter, o) -> o;
	private static final BiFunction<Method, Object[], Object[]> REACTIVE_ARGS_CONVERTER = (method, args) -> {

		if (ReactiveWrappers.isAvailable()) {

			Class<?>[] parameterTypes = method.getParameterTypes();

			Object[] converted = new Object[args.length];
			for (int i = 0; i < args.length; i++) {

				Object value = args[i];
				Object convertedArg = value;

				Class<?> parameterType = parameterTypes.length > i ? parameterTypes[i] : null;

				if ((value != null) && (parameterType != null)) {
					if (!parameterType.isAssignableFrom(value.getClass())
							&& ReactiveWrapperConverters.canConvert(value.getClass(), parameterType)) {

						convertedArg = ReactiveWrapperConverters.toWrapper(value, parameterType);
					}
				}

				converted[i] = convertedArg;
			}

			return converted;
		}

		return args;
	};

	private static final RepositoryComposition EMPTY = new RepositoryComposition(null, RepositoryFragments.empty(),
			MethodLookups.direct(), PASSTHRU_ARG_CONVERTER);

	private final Map<Method, Method> methodCache = new ConcurrentHashMap<>();
	private final RepositoryFragments fragments;
	private final MethodLookup methodLookup;
	private final List<MethodLookup.MethodPredicate> lookups;
	private final BiFunction<Method, Object[], Object[]> argumentConverter;
	private final @Nullable RepositoryMetadata metadata;

	private RepositoryComposition(@Nullable RepositoryMetadata metadata, RepositoryFragments fragments,
			MethodLookup methodLookup, BiFunction<Method, Object[], Object[]> argumentConverter) {

		this.metadata = metadata;
		this.fragments = fragments;
		this.methodLookup = methodLookup;
		this.lookups = methodLookup.getLookups();
		this.argumentConverter = argumentConverter;
	}

	/**
	 * Create an empty {@link RepositoryComposition}.
	 *
	 * @return an empty {@link RepositoryComposition}.
	 */
	public static RepositoryComposition empty() {
		return EMPTY;
	}

	/**
	 * Create an {@link RepositoryComposition} using the provided {@link RepositoryMetadata} to set {@link MethodLookup
	 * method lookups} depending on the repository type (reactive/imperative).
	 *
	 * @return an empty {@link RepositoryComposition}.
	 * @since 2.4
	 */
	public static RepositoryComposition fromMetadata(RepositoryMetadata metadata) {

		if (metadata.isReactiveRepository()) {
			return new RepositoryComposition(metadata, RepositoryFragments.empty(), MethodLookups.forReactiveTypes(metadata),
					REACTIVE_ARGS_CONVERTER);
		}

		return new RepositoryComposition(metadata, RepositoryFragments.empty(), MethodLookups.forRepositoryTypes(metadata),
				PASSTHRU_ARG_CONVERTER);
	}

	/**
	 * Create a {@link RepositoryComposition} for just a single {@code implementation} with {@code MethodLookups#direct()}
	 * method lookup.
	 *
	 * @param implementation must not be {@literal null}.
	 * @return the {@link RepositoryComposition} for a single {@code implementation}.
	 */
	public static RepositoryComposition just(Object implementation) {
		return new RepositoryComposition(null, RepositoryFragments.just(implementation), MethodLookups.direct(),
				PASSTHRU_ARG_CONVERTER);
	}

	/**
	 * Create a {@link RepositoryComposition} from {@link RepositoryFragment fragments} with
	 * {@code MethodLookups#direct()} method lookup.
	 *
	 * @param fragments must not be {@literal null}.
	 * @return the {@link RepositoryComposition} from {@link RepositoryFragment fragments}.
	 */
	public static RepositoryComposition of(RepositoryFragment<?>... fragments) {
		return of(Arrays.asList(fragments));
	}

	/**
	 * Create a {@link RepositoryComposition} from {@link RepositoryFragment fragments} with
	 * {@code MethodLookups#direct()} method lookup.
	 *
	 * @param fragments must not be {@literal null}.
	 * @return the {@link RepositoryComposition} from {@link RepositoryFragment fragments}.
	 */
	public static RepositoryComposition of(List<RepositoryFragment<?>> fragments) {
		return new RepositoryComposition(null, RepositoryFragments.from(fragments), MethodLookups.direct(),
				PASSTHRU_ARG_CONVERTER);
	}

	/**
	 * Create a {@link RepositoryComposition} from {@link RepositoryFragments} and {@link RepositoryMetadata} with
	 * {@code MethodLookups#direct()} method lookup.
	 *
	 * @param fragments must not be {@literal null}.
	 * @return the {@link RepositoryComposition} from {@link RepositoryFragments fragments}.
	 */
	public static RepositoryComposition of(RepositoryFragments fragments) {
		return new RepositoryComposition(null, fragments, MethodLookups.direct(), PASSTHRU_ARG_CONVERTER);
	}

	/**
	 * Create a new {@link RepositoryComposition} retaining current configuration and append {@link RepositoryFragment} to
	 * the new composition. The resulting composition contains the appended {@link RepositoryFragment} as last element.
	 *
	 * @param fragment must not be {@literal null}.
	 * @return the new {@link RepositoryComposition}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public RepositoryComposition append(RepositoryFragment<?> fragment) {
		return new RepositoryComposition(metadata, fragments.append(fragment), methodLookup, argumentConverter);
	}

	/**
	 * Create a new {@link RepositoryComposition} retaining current configuration and append {@link RepositoryFragments}
	 * to the new composition. The resulting composition contains the appended {@link RepositoryFragments} as last
	 * element.
	 *
	 * @param fragments must not be {@literal null}.
	 * @return the new {@link RepositoryComposition}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public RepositoryComposition append(RepositoryFragments fragments) {
		return new RepositoryComposition(metadata, this.fragments.append(fragments), methodLookup, argumentConverter);
	}

	/**
	 * Create a new {@link RepositoryComposition} retaining current configuration and set {@code argumentConverter}.
	 *
	 * @param argumentConverter must not be {@literal null}.
	 * @return the new {@link RepositoryComposition}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public RepositoryComposition withArgumentConverter(BiFunction<Method, Object[], Object[]> argumentConverter) {
		return new RepositoryComposition(metadata, fragments, methodLookup, argumentConverter);
	}

	/**
	 * Create a new {@link RepositoryComposition} retaining current configuration and set {@code methodLookup}.
	 *
	 * @param methodLookup must not be {@literal null}.
	 * @return the new {@link RepositoryComposition}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public RepositoryComposition withMethodLookup(MethodLookup methodLookup) {
		return new RepositoryComposition(metadata, fragments, methodLookup, argumentConverter);
	}

	/**
	 * Create a new {@link RepositoryComposition} retaining current configuration and set {@code metadata}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @return new instance of {@link RepositoryComposition}.
	 * @since 2.4
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public RepositoryComposition withMetadata(RepositoryMetadata metadata) {
		return new RepositoryComposition(metadata, fragments, methodLookup, argumentConverter);
	}

	/**
	 * Return {@literal true} if this {@link RepositoryComposition} contains no {@link RepositoryFragment fragments}.
	 *
	 * @return {@literal true} if this {@link RepositoryComposition} contains no {@link RepositoryFragment fragments}.
	 */
	public boolean isEmpty() {
		return fragments.isEmpty();
	}

	/**
	 * Invoke a method on the repository by routing the invocation to the appropriate {@link RepositoryFragment}.
	 */
	public @Nullable Object invoke(Method method, Object... args) throws Throwable {
		return invoke(NoOpRepositoryInvocationMulticaster.INSTANCE, method, args);
	}

	/**
	 * Invoke a method on the repository by routing the invocation to the appropriate {@link RepositoryFragment}.
	 */
	@Nullable
	Object invoke(RepositoryInvocationMulticaster listener, Method method, Object[] args) throws Throwable {

		Method methodToCall = getMethod(method);

		if (methodToCall == null) {
			throw new IllegalArgumentException(String.format("No fragment found for method %s", method));
		}

		ReflectionUtils.makeAccessible(methodToCall);

		return fragments.invoke(metadata != null ? metadata.getRepositoryInterface() : method.getDeclaringClass(), listener,
				method, methodToCall, argumentConverter.apply(methodToCall, args));
	}

	/**
	 * Find the {@link RepositoryFragment} for the given {@link Method} invoked on the composite interface.
	 *
	 * @param method must not be {@literal null}.
	 * @return the fragment implementing that method or {@literal null} if not found.
	 */
	public @Nullable RepositoryFragment<?> findFragment(Method method) {

		Method methodToCall = getMethod(method);

		if (methodToCall != null) {

			return fragments.stream().filter(it -> it.hasMethod(methodToCall)) //
					.findFirst().orElse(null);
		}

		return null;
	}

	/**
	 * Find the implementation method for the given {@link Method} invoked on the composite interface.
	 *
	 * @param method must not be {@literal null}.
	 */
	public Optional<Method> findMethod(Method method) {
		return Optional.ofNullable(getMethod(method));
	}

	/**
	 * Find the implementation method for the given {@link Method} invoked on the composite interface.
	 *
	 * @param method must not be {@literal null}.
	 * @since 2.2
	 */
	@Nullable
	Method getMethod(Method method) {

		return methodCache.computeIfAbsent(method,
				key -> RepositoryFragments.findMethod(InvokedMethod.of(key), lookups, fragments));
	}

	/**
	 * Validates that all {@link RepositoryFragment fragments} have an implementation.
	 */
	public void validateImplementation() {

		fragments.stream().forEach(it -> it.getImplementation() //
				.orElseThrow(() -> {
					Class<?> repositoryInterface = metadata != null ? metadata.getRepositoryInterface() : Object.class;
					return new FragmentNotImplementedException(String.format("Fragment %s used in %s has no implementation",
							ClassUtils.getQualifiedName(it.getSignatureContributor()),
							ClassUtils.getQualifiedName(repositoryInterface)), repositoryInterface, it);
				}));
	}

	public RepositoryFragments getFragments() {
		return this.fragments;
	}

	public MethodLookup getMethodLookup() {
		return this.methodLookup;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof RepositoryComposition that)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(fragments, that.fragments);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(fragments);
	}

	public BiFunction<Method, Object[], Object[]> getArgumentConverter() {
		return this.argumentConverter;
	}

	/**
	 * Value object representing an ordered list of {@link RepositoryFragment fragments}.
	 *
	 * @author Mark Paluch
	 * @author Christoph Strobl
	 */
	public static class RepositoryFragments implements Streamable<RepositoryFragment<?>> {

		static final RepositoryFragments EMPTY = new RepositoryFragments(Collections.emptyList());

		private final Map<Method, RepositoryFragment<?>> fragmentCache = new ConcurrentHashMap<>();
		private final Map<Method, RepositoryMethodInvoker> invocationMetadataCache = new ConcurrentHashMap<>();
		private final List<RepositoryFragment<?>> fragments;

		private RepositoryFragments(List<RepositoryFragment<?>> fragments) {
			this.fragments = fragments;
		}

		/**
		 * Create empty {@link RepositoryFragments}.
		 *
		 * @return empty {@link RepositoryFragments}.
		 */
		public static RepositoryFragments empty() {
			return EMPTY;
		}

		/**
		 * Create {@link RepositoryFragments} from just implementation objects.
		 *
		 * @param implementations must not be {@literal null}.
		 * @return the {@link RepositoryFragments} for {@code implementations}.
		 */
		public static RepositoryFragments just(Object... implementations) {

			Assert.notNull(implementations, "Implementations must not be null");
			Assert.noNullElements(implementations, "Implementations must not contain null elements");

			return new RepositoryFragments(
					Arrays.stream(implementations).map(RepositoryFragment::implemented).collect(Collectors.toList()));
		}

		/**
		 * Create {@link RepositoryFragments} from {@link RepositoryFragments fragments}.
		 *
		 * @param fragments must not be {@literal null}.
		 * @return the {@link RepositoryFragments} for {@code implementations}.
		 */
		public static RepositoryFragments of(RepositoryFragment<?>... fragments) {

			Assert.notNull(fragments, "RepositoryFragments must not be null");
			Assert.noNullElements(fragments, "RepositoryFragments must not contain null elements");

			return new RepositoryFragments(Arrays.asList(fragments));
		}

		/**
		 * Create {@link RepositoryFragments} from a {@link Collection} of {@link RepositoryFragment fragments}.
		 *
		 * @param fragments must not be {@literal null}.
		 * @return the {@link RepositoryFragments} for {@code implementations}.
		 */
		public static RepositoryFragments from(Collection<RepositoryFragment<?>> fragments) {

			Assert.notNull(fragments, "RepositoryFragments must not be null");

			return new RepositoryFragments(new ArrayList<>(fragments));
		}

		@Override
		public boolean isEmpty() {
			return this.fragments.isEmpty();
		}

		/**
		 * Create new {@link RepositoryFragments} from the current content appending {@link RepositoryFragment}.
		 *
		 * @param fragment must not be {@literal null}
		 * @return the new {@link RepositoryFragments} containing all existing fragments and the given
		 *         {@link RepositoryFragment} as last element.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public RepositoryFragments append(RepositoryFragment<?> fragment) {

			Assert.notNull(fragment, "RepositoryFragment must not be null");

			return concat(stream(), Stream.of(fragment));
		}

		/**
		 * Create new {@link RepositoryFragments} from the current content appending {@link RepositoryFragments}.
		 *
		 * @param fragments must not be {@literal null}
		 * @return the new {@link RepositoryFragments} containing all existing fragments and the given
		 *         {@link RepositoryFragments} as last elements.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public RepositoryFragments append(RepositoryFragments fragments) {

			Assert.notNull(fragments, "RepositoryFragments must not be null");

			if (fragments.isEmpty()) {
				return this;
			}

			return concat(stream(), fragments.stream());
		}

		private static RepositoryFragments concat(Stream<RepositoryFragment<?>> left, Stream<RepositoryFragment<?>> right) {
			return from(Stream.concat(left, right).collect(Collectors.toList()));
		}

		@Override
		public Iterator<RepositoryFragment<?>> iterator() {
			return fragments.iterator();
		}

		@Nullable
		RepositoryFragment<?> findFragment(Method methodToCall) {
			return fragmentCache.computeIfAbsent(methodToCall, this::findImplementationFragment);
		}

		/**
		 * @return {@link Stream} of {@link Method methods}.
		 */
		public Stream<Method> methods() {
			return stream().flatMap(RepositoryFragment::methods);
		}

		/**
		 * Invoke {@link Method} by resolving the fragment that implements a suitable method.
		 */
		@Deprecated(forRemoval = true)
		@SuppressWarnings("NullAway")
		public @Nullable Object invoke(Method invokedMethod, Method methodToCall, Object[] args) throws Throwable {
			return invoke(null, NoOpRepositoryInvocationMulticaster.INSTANCE, invokedMethod, methodToCall, args);
		}

		/**
		 * Invoke {@link Method} by resolving the fragment that implements a suitable method.
		 */
		@Nullable
		Object invoke(@Nullable Class<?> repositoryInterface, RepositoryInvocationMulticaster listener,
				Method invokedMethod, Method methodToCall, Object[] args) throws Throwable {

			RepositoryFragment<?> fragment = fragmentCache.computeIfAbsent(methodToCall, this::findImplementationFragment);
			Optional<?> optional = fragment.getImplementation();

			if (optional.isEmpty()) {
				throw new IllegalArgumentException(String.format("No implementation found for method %s", methodToCall));
			}

			RepositoryMethodInvoker repositoryMethodInvoker = invocationMetadataCache.get(invokedMethod);

			if (repositoryMethodInvoker == null) {

				repositoryMethodInvoker = RepositoryMethodInvoker.forFragmentMethod(invokedMethod, optional.get(),
						methodToCall);
				invocationMetadataCache.put(invokedMethod, repositoryMethodInvoker);
			}

			Assert.notNull(repositoryInterface, "Repository interface must not be null");

			return repositoryMethodInvoker.invoke(repositoryInterface, listener, args);
		}

		private RepositoryFragment<?> findImplementationFragment(Method key) {

			return stream().filter(it -> it.hasMethod(key)) //
					.filter(it -> it.getImplementation().isPresent()) //
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(String.format("No fragment found for method %s", key)));
		}

		private static @Nullable Method findMethod(InvokedMethod invokedMethod, List<MethodLookup.MethodPredicate> lookups,
				RepositoryFragments fragments) {

			for (MethodLookup.MethodPredicate methodPredicate : lookups) {
				for (RepositoryFragment<?> fragment : fragments) {
					for (Method candidate : fragment.findMethods(invokedMethod.getName())) {
						if (methodPredicate.test(invokedMethod, candidate)) {
							return candidate;
						}
					}
				}
			}

			return null;
		}

		List<RepositoryFragment<?>> getFragments() {
			return fragments;
		}

		/**
		 * Returns the number of {@link RepositoryFragment fragments} available.
		 *
		 * @return the number of {@link RepositoryFragment fragments}.
		 * @since 2.5
		 */
		public int size() {
			return fragments.size();
		}

		@Override
		public String toString() {
			return fragments.toString();
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof RepositoryFragments that)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(fragmentCache, that.fragmentCache)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(invocationMetadataCache, that.invocationMetadataCache)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(fragments, that.fragments);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHash(fragmentCache, invocationMetadataCache, fragmentCache);
		}

	}
}
