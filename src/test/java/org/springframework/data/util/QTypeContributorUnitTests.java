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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.data.aot.sample.ConfigWithQuerydslPredicateExecutor.Person;
import org.springframework.data.aot.sample.QConfigWithQuerydslPredicateExecutor_Person;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.javapoet.ClassName;

import com.querydsl.core.types.EntityPath;

/**
 * Unit tests for {@link QTypeContributor}.
 *
 * @author Christoph Strobl
 * @author ckdgus08
 */
class QTypeContributorUnitTests {

	@Test // GH-2721
	void addsQTypeHintIfPresent() {

		GenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(ClassName.get(this.getClass())), new InMemoryGeneratedFiles());

		QTypeContributor.contributeEntityPath(Person.class, generationContext, null);

		assertThat(generationContext.getRuntimeHints())
				.matches(RuntimeHintsPredicates.reflection().onType(QConfigWithQuerydslPredicateExecutor_Person.class));
	}

	@Test // GH-2721
	void doesNotAddQTypeHintIfTypeNotPresent() {

		GenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(ClassName.get(this.getClass())), new InMemoryGeneratedFiles());

		QTypeContributor.contributeEntityPath(Person.class, generationContext,
				HidingClassLoader.hideTypes(QConfigWithQuerydslPredicateExecutor_Person.class));

		assertThat(generationContext.getRuntimeHints()).matches(
				RuntimeHintsPredicates.reflection().onType(QConfigWithQuerydslPredicateExecutor_Person.class).negate());
	}

	@Test // GH-2721
	void doesNotAddQTypeHintIfQuerydslNotPresent() {

		GenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(ClassName.get(this.getClass())), new InMemoryGeneratedFiles());

		QTypeContributor.contributeEntityPath(Person.class, generationContext, HidingClassLoader.hide(EntityPath.class));

		assertThat(generationContext.getRuntimeHints()).matches(
				RuntimeHintsPredicates.reflection().onType(QConfigWithQuerydslPredicateExecutor_Person.class).negate());
	}

	@Test // GH-3284
	void addsQTypeHintForArrayType() {

		GenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(ClassName.get(this.getClass())), new InMemoryGeneratedFiles());

		QTypeContributor.contributeEntityPath(Person[].class, generationContext, HidingClassLoader.hideTypes());

		assertThat(generationContext.getRuntimeHints()).matches(
				RuntimeHintsPredicates.reflection().onType(QConfigWithQuerydslPredicateExecutor_Person.class).negate());
		assertThat(generationContext.getRuntimeHints())
				.matches(RuntimeHintsPredicates.reflection().onType(QConfigWithQuerydslPredicateExecutor_Person[].class));
	}

	@Test // GH-3284
	void doesNotAddQTypeHintForPrimitiveType() {

		GenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(ClassName.get(this.getClass())), new InMemoryGeneratedFiles());

		QTypeContributor.contributeEntityPath(int.class, generationContext, getClass().getClassLoader());

		assertThat(generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
	}

	@Test // GH-3284
	void doesNotFailForTypeInDefaultPackage() throws Exception {

		GenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(ClassName.get(this.getClass())), new InMemoryGeneratedFiles());

		class CapturingClassLoader extends ClassLoader {

			final Set<String> lookups = new HashSet<>(10);

			CapturingClassLoader() {
				super(URLClassLoader.getSystemClassLoader());
			}

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				lookups.add(name);
				return super.loadClass(name);
			}
		}

		CapturingClassLoader classLoaderToUse = new CapturingClassLoader();

		var typeInDefaultPackage = Class.forName("TypeInDefaultPackage");
		assertThatNoException().isThrownBy(
				() -> QTypeContributor.contributeEntityPath(typeInDefaultPackage, generationContext, classLoaderToUse));
		assertThat(classLoaderToUse.lookups).contains("QTypeInDefaultPackage");
	}
}
