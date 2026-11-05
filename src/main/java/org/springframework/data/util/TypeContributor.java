/*
 * Copyright 2022-present the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * @author Christoph Strobl
 * @author Blaz Snuderl
 * @since 3.0
 */
public class TypeContributor {

	public static final String DATA_NAMESPACE = "org.springframework.data";
	public static final BindingReflectionHintsRegistrar DATA_BINDING_REGISTRAR = new BindingReflectionHintsRegistrar();
	public static final ReflectiveRuntimeHintsRegistrar REFLECTIVE_REGISTRAR = new ReflectiveRuntimeHintsRegistrar();

	/**
	 * Types already contributed to a {@link RuntimeHints} instance. Contributing a (non-annotation) type twice registers
	 * the very same hints again while re-walking its entire type graph, which is why contributions are tracked per
	 * {@link RuntimeHints}. Keys are held weakly to not retain hints beyond AOT processing.
	 */
	private static final Map<RuntimeHints, Set<Class<?>>> contributedTypes = new ConcurrentReferenceHashMap<>(8,
			ConcurrentReferenceHashMap.ReferenceType.WEAK);

	/**
	 * Contribute the type with default reflection configuration, skip annotations.
	 *
	 * @param type
	 * @param contribution
	 */
	public static void contribute(Class<?> type, GenerationContext contribution) {
		contribute(type, Collections.emptySet(), contribution);
	}

	/**
	 * Contribute the type with default reflection configuration and only include matching annotations.
	 *
	 * @param type
	 * @param filter
	 * @param contribution
	 */
	@SuppressWarnings("unchecked")
	public static void contribute(Class<?> type, Predicate<Class<? extends Annotation>> filter,
			GenerationContext contribution) {

		if (type.isPrimitive()) {
			return;
		}

		if (type.isAnnotation() && filter.test((Class<? extends Annotation>) type)) {

			contribution.getRuntimeHints().reflection().registerType(type,
					hint -> {});

			return;
		}

		if (!isNewContribution(type, contribution)) {
			return;
		}

		DATA_BINDING_REGISTRAR.registerReflectionHints(contribution.getRuntimeHints().reflection(), type);
		REFLECTIVE_REGISTRAR.registerRuntimeHints(contribution.getRuntimeHints(), type);
	}

	/**
	 * Contribute the given types with default reflection configuration and only include matching annotations.
	 * <p>
	 * Data binding hints are registered for all given types in a single pass so that types reachable from more than one of
	 * the given types are visited once instead of once per given type.
	 *
	 * @param types the types to contribute.
	 * @param filter filter to include annotation types.
	 * @param contribution the generation context to contribute to.
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public static void contribute(Collection<Class<?>> types, Predicate<Class<? extends Annotation>> filter,
			GenerationContext contribution) {

		List<Class<?>> dataBindingTypes = new ArrayList<>(types.size());

		for (Class<?> type : types) {

			if (type.isPrimitive()) {
				continue;
			}

			if (type.isAnnotation() && filter.test((Class<? extends Annotation>) type)) {

				contribution.getRuntimeHints().reflection().registerType(type, hint -> {});
				continue;
			}

			if (!isNewContribution(type, contribution)) {
				continue;
			}

			dataBindingTypes.add(type);
		}

		if (dataBindingTypes.isEmpty()) {
			return;
		}

		DATA_BINDING_REGISTRAR.registerReflectionHints(contribution.getRuntimeHints().reflection(),
				dataBindingTypes.toArray(Type[]::new));

		for (Class<?> type : dataBindingTypes) {
			REFLECTIVE_REGISTRAR.registerRuntimeHints(contribution.getRuntimeHints(), type);
		}
	}

	/**
	 * Register the type as contributed to the given {@link GenerationContext} returning whether the type was contributed
	 * for the first time.
	 */
	private static boolean isNewContribution(Class<?> type, GenerationContext contribution) {
		return contributedTypes.computeIfAbsent(contribution.getRuntimeHints(), it -> ConcurrentHashMap.newKeySet())
				.add(type);
	}

	/**
	 * Contribute the type with default reflection configuration and only include annotations from a certain namespace and
	 * those meta annotated with one of them.
	 *
	 * @param type
	 * @param annotationNamespaces
	 * @param contribution
	 */
	public static void contribute(Class<?> type, Set<String> annotationNamespaces, GenerationContext contribution) {
		contribute(type, it -> isPartOfOrMetaAnnotatedWith(it, annotationNamespaces), contribution);
	}

	public static boolean isPartOf(Class<?> type, Set<String> namespaces) {
		return namespaces.stream().anyMatch(namespace -> type.getPackageName().startsWith(namespace));
	}

	public static boolean isPartOfOrMetaAnnotatedWith(Class<? extends Annotation> annotation, Set<String> namespaces) {

		if (isPartOf(annotation, namespaces)) {
			return true;
		}

		return MergedAnnotation.of(annotation).getMetaTypes().stream().anyMatch(it -> isPartOf(annotation, namespaces));
	}
}
