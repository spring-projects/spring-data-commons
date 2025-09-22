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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.aot.generate.GeneratedTypeReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.util.Version;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * Unit testa for {@link AotRepositoryBeanDefinitionPropertiesDecorator}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class AotRepositoryBeanDefinitionPropertiesDecoratorUnitTests {

	private static final String TYPE_NAME = "com.example.UserRepositoryImpl__AotRepository";
	@Mock RepositoryContributor contributor;
	CodeBlock.Builder inheritedSource;
	AotRepositoryFragmentMetadata metadata = new AotRepositoryFragmentMetadata();
	RepositoryConstructorBuilder constructorBuilder = new RepositoryConstructorBuilder(metadata);
	AotRepositoryBeanDefinitionPropertiesDecorator decorator;

	@BeforeEach
	void beforeEach() {

		when(contributor.getContributedTypeName()).thenReturn(GeneratedTypeReference.of(ClassName.bestGuess(TYPE_NAME)));
		when(contributor.getAotFragmentMetadata()).thenReturn(metadata);
		inheritedSource = CodeBlock.builder();
		decorator = new AotRepositoryBeanDefinitionPropertiesDecorator(() -> inheritedSource.build(), contributor);
	}

	@Test // GH-3344
	void addsExistingSource() {

		inheritedSource.add("beanDefinition.getPropertyValues().addPropertyValue($S, $S)", "repositoryBaseClass",
				"org.springframework.data.BaseRepository");

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.contains(
						"beanDefinition.getPropertyValues().addPropertyValue(\"repositoryBaseClass\", \"org.springframework.data.BaseRepository\")");
	}

	@Test // GH-3344
	void addsFragmentFunction() {

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence(".addPropertyValue(\"repositoryFragmentsFunction\", new ",
						"RepositoryFragmentsFunction() {") //
				.containsSubsequence("RepositoryFragments getRepositoryFragments(", "BeanFactory beanFactory,",
						"FragmentCreationContext context)", "{");
	}

	@Test // GH-3344
	void addsPlainNoArgConstructorForEmptyArgs() {

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s());".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void resolvesAndAddsArgumentsForCtor() {

		constructorBuilder.addParameter("plain", ResolvableType.forClass(Version.class));
		constructorBuilder.addParameter("noGenericsDefined", ResolvableType.forClass(List.class));
		constructorBuilder.addParameter("withGenerics", ResolvableType.forClassWithGenerics(Set.class, String.class));

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence("Version plain = beanFactory.getBean(", "Version.class)") //
				.containsSubsequence("List noGenericsDefined = beanFactory.getBean(", "List.class)") //
				.containsSubsequence("Set<", "String> withGenerics = beanFactory.getBean(", "Set") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(plain, noGenericsDefined, withGenerics));" //
						.formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void resolvesValueFromCodeblock() {

		constructorBuilder.addParameter("byTypeAndName", ResolvableType.forClass(Version.class), customizer -> {
			customizer.origin(new RuntimeBeanReference("foo", Version.class));
		});

		constructorBuilder.addParameter("byName", ResolvableType.forClass(Version.class), customizer -> {
			customizer.origin(new RuntimeBeanReference("bar"));
		});

		constructorBuilder.addParameter("foo", Integer.class,
				customizer -> customizer.origin(ctx -> AotRepositoryConstructorBuilder.ParameterOrigin.of(CodeBlock.of("1"))));

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence("Version byTypeAndName = beanFactory.getBean(\"foo\"", "Version.class)") //
				.containsSubsequence("Version byName = ", "Version) beanFactory.getBean(\"bar\"") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(byTypeAndName, byName, 1));" //
						.formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnBeanFactoryIfRequested() {

		constructorBuilder.addParameter("beanFactory", BeanFactory.class);

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("BeanFactory beanFactory = beanFactory.getBean(") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(beanFactory));".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnContextIfRequested() {

		constructorBuilder.addParameter("context", RepositoryFactoryBeanSupport.FragmentCreationContext.class);

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("FragmentCreationContext context = beanFactory.getBean(") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(context));".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnContextWithDifferentNameIfRequested() {

		constructorBuilder.addParameter("theContext", RepositoryFactoryBeanSupport.FragmentCreationContext.class);

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.doesNotContain("FragmentCreationContext theContext = beanFactory.getBean(") //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(context));".formatted(TYPE_NAME));
	}

	@Test // GH-3344
	void passesOnBeanFactoryDifferentNameIfRequested() {

		constructorBuilder.addParameter("myBeanFactory", BeanFactory.class);

		CodeBlock decorate = decorator.decorate();

		assertThat(decorate.toString()) //
				.containsSubsequence("return ", "RepositoryFragments.just(new %s(beanFactory));".formatted(TYPE_NAME));
	}
}
