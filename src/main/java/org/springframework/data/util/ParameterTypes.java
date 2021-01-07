/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.TypeUtils;

/**
 * Abstraction over a list of parameter value types. Allows to check whether a list of parameter values with the given
 * type setup is a candidate for the invocation of a given {@link Method} (see {@link #areValidFor(Method)}). This is
 * necessary to properly match parameter values against methods declaring varargs arguments. The implementation favors
 * direct matches and only computes the alternative sets of types to be considered if the primary one doesn't match.
 *
 * @author Oliver Drotbohm
 * @since 2.1.7
 * @soundtrack Signs, High Times - Tedeschi Trucks Band (Signs)
 */
public class ParameterTypes {

	private static final TypeDescriptor OBJECT_DESCRIPTOR = TypeDescriptor.valueOf(Object.class);
	private static final ConcurrentMap<List<TypeDescriptor>, ParameterTypes> cache = new ConcurrentReferenceHashMap<>();

	private final List<TypeDescriptor> types;
	private final Lazy<Collection<ParameterTypes>> alternatives;

	/**
	 * Creates a new {@link ParameterTypes} for the given types.
	 *
	 * @param types
	 */
	private ParameterTypes(List<TypeDescriptor> types) {

		this.types = types;
		this.alternatives = Lazy.of(() -> getAlternatives());
	}

	public ParameterTypes(List<TypeDescriptor> types, Lazy<Collection<ParameterTypes>> alternatives) {
		this.types = types;
		this.alternatives = alternatives;
	}

	/**
	 * Returns the {@link ParameterTypes} for the given list of {@link TypeDescriptor}s.
	 *
	 * @param types must not be {@literal null}.
	 * @return
	 */
	public static ParameterTypes of(List<TypeDescriptor> types) {

		Assert.notNull(types, "Types must not be null!");

		return cache.computeIfAbsent(types, ParameterTypes::new);
	}

	/**
	 * Returns the {@link ParameterTypes} for the given {@link Class}es.
	 *
	 * @param types must not be {@literal null}.
	 * @return
	 */
	static ParameterTypes of(Class<?>... types) {

		Assert.notNull(types, "Types must not be null!");
		Assert.noNullElements(types, "Types must not have null elements!");

		return of(Arrays.stream(types) //
				.map(TypeDescriptor::valueOf) //
				.collect(Collectors.toList()));
	}

	/**
	 * Returns whether the parameter types are valid for the given {@link Method}. That means, a parameter value list with
	 * the given type arrangement is a valid list to invoke the given method.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	public boolean areValidFor(Method method) {

		Assert.notNull(method, "Method must not be null!");

		// Direct matches
		if (areValidTypes(method)) {
			return true;
		}

		return hasValidAlternativeFor(method);
	}

	/**
	 * Returns whether we have a valid alternative variant (making use of varargs) that will match the given method's
	 * signature.
	 *
	 * @param method
	 * @return
	 */
	private boolean hasValidAlternativeFor(Method method) {

		return alternatives.get().stream().anyMatch(it -> it.areValidTypes(method)) //
				|| getParent().map(parent -> parent.hasValidAlternativeFor(method)).orElse(false);
	}

	/**
	 * Returns all suitable alternatives to the current {@link ParameterTypes}.
	 *
	 * @return will never be {@literal null}.
	 */
	List<ParameterTypes> getAllAlternatives() {

		List<ParameterTypes> result = new ArrayList<>();
		result.addAll(alternatives.get());

		getParent().ifPresent(it -> result.addAll(it.getAllAlternatives()));

		return result;
	}

	/**
	 * Returns whether the {@link ParameterTypes} consists of the given types.
	 *
	 * @param types must not be {@literal null}.
	 * @return
	 */
	boolean hasTypes(Class<?>... types) {

		Assert.notNull(types, "Types must not be null!");

		return Arrays.stream(types) //
				.map(TypeDescriptor::valueOf) //
				.collect(Collectors.toList())//
				.equals(this.types);
	}

