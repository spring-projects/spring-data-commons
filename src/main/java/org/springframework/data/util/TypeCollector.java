/*
 * Copyright 2019-2025 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.aot.AotServices;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Contract;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Collector to inspect domain types and discover the type graph that is relevant for Spring Data operations.
 * <p>
 * Type inspection walks through all class members (fields, methods, constructors) and introspects those for additional
 * types that are part of the domain model.
 * <p>
 * Type collection can be customized by providing filters that stop introspection when encountering a {@link Predicate}
 * that returns {@code false}. Filters are {@link Predicate#and(Predicate) combined} so that multiple filters can be
 * taken into account. A type/field/method must pass all filters to be considered for further inspection.
 * <p>
 * The collector uses {@link AotServices} to discover implementations of {@link TypeCollectorFilters} so that
 * components using {@link TypeCollector} can contribute their own filtering logic to exclude types, fields, and methods
 * from being inspected.
 *
 * @author Christoph Strobl
 * @author Sebastien Deleuze
 * @author John Blum
 * @author Mark Paluch
 * @since 3.0
 */
public class TypeCollector {

	private static final Log logger = LogFactory.getLog(TypeCollector.class);

	private static final AotServices<TypeCollectorFilters> providers = AotServices.factories()
			.load(TypeCollectorFilters.class);

	private Predicate<Class<?>> typeFilter = Predicates.isTrue();

	private Predicate<Method> methodFilter = Predicates.isTrue();

	private Predicate<Field> fieldFilter = Predicates.isTrue();

	/**
	 * Create a new {@link TypeCollector} applying all {@link TypeCollectorFilters} discovered through
	 * {@link AotServices}.
	 */
	public TypeCollector() {

		providers.forEach(provider -> {
			filterTypes(provider.classPredicate());
			filterMethods(provider.methodPredicate());
			filterFields(provider.fieldPredicate());
		});
	}

	/**
	 * Add a filter to exclude types from being introspected.
	 *
	 * @param filter filter predicate matching a {@link Class}.
	 * @return {@code this} TypeCollector instance.
	 */
	@Contract("_ -> this")
	public TypeCollector filterTypes(Predicate<Class<?>> filter) {
		this.typeFilter = this.typeFilter.and(filter);
		return this;
	}

	/**
	 * Add a filter to exclude methods from being introspected.
	 *
	 * @param filter filter predicate matching a {@link Class}.
	 * @return {@code this} TypeCollector instance.
	 * @since 4.0
	 */
	@Contract("_ -> this")
	public TypeCollector filterMethods(Predicate<Method> filter) {
		this.methodFilter = methodFilter.and(filter);
		return this;
	}

	/**
	 * Add a filter to exclude fields from being introspected.
	 *
	 * @param filter filter predicate matching a {@link Class}.
	 * @return {@code this} TypeCollector instance.
	 * @since 4.0
	 */
	@Contract("_ -> this")
	public TypeCollector filterFields(Predicate<Field> filter) {
		this.fieldFilter = fieldFilter.and(filter);
		return this;
	}

	/**
	 * Inspect the given type and resolve those reachable via fields, methods, generics, ...
	 *
	 * @param types the types to inspect
	 * @return a type model collector for the type
	 */
	public static ReachableTypes inspect(Class<?>... types) {
		return inspect(Arrays.asList(types));
	}

	/**
	 * Inspect the given type and resolve those reachable via fields, methods, generics, ...
	 *
	 * @param types the types to inspect
	 * @return a type model collector for the type
	 */
	public static ReachableTypes inspect(Collection<Class<?>> types) {
		return inspect(it -> {}, types);
	}

	/**
	 * Inspect the given type and resolve those reachable via fields, methods, generics, ...
	 *
	 * @param collectorCustomizer the customizer function to configure the {@link TypeCollector}.
	 * @param types the types to inspect.
	 * @return a type model collector for the type.
	 * @since 4.0
	 */
	public static ReachableTypes inspect(Consumer<TypeCollector> collectorCustomizer, Class<?>... types) {
		return inspect(collectorCustomizer, Arrays.asList(types));
	}

	/**
	 * Inspect the given type and resolve those reachable via fields, methods, generics, ...
	 *
	 * @param collectorCustomizer the customizer function to configure the {@link TypeCollector}.
	 * @param types the types to inspect.
	 * @return a type model collector for the type.
	 * @since 4.0
	 */
	public static ReachableTypes inspect(Consumer<TypeCollector> collectorCustomizer, Collection<Class<?>> types) {
		TypeCollector typeCollector = new TypeCollector();
		collectorCustomizer.accept(typeCollector);
		return new ReachableTypes(typeCollector, types);
	}

