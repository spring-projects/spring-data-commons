/*
 * Copyright 2022-2023 the original author or authors.
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.aot.ManagedTypesRegistrationAotContribution.ManagedTypesInstanceCodeFragment;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ManagedTypesBeanRegistrationAotProcessor}.
 *
 * @author Christoph Strobl
 */
class ManagedTypesBeanRegistrationAotProcessorUnitTests {

	private final RootBeanDefinition managedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(ManagedTypes.class).setFactoryMethod("fromIterable")
			.addConstructorArgValue(Collections.singleton(A.class)).getBeanDefinition();

	private final RootBeanDefinition myManagedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(MyManagedTypes.class).getBeanDefinition();

	private final RootBeanDefinition invocationCountingManagedTypesDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(InvocationRecordingManagedTypes.class).getBeanDefinition();

	private DefaultListableBeanFactory beanFactory;

	@BeforeEach
	void beforeEach() {
		beanFactory = spy(new DefaultListableBeanFactory());
	}

	@Test // GH-2593
	void processesBeanWithMatchingModulePrefix() {

		beanFactory.registerBeanDefinition("commons.managed-types", managedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		assertThat(contribution).isNotNull();
	}

	@Test // GH-2593
	void processesBeanDefinitionIfPossibleWithoutLoadingTheBean() {

		beanFactory.registerBeanDefinition("commons.managed-types", managedTypesDefinition);

		createPostProcessor("commons").processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		verify(beanFactory, never()).getBean(eq("commons.managed-types"), eq(ManagedTypes.class));
	}

	@Test // GH-2593
	void contributesReflectionForManagedTypes() {

		beanFactory.registerBeanDefinition("commons.managed-types", managedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		GenerationContext generationContext = new TestGenerationContext(Object.class);

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
	void processesMatchingSubtypeBeanByAttemptingToLoadItIfNoMatchingConstructorArgumentFound() {

		beanFactory.registerBeanDefinition("commons.managed-types", myManagedTypesDefinition);

		createPostProcessor("commons").processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		verify(beanFactory).getBean(eq("commons.managed-types"), eq(ManagedTypes.class));
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

	@Test // GH-2593
	void returnsEmptyContributionWhenBeanCannotBeLoaded() {

		doThrow(new BeanCreationException("o_O")).when(beanFactory).getBean(eq("commons.managed-types"),
				eq(ManagedTypes.class));

		beanFactory.registerBeanDefinition("commons.managed-types", myManagedTypesDefinition);

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		GenerationContext generationContext = new TestGenerationContext(Object.class);

		contribution.applyTo(generationContext, null);

		assertThat(generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
		verify(beanFactory).getBean(eq("commons.managed-types"), eq(ManagedTypes.class));
	}

	@Test // GH-2680
	void generatesInstanceSupplierCodeFragmentToAvoidDuplicateInvocations() {

		beanFactory.registerBeanDefinition("commons.managed-types", invocationCountingManagedTypesDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "commons.managed-types");

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		AotTestCodeContributionBuilder.withContextFor(this.getClass()).writeContentFor(contribution).compile(it -> {

			InvocationRecordingManagedTypes sourceTypes = beanFactory.getBean(InvocationRecordingManagedTypes.class);
			assertThat(sourceTypes.getCounter()).isOne();

			InstanceSupplier<InvocationRecordingManagedTypes> types = ReflectionTestUtils
					.invokeMethod(it.getAllCompiledClasses().iterator().next(), "instance");
			try {
				assertThat(types.get(registeredBean).source).isNotSameAs(sourceTypes);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test // GH-2680
	void generatesInstanceSupplierCodeFragmentToAvoidDuplicateInvocationsForEmptyManagedTypes() {

		beanFactory.registerBeanDefinition("commons.managed-types", BeanDefinitionBuilder.rootBeanDefinition(EmptyManagedTypes.class).getBeanDefinition());
		RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "commons.managed-types");

		BeanRegistrationAotContribution contribution = createPostProcessor("commons")
				.processAheadOfTime(RegisteredBean.of(beanFactory, "commons.managed-types"));

		AotTestCodeContributionBuilder.withContextFor(this.getClass()).writeContentFor(contribution).compile(it -> {


			InstanceSupplier<ManagedTypes> types = ReflectionTestUtils
					.invokeMethod(it.getAllCompiledClasses().iterator().next(), "instance");
			try {
				assertThat(types.get(registeredBean).toList()).isEmpty();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test // GH-2680
	void generatesInstanceSupplierCodeFragmentForTypeWithCustomFactoryMethod() {

		beanFactory.registerBeanDefinition("commons.managed-types",
				BeanDefinitionBuilder.rootBeanDefinition(StoreManagedTypesWithCustomFactoryMethod.class).getBeanDefinition());

		RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "commons.managed-types");

		BeanRegistrationAotContribution contribution = createPostProcessor("commons").processAheadOfTime(registeredBean);

		AotTestCodeContributionBuilder.withContextFor(this.getClass()).writeContentFor(contribution).compile(it -> {

			InstanceSupplier<StoreManagedTypesWithCustomFactoryMethod> types = ReflectionTestUtils
					.invokeMethod(it.getAllCompiledClasses().iterator().next(), "instance");

			try {
				assertThat(types.get(registeredBean).toList()).containsExactlyInAnyOrder(A.class, B.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test // GH-2680
	void canGenerateCodeReturnsTrueIfFactoryMethodPresent() {

		beanFactory.registerBeanDefinition("managed-types", managedTypesDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "managed-types");

		ManagedTypesInstanceCodeFragment fragment = new ManagedTypesInstanceCodeFragment(List.of(A.class, B.class),
				registeredBean, Mockito.mock(BeanRegistrationCodeFragments.class));
		Builder methodBuilder = MethodSpec.methodBuilder("instance");
		fragment.generateInstanceFactory(methodBuilder);

		assertThat(fragment.canGenerateCode()).isTrue();
	}

	@Test // GH-2680
	void canGenerateCodeReturnsFalseIfNoFactoryMethodPresent() {

		beanFactory.registerBeanDefinition("managed-types", myManagedTypesDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "managed-types");

		ManagedTypesInstanceCodeFragment fragment = new ManagedTypesInstanceCodeFragment(List.of(A.class, B.class),
				registeredBean, Mockito.mock(BeanRegistrationCodeFragments.class));

		assertThat(fragment.canGenerateCode()).isFalse();
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

	static class StoreManagedTypesWithFactoryMethodOfClassNames implements ManagedTypes {
		@Override
		public void forEach(Consumer<Class<?>> action) {
			// just do nothing ¯\_(ツ)_/¯
		}
	}

	public static class EmptyManagedTypes implements ManagedTypes {

		public EmptyManagedTypes() {

		}

		public static EmptyManagedTypes of(ManagedTypes source) {
			return new EmptyManagedTypes();
		}

		@Override
		public void forEach(Consumer<Class<?>> action) {

		}
	}

	public static class StoreManagedTypesWithCustomFactoryMethod implements ManagedTypes {

		private ManagedTypes source;

		public StoreManagedTypesWithCustomFactoryMethod() {
			source = it -> Arrays.asList(A.class, B.class).forEach(it);
		}

		public StoreManagedTypesWithCustomFactoryMethod(ManagedTypes source) {
			this.source = source;
		}

		public static StoreManagedTypesWithCustomFactoryMethod of(ManagedTypes source) {
			return new StoreManagedTypesWithCustomFactoryMethod(source);
		}

		@Override
		public void forEach(Consumer<Class<?>> action) {
			source.forEach(action);
		}
	}

	public static class InvocationRecordingManagedTypes implements ManagedTypes {

		private final AtomicInteger counter = new AtomicInteger(0);
		private ManagedTypes source = ManagedTypes.from(A.class, B.class);

		public static InvocationRecordingManagedTypes from(ManagedTypes source) {

			InvocationRecordingManagedTypes newInstance = new InvocationRecordingManagedTypes();
			newInstance.source = source;
			return newInstance;
		}

		@Override
		public void forEach(Consumer<Class<?>> action) {

			counter.getAndIncrement();
			source.forEach(action);
		}

		public int getCounter() {
			return counter.get();
		}
	}

	static class NotManagedTypes {}

	@Configuration(proxyBeanMethods = false)
	public static class EntityManagerWithPackagesToScanConfiguration {

		@Bean(name = "commons.managed-types")
		ManagedTypes managedTypes() {
			return ManagedTypes.from(A.class, B.class);
		}
	}
}
