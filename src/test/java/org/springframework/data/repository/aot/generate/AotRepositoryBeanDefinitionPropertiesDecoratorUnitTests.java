/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.aot.generate.GeneratedTypeReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.util.Version;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * Unit testa for {@link AotRepositoryBeanDefinitionPropertiesDecorator}.
 *
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
class AotRepositoryBeanDefinitionPropertiesDecoratorUnitTests {

	private static final String TYPE_NAME = "com.example.UserRepositoryImpl__AotRepository";
	@Mock RepositoryContributor contributor;
	CodeBlock.Builder inheritedSource;

	AotRepositoryBeanDefinitionPropertiesDecorator decorator;

	@BeforeEach
	void beforeEach() {

		when(contributor.getContributedTypeName()).thenReturn(GeneratedTypeReference.of(ClassName.bestGuess(TYPE_NAME)));
		inheritedSource = CodeBlock.builder();
		decorator = new AotRepositoryBeanDefinitionPropertiesDecorator(() -> inheritedSource.build(), contributor);
	}

	@Test // GH-3344
	void addsExistingSource() {

		inheritedSource.add("beanDefinition.getPropertyValues().addPropertyValue($S, $S)", "repositoryBaseClass",
				"org.springframework.data.BaseRepository");
		when(contributor.requiredArgs()).thenReturn(Map.of());

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.contains(
						"beanDefinition.getPropertyValues().addPropertyValue(\"repositoryBaseClass\", \"org.springframework.data.BaseRepository\")");
	}

	@Test // GH-3344
	void addsFragmentFunction() {

		when(contributor.requiredArgs()).thenReturn(Map.of());

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence(".addPropertyValue(\"repositoryFragmentsFunction\", new ",
						"RepositoryFragmentsFunction() {") //
				.containsSubsequence("RepositoryFragments getRepositoryFragments(", "BeanFactory beanFactory,",
						"FragmentCreationContext context)", "{");
	}

	@Test // GH-3344
	void addsPlainNoArgConstructorForEmptyArgs() {

		when(contributor.requiredArgs()).thenReturn(Map.of());

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s());".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void resolvesAndAddsArgumentsForCtor() {

		Map<String, ResolvableType> ctorArgs = new LinkedHashMap<>(3);
		ctorArgs.put("plain", ResolvableType.forClass(Version.class));
		ctorArgs.put("noGenericsDefined", ResolvableType.forClass(List.class));
		ctorArgs.put("withGenerics", ResolvableType.forClassWithGenerics(Set.class, String.class));

		when(contributor.requiredArgs()).thenReturn(ctorArgs);

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence("Version plain = beanFactory.getBean(", "Version.class)") //
				.containsSubsequence("List noGenericsDefined = beanFactory.getBean(", "List.class)") //
				.containsSubsequence("Set<", "String> withGenerics = beanFactory.getBean(", "Set") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(plain, noGenericsDefined, withGenerics));" //
						.formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnBeanFactoryIfRequested() {

		when(contributor.requiredArgs()).thenReturn(Map.of("beanFactory", ResolvableType.forClass(BeanFactory.class)));

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("BeanFactory beanFactory = beanFactory.getBean(") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(beanFactory));".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnContextIfRequested() {

		when(contributor.requiredArgs()).thenReturn(
				Map.of("context", ResolvableType.forClass(RepositoryFactoryBeanSupport.FragmentCreationContext.class)));

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("FragmentCreationContext context = beanFactory.getBean(") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(context));".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnContextWithDifferentNameIfRequested() {

		when(contributor.requiredArgs()).thenReturn(
				Map.of("theContext", ResolvableType.forClass(RepositoryFactoryBeanSupport.FragmentCreationContext.class)));

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("FragmentCreationContext theContext = beanFactory.getBean(") //
				.containsSubsequence("FragmentCreationContext theContext = context") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(theContext));".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnBeanFactoryDifferentNameIfRequested() {

		when(contributor.requiredArgs()).thenReturn(Map.of("myBeanFactory", ResolvableType.forClass(BeanFactory.class)));

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("BeanFactory myBeanFactory = beanFactory.getBean(") //
				.containsSubsequence("BeanFactory myBeanFactory = beanFactory") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(myBeanFactory));".formatted(TYPE_NAME));
	}
}