	private void process(Class<?> root, Consumer<ResolvableType> consumer) {
		processType(ResolvableType.forType(root), new InspectionCache(), consumer);
	}

	private void processType(ResolvableType type, InspectionCache cache, Consumer<ResolvableType> callback) {

		if (ResolvableType.NONE.equals(type) || cache.contains(type) || type.toClass().isSynthetic()) {
			return;
		}

		cache.add(type);

		// continue inspection but only add those matching the filter criteria to the result
		if (typeFilter.test(type.toClass())) {
			callback.accept(type);
		}

		Set<Type> additionalTypes = new LinkedHashSet<>();
		additionalTypes.addAll(TypeUtils.resolveTypesInSignature(type));
		additionalTypes.addAll(visitConstructorsOfType(type));
		additionalTypes.addAll(visitMethodsOfType(type));
		additionalTypes.addAll(visitFieldsOfType(type));

		if (!ObjectUtils.isEmpty(type.toClass().getDeclaredClasses())) {
			additionalTypes.addAll(Arrays.asList(type.toClass().getDeclaredClasses()));
		}

		for (Type discoveredType : additionalTypes) {
			processType(ResolvableType.forType(discoveredType, type), cache, callback);
		}
	}

	private Set<Type> visitConstructorsOfType(ResolvableType type) {

		if (!typeFilter.test(type.toClass())) {
			return Collections.emptySet();
		}

		Set<Type> discoveredTypes = new LinkedHashSet<>();

		for (Constructor<?> constructor : type.toClass().getDeclaredConstructors()) {

			if (Predicates.isExcluded(constructor)) {
				continue;
			}
			for (Class<?> signatureType : TypeUtils.resolveTypesInSignature(type.toClass(), constructor)) {
				if (typeFilter.test(signatureType)) {
					discoveredTypes.add(signatureType);
				}
			}
		}

		return new HashSet<>(discoveredTypes);
	}

	private Set<Type> visitMethodsOfType(ResolvableType type) {

		if (!typeFilter.test(type.toClass())) {
			return Collections.emptySet();
		}

		Set<Type> discoveredTypes = new LinkedHashSet<>();
		try {
			ReflectionUtils.doWithLocalMethods(type.toClass(), method -> {
				if (!methodFilter.test(method)) {
					return;
				}
				for (Class<?> signatureType : TypeUtils.resolveTypesInSignature(type.toClass(), method)) {
					if (typeFilter.test(signatureType)) {
						discoveredTypes.add(signatureType);
					}
				}
			});
		} catch (Exception cause) {
			logger.warn(cause);
		}

		return new HashSet<>(discoveredTypes);
	}

	private Set<Type> visitFieldsOfType(ResolvableType type) {

		Set<Type> discoveredTypes = new LinkedHashSet<>();

		ReflectionUtils.doWithLocalFields(type.toClass(), field -> {
			if (!fieldFilter.test(field)) {
				return;
			}
			for (Class<?> signatureType : TypeUtils.resolveTypesInSignature(ResolvableType.forField(field, type))) {
				if (typeFilter.test(signatureType)) {
					discoveredTypes.add(signatureType);
				}
			}
		});

		return discoveredTypes;
	}

	/**
	 * Container for reachable types starting from a set of root types.
	 */
	public static class ReachableTypes {

		private final Iterable<Class<?>> roots;
		private final Lazy<List<Class<?>>> reachableTypes = Lazy.of(this::collect);
		private final TypeCollector typeCollector;

		ReachableTypes(TypeCollector typeCollector, Iterable<Class<?>> roots) {

			this.typeCollector = typeCollector;
			this.roots = roots;
		}

		/**
		 * Performs the given action for each element of the reachable types until all elements have been processed or the
		 * action throws an exception. Actions are performed in the order of iteration, if that order is specified.
		 * Exceptions thrown by the action are relayed to the caller.
		 *
		 * @param action The action to be performed for each element
		 */
		public void forEach(Consumer<ResolvableType> action) {
			roots.forEach(it -> typeCollector.process(it, action));
		}

		/**
		 * Return all reachable types as list of {@link Class classes}. The resulting list is unmodifiable.
		 *
		 * @return an unmodifiable list of reachable types.
		 */
		public List<Class<?>> list() {
			return reachableTypes.get();
		}

