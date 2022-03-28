/*
 * Copyright 2022 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.SynthesizedAnnotation;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
class TypeContributor {

	public static final String DATA_NAMESPACE = "org.springframework.data";

	/**
	 * Contribute the type with default reflection configuration, skip annotations.
	 *
	 * @param type
	 * @param contribution
	 */
	static void contribute(Class<?> type, CodeContribution contribution) {
		contribute(type, Collections.emptySet(), contribution);
	}

	/**
	 * Contribute the type with default reflection configuration and only include matching annotations.
	 *
	 * @param type
	 * @param filter
	 * @param contribution
	 */
	static void contribute(Class<?> type, Predicate<Class<? extends Annotation>> filter, CodeContribution contribution) {

		if (type.isPrimitive()) {
			return;
		}

		if (type.isAnnotation() && filter.test((Class<? extends Annotation>) type)) {

			contribution.runtimeHints().reflection().registerType(type, hint -> {
				hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
			});

			// TODO: do we need this if meta annotated with SD annotation?
			if (type.getPackage().getName().startsWith(DATA_NAMESPACE)) {
				contribution.runtimeHints().proxies().registerJdkProxy(type, SynthesizedAnnotation.class);
			}
			return;
		}

		if (type.isInterface()) {
			contribution.runtimeHints().reflection().registerType(type, hint -> {
				hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
			});
			return;
		}

		contribution.runtimeHints().reflection().registerType(type, hint -> {
			hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
		});
	}

	/**
	 * Contribute the type with default reflection configuration and only include annotations from a certain namespace and
	 * those meta annotated with one of them.
	 *
	 * @param type
	 * @param annotationNamespaces
	 * @param contribution
	 */
	static void contribute(Class<?> type, Set<String> annotationNamespaces, CodeContribution contribution) {
		contribute(type, it -> isPartOfOrMetaAnnotatedWith(it, annotationNamespaces), contribution);
	}

	private static boolean isPartOf(Class<?> type, Set<String> namespaces) {
		return namespaces.stream().anyMatch(namespace -> type.getPackageName().startsWith(namespace));
	}

	protected static boolean isPartOfOrMetaAnnotatedWith(Class<? extends Annotation> annotation, Set<String> namespaces) {

		if (isPartOf(annotation, namespaces)) {
			return true;
		}

		return MergedAnnotation.of(annotation).getMetaTypes().stream().anyMatch(it -> isPartOf(annotation, namespaces));
	}
}
