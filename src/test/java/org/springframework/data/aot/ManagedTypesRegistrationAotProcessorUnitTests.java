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

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.ManagedTypes;

/**
 * @author Christoph Strobl
 */
class ManagedTypesRegistrationAotProcessorUnitTests {

	final RootBeanDefinition managedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(ManagedTypes.class).setFactoryMethod("of")
			.addConstructorArgValue(Collections.singleton(A.class)).getBeanDefinition();

	final RootBeanDefinition myManagedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(MyManagedTypes.class).getBeanDefinition();

	@Test // GH-2593
	void processesBeanWithMatchingModulePrefix() {

		BeanRegistrationAotContribution contribution = createPostProcessor("commons", bf -> {
			bf.registerBeanDefinition("commons.managed-types", managedTypesDefinition);
		}).contribute(managedTypesDefinition, ManagedTypes.class, "commons.managed-types");

		assertThat(contribution).isNotNull();
	}

	@Test // GH-2593
	void contributesReflectionForManagedTypes() {

		BeanRegistrationAotContribution contribution = createPostProcessor("commons", bf -> {
			bf.registerBeanDefinition("commons.managed-types", managedTypesDefinition);
		}).contribute(managedTypesDefinition, ManagedTypes.class, "commons.managed-types");

		DefaultGenerationContext generationContext = new DefaultGenerationContext(new ClassNameGenerator(),
				new InMemoryGeneratedFiles(), new RuntimeHints());

		contribution.applyTo(generationContext, null);

		new CodeContributionAssert(generationContext) //
				.contributesReflectionFor(A.class) //
				.doesNotContributeReflectionFor(B.class);
	}

	@Test // GH-2593
	void processesMatchingSubtypeBean() {

		BeanRegistrationAotContribution contribution = createPostProcessor("commons", bf -> {
			bf.registerBeanDefinition("commons.managed-types", myManagedTypesDefinition);
		}).contribute(myManagedTypesDefinition, MyManagedTypes.class, "commons.managed-types");

		assertThat(contribution).isNotNull();
	}

	@Test // GH-2593
	void ignoresBeanNotMatchingRequiredType() {

		BeanRegistrationAotContribution contribution = createPostProcessor("commons", bf -> {
			bf.registerBeanDefinition("commons.managed-types", managedTypesDefinition);
		}).contribute(managedTypesDefinition, Object.class, "commons.managed-types");

		assertThat(contribution).isNull();
	}

	@Test // GH-2593
	void ignoresBeanNotMatchingPrefix() {

		BeanRegistrationAotContribution contribution = createPostProcessor("commons", bf -> {
			bf.registerBeanDefinition("commons.managed-types", managedTypesDefinition);
		}).contribute(managedTypesDefinition, ManagedTypes.class, "jpa.managed-types");

		assertThat(contribution).isNull();
	}

	private ManagedTypesRegistrationAotProcessor createPostProcessor(String prefix, Consumer<DefaultListableBeanFactory> action) {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		action.accept(beanFactory);

		ManagedTypesRegistrationAotProcessor postProcessor = createPostProcessor(beanFactory);
		postProcessor.setModulePrefix(prefix);

		return postProcessor;
	}

	private ManagedTypesRegistrationAotProcessor createPostProcessor(BeanFactory beanFactory) {

		ManagedTypesRegistrationAotProcessor managedTypesRegistrationAotProcessor = new ManagedTypesRegistrationAotProcessor();
		managedTypesRegistrationAotProcessor.setBeanFactory(beanFactory);
		return managedTypesRegistrationAotProcessor;
	}

	static class A {}

	static class B {}

	static class MyManagedTypes implements ManagedTypes {

		@Override
		public void forEach(Consumer<Class<?>> action) {
			// just do nothing ¯\_(ツ)_/¯
		}
	}
}