	/**
	 * Returns whether the current parameter types match the given {@link Method}'s parameters exactly, i.e. they're
	 * equal, not only assignable.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	public boolean exactlyMatchParametersOf(Method method) {

		if (method.getParameterCount() != types.size()) {
			return false;
		}

		Class<?>[] parameterTypes = method.getParameterTypes();

		for (int i = 0; i < parameterTypes.length; i++) {
			if (parameterTypes[i] != types.get(i).getType()) {
				return false;
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return types.stream() //
				.map(TypeDescriptor::getType) //
				.map(Class::getSimpleName) //
				.collect(Collectors.joining(", ", "(", ")"));
	}

	protected Optional<ParameterTypes> getParent() {
		return types.isEmpty() ? Optional.empty() : getParent(getTail());
	}

	protected final Optional<ParameterTypes> getParent(TypeDescriptor tail) {

		return types.size() <= 1 //
				? Optional.empty() //
				: Optional.of(ParentParameterTypes.of(types.subList(0, types.size() - 1), tail));
	}

	protected Optional<ParameterTypes> withLastVarArgs() {

		TypeDescriptor lastDescriptor = types.get(types.size() - 1);

		return lastDescriptor.isArray() //
				? Optional.empty() //
				: Optional.ofNullable(withVarArgs(lastDescriptor));
	}

	@SuppressWarnings("null")
	private ParameterTypes withVarArgs(TypeDescriptor descriptor) {

		TypeDescriptor lastDescriptor = types.get(types.size() - 1);

		if (lastDescriptor.isArray() && lastDescriptor.getElementTypeDescriptor().equals(descriptor)) {
			return this;
		}

		List<TypeDescriptor> result = new ArrayList<>(types.subList(0, types.size() - 1));
		result.add(TypeDescriptor.array(descriptor));

		return ParameterTypes.of(result);
	}

	private Collection<ParameterTypes> getAlternatives() {

		if (types.isEmpty()) {
			return Collections.emptyList();
		}

		List<ParameterTypes> alternatives = new ArrayList<>();

		withLastVarArgs().ifPresent(alternatives::add);

		ParameterTypes objectVarArgs = withVarArgs(OBJECT_DESCRIPTOR);

		if (!alternatives.contains(objectVarArgs)) {
			alternatives.add(objectVarArgs);
		}

		return alternatives;
	}

	/**
	 * Returns whether the current type list makes up valid arguments for the given method.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private boolean areValidTypes(Method method) {

		Assert.notNull(method, "Method must not be null!");

		if (method.getParameterCount() != types.size()) {
			return false;
		}

		Class<?>[] parameterTypes = method.getParameterTypes();

		for (int i = 0; i < parameterTypes.length; i++) {
			if (!TypeUtils.isAssignable(parameterTypes[i], types.get(i).getType())) {
				return false;
			}
		}

		return true;
	}

	private TypeDescriptor getTail() {
		return types.get(types.size() - 1);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof ParameterTypes)) {
			return false;
		}

		ParameterTypes that = (ParameterTypes) o;

		return ObjectUtils.nullSafeEquals(types, that.types);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(types);
	}

	/**
	 * Extension of {@link ParameterTypes} that remembers the seed tail and only adds typed varargs if the current tail is
	 * assignable to the seed one.
	 *
	 * @author Oliver Drotbohm
	 */
	static class ParentParameterTypes extends ParameterTypes {

		private final TypeDescriptor tail;

		private ParentParameterTypes(List<TypeDescriptor> types, TypeDescriptor tail) {

			super(types);
			this.tail = tail;
		}

		public static ParentParameterTypes of(List<TypeDescriptor> types, TypeDescriptor tail) {
			return new ParentParameterTypes(types, tail);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ParameterTypes#getParent()
		 */
		@Override
		protected Optional<ParameterTypes> getParent() {
			return super.getParent(tail);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ParameterTypes#withLastVarArgs()
		 */
		@Override
		protected Optional<ParameterTypes> withLastVarArgs() {

			return !tail.isAssignableTo(super.getTail()) //
					? Optional.empty() //
					: super.withLastVarArgs();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof ParentParameterTypes)) {
				return false;
			}

			if (!super.equals(o)) {
				return false;
			}

			ParentParameterTypes that = (ParentParameterTypes) o;

			return ObjectUtils.nullSafeEquals(tail, that.tail);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = super.hashCode();

			result = 31 * result + ObjectUtils.nullSafeHashCode(tail);

			return result;
		}
	}
}
