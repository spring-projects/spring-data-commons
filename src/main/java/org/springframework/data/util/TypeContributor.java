/*
 * Copyright 2022-2025 the original author or authors.
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
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
public class TypeContributor {

	public static final String DATA_NAMESPACE = "org.springframework.data";
	public static final BindingReflectionHintsRegistrar REGISTRAR = new BindingReflectionHintsRegistrar();

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

		REGISTRAR.registerReflectionHints(contribution.getRuntimeHints().reflection(), type);
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
