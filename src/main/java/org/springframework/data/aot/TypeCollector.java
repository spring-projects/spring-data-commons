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
package org.springframework.data.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.data.util.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 * @author Sebastien Deleuze
 * @author John Blum
 */
public class TypeCollector {

	private static final Log logger = LogFactory.getLog(TypeCollector.class);

	static final Set<String> EXCLUDED_DOMAINS = new HashSet<>(Arrays.asList("java", "sun.", "jdk.", "reactor.",
			"kotlinx.", "kotlin.", "org.springframework.core.", "org.springframework.boot."));

	private final Predicate<Class<?>> excludedDomainsFilter = type -> {
		String packageName = type.getPackageName();
		return EXCLUDED_DOMAINS.stream().noneMatch(packageName::startsWith);
	};

	private Predicate<Class<?>> typeFilter = excludedDomainsFilter;

	private final Predicate<Method> methodFilter = method -> {

		Predicate<Method> excludedDomainsPredicate = methodToTest ->
			excludedDomainsFilter.test(methodToTest.getDeclaringClass());

		Predicate<Method> excludedMethodsPredicate = Predicates.IS_BRIDGE_METHOD
				.or(Predicates.IS_OBJECT_MEMBER)
				.or(Predicates.IS_HIBERNATE_MEMBER)
				.or(Predicates.IS_ENUM_MEMBER)
				.or(Predicates.IS_NATIVE)
				.or(Predicates.IS_PRIVATE)
				.or(Predicates.IS_PROTECTED)
				.or(Predicates.IS_SYNTHETIC)
				.or(excludedDomainsPredicate.negate());

		return !excludedMethodsPredicate.test(method);
	};

	private Predicate<Field> fieldFilter = field -> {

		Predicate<Member> excludedFieldPredicate = Predicates.IS_HIBERNATE_MEMBER
			.or(Predicates.IS_SYNTHETIC)
			.or(Predicates.IS_JAVA);

		return !excludedFieldPredicate.test(field);
	};

	@NonNull
	public TypeCollector filterFields(@NonNull Predicate<Field> filter) {
		this.fieldFilter = filter.and(filter);
		return this;
	}

	@NonNull
	public TypeCollector filterTypes(@NonNull Predicate<Class<?>> filter) {
		this.typeFilter = this.typeFilter.and(filter);
		return this;
	}

	/**
	 * Inspect the given type and resolve those reachable via fields, methods, generics, ...
	 *
	 * @param types the types to inspect
	 * @return a type model collector for the type
	 */
	@NonNull
	public static ReachableTypes inspect(Class<?>... types) {
		return inspect(Arrays.asList(types));
	}

	@NonNull
	public static ReachableTypes inspect(Collection<Class<?>> types) {
		return new ReachableTypes(new TypeCollector(), types);
	}

	private void process(Class<?> root, Consumer<ResolvableType> consumer) {
		processType(ResolvableType.forType(root), new InspectionCache(), consumer);
	}

	private void processType(ResolvableType type, InspectionCache cache, Consumer<ResolvableType> callback) {

		if (ResolvableType.NONE.equals(type) || cache.contains(type)) {
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

	Set<Type> visitConstructorsOfType(ResolvableType type) {
		if (!typeFilter.test(type.toClass())) {
			return Collections.emptySet();
		}
		Set<Type> discoveredTypes = new LinkedHashSet<>();
		for (Constructor<?> constructor : type.toClass().getDeclaredConstructors()) {
			for (Class<?> signatureType : TypeUtils.resolveTypesInSignature(type.toClass(), constructor)) {
				if (typeFilter.test(signatureType)) {
					discoveredTypes.add(signatureType);
				}
			}
		}
		return new HashSet<>(discoveredTypes);
	}

	Set<Type> visitMethodsOfType(ResolvableType type) {
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

	Set<Type> visitFieldsOfType(ResolvableType type) {
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

	public static class ReachableTypes {

		private final Iterable<Class<?>> roots;
		private final Lazy<List<Class<?>>> reachableTypes = Lazy.of(this::collect);
		private final TypeCollector typeCollector;

		public ReachableTypes(@NonNull TypeCollector typeCollector, @NonNull Iterable<Class<?>> roots) {

			this.typeCollector = typeCollector;
			this.roots = roots;
		}

		public void forEach(Consumer<ResolvableType> consumer) {
			roots.forEach(it -> typeCollector.process(it, consumer));
		}

		public List<Class<?>> list() {
			return reachableTypes.get();
		}

		private List<Class<?>> collect() {
			List<Class<?>> target = new ArrayList<>();
			forEach(it -> target.add(it.toClass()));
			return target;
		}
	}

	static class InspectionCache {

		private final Map<String, ResolvableType> mutableCache = new LinkedHashMap<>();

		public void add(@NonNull ResolvableType resolvableType) {
			mutableCache.put(resolvableType.toString(), resolvableType);
		}

		public void clear() {
			mutableCache.clear();
		}

		public boolean contains(@NonNull ResolvableType key) {
			return mutableCache.containsKey(key.toString());
		}

		public boolean isEmpty() {
			return mutableCache.isEmpty();
		}

		public int size() {
			return mutableCache.size();
		}
	}
}
