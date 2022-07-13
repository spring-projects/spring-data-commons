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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedClasses;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.domain.ManagedTypes;

/**
 * @author Christoph Strobl
 */
class ManagedTypesBeanRegistrationAotProcessorUnitTests {

	final RootBeanDefinition managedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(ManagedTypes.class).setFactoryMethod("fromIterable")
			.addConstructorArgValue(Collections.singleton(A.class)).getBeanDefinition();

	final RootBeanDefinition myManagedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(MyManagedTypes.class).getBeanDefinition();

	DefaultListableBeanFactory beanFactory;

	@BeforeEach
	void beforeEach() {
		beanFactory = new DefaultListableBeanFactory();
	}

	@Test // GH-2593
	void processesBeanWithMatchingModulePrefix() {

		beanFactory.registerBeanDefinition("commons.managed-types", managedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		assertThat(contribution).isNotNull();
	}

	@Test // GH-2593
	void contributesReflectionForManagedTypes() {

		beanFactory.registerBeanDefinition("commons.managed-types", managedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		DefaultGenerationContext generationContext = new DefaultGenerationContext(
				new GeneratedClasses(new ClassNameGenerator(Object.class)),
				new InMemoryGeneratedFiles(), new RuntimeHints());

		contribution.applyTo(generationContext, null);

		assertThat(generationContext.getRuntimeHints()).matches(RuntimeHintsPredicates.reflection().onType(A.class)
				.and(RuntimeHintsPredicates.reflection().onType(B.class).negate()));
	}

	@Test // GH-2593
	void processesMatchingSubtypeBean() {

		beanFactory.registerBeanDefinition("commons.managed-types", myManagedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		assertThat(contribution).isNotNull();
	}

	@Test // GH-2593
	void ignoresBeanNotMatchingRequiredType() {

		beanFactory.registerBeanDefinition("commons.managed-types",
				BeanDefinitionBuilder.rootBeanDefinition(NotManagedTypes.class).getBeanDefinition());

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		assertThat(contribution).isNull();
	}

	@Test // GH-2593
	void ignoresBeanNotMatchingPrefix() {

		beanFactory.registerBeanDefinition("jpa.managed-types", managedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "jpa.managed-types"));

		assertThat(contribution).isNull();
	}

	private ManagedTypesBeanRegistrationAotProcessor createPostProcessor(String moduleIdentifier) {
		ManagedTypesBeanRegistrationAotProcessor postProcessor = new ManagedTypesBeanRegistrationAotProcessor();
		postProcessor.setModuleIdentifier(moduleIdentifier);

		return postProcessor;
	}

	static class A {}

	static class B {}

	static class MyManagedTypes implements ManagedTypes {
		@Override
		public void forEach(Consumer<Class<?>> action) {
			// just do nothing ¯\_(ツ)_/¯
		}
	}

	static class NotManagedTypes {}
}