		private List<Class<?>> collect() {
			List<Class<?>> target = new ArrayList<>();
			forEach(it -> target.add(it.toClass()));
			return List.copyOf(target);
		}

	}

	static class InspectionCache {

		private final Map<String, ResolvableType> mutableCache = new HashMap<>();

		public void add(ResolvableType resolvableType) {
			mutableCache.put(resolvableType.toString(), resolvableType);
		}

		public void clear() {
			mutableCache.clear();
		}

		public boolean contains(ResolvableType key) {
			return mutableCache.containsKey(key.toString());
		}

		public boolean isEmpty() {
			return mutableCache.isEmpty();
		}

		public int size() {
			return mutableCache.size();
		}

	}

	/**
	 * Strategy interface providing predicates to filter types, fields, and methods from being introspected and
	 * contributed to AOT processing.
	 * <p>
	 * {@code BeanRegistrationAotProcessor} implementations must be registered in a
	 * {@value AotServices#FACTORIES_RESOURCE_LOCATION} resource. This interface serves as SPI and can be provided through
	 * {@link org.springframework.beans.factory.aot.AotServices}.
	 * <p>
	 * {@link TypeCollector} discovers all implementations and applies the combined predicates returned by this interface
	 * to filter unwanted reachable types from AOT contribution.
	 *
	 * @author Mark Paluch
	 * @since 4.0
	 */
	public interface TypeCollectorFilters {

		/**
		 * Return a predicate to filter types.
		 *
		 * @return a predicate to filter types.
		 */
		default Predicate<Class<?>> classPredicate() {
			return Predicates.isTrue();
		}

		/**
		 * Return a predicate to filter fields.
		 *
		 * @return a predicate to filter fields.
		 */
		default Predicate<Field> fieldPredicate() {
			return Predicates.isTrue();
		}

		/**
		 * Return a predicate to filter methods for method signature introspection. not provided.
		 *
		 * @return a predicate to filter methods.
		 */
		default Predicate<Method> methodPredicate() {
			return Predicates.isTrue();
		}

	}

	/**
	 * Default implementation of {@link TypeCollectorFilters} that excludes types from certain packages and
	 * filters out unwanted fields and methods.
	 *
	 * @since 4.0
	 */
	private static class DefaultTypeCollectorFilters implements TypeCollectorFilters {

		private static final Set<String> EXCLUDED_DOMAINS = Set.of("java", "sun.", "jdk.", "reactor.", "kotlinx.",
				"kotlin.", "org.springframework.core.", "org.springframework.data.mapping.",
				"org.springframework.data.repository.", "org.springframework.boot.", "org.springframework.context.",
				"org.springframework.beans.");

		private static final Predicate<Class<?>> PACKAGE_PREDICATE = type -> {

			String packageName = type.getPackageName() + ".";

			for (String excludedDomain : EXCLUDED_DOMAINS) {
				if (packageName.startsWith(excludedDomain)) {
					return true;
				}
			}

			return false;
		};

		private static final Predicate<Class<?>> UNREACHABLE_CLASS = type -> type.isLocalClass() || type.isAnonymousClass();

		private static final Predicate<Member> UNWANTED_FIELDS = Predicates.IS_SYNTHETIC //
				.or(Predicates.IS_JAVA) //
				.or(Predicates.declaringClass(PACKAGE_PREDICATE));

		private static final Predicate<Method> UNWANTED_METHODS = Predicates.IS_BRIDGE_METHOD //
				.or(Predicates.IS_STATIC) //
				.or(Predicates.IS_SYNTHETIC) //
				.or(Predicates.IS_NATIVE) //
				.or(Predicates.IS_PRIVATE) //
				.or(Predicates.IS_PROTECTED) //
				.or(Predicates.IS_OBJECT_MEMBER) //
				.or(Predicates.IS_ENUM_MEMBER) //
				.or(Predicates.declaringClass(PACKAGE_PREDICATE));

		@Override
		public Predicate<Class<?>> classPredicate() {
			return UNREACHABLE_CLASS.or(PACKAGE_PREDICATE).negate();
		}

		@Override
		public Predicate<Field> fieldPredicate() {
			return (Predicate) UNWANTED_FIELDS.negate();
		}

		@Override
		public Predicate<Method> methodPredicate() {
			return UNWANTED_METHODS.negate();
		}

	}

}
