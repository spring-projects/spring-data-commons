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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.classloadersupport.HidingClassLoader;

/**
 * @author Christoph Strobl
 */
class TypeScannerUnitTests {

	@Test // GH-2634
	void looksForTypesMatchingAnnotationFilter() {

		Set<Class<?>> result = TypeScanner.typeScanner(getClass().getClassLoader()) //
				.scanPackages(getClass().getPackageName()) //
				.forTypesAnnotatedWith(FindMe.class) //
				.collectAsSet();

		assertThat(result).containsExactlyInAnyOrder(AnnotatedWithFindMe.class, AnnotatedWithMetaFindMe.class);
	}

	@Test // GH-2634
	void looksForAllTypesIfNoFilter() {

		Set<Class<?>> result = TypeScanner.typeScanner(getClass().getClassLoader()) //
				.scanPackages(getClass().getPackageName()) //
				.collectAsSet();

		assertThat(result).contains(AnnotatedWithFindMe.class, AnnotatedWithMetaFindMe.class, WithoutAnnotations.class);
	}

	@Test // GH-2634
	void ignoresClassesNotFound() {

		TypeScanner scanner = TypeScanner.typeScanner(HidingClassLoader.hideTypes(AnnotatedWithFindMe.class)) //
				.scanPackages(getClass().getPackageName()) //
				.forTypesAnnotatedWith(FindMe.class);

		scanner.collectAsSet(); // no exception
	}

	@Test // GH-2634
	void raisesErrorClassesNotFoundIfConfigured() {

		TypeScanner scanner = TypeScanner.typeScanner(HidingClassLoader.hideTypes(AnnotatedWithFindMe.class)) //
				.scanPackages(getClass().getPackageName()) //
				.forTypesAnnotatedWith(FindMe.class).onClassNotFound(ex -> {
					throw new IllegalStateException(ex);
				});

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> scanner.collectAsSet()); // no exception
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface FindMe {
	}

	@FindMe
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaAnnotatedWithFindMe {
	}

	@FindMe
	private static class AnnotatedWithFindMe {

	}

	@MetaAnnotatedWithFindMe
	private static class AnnotatedWithMetaFindMe {

	}

	private static class WithoutAnnotations {

	}
}
