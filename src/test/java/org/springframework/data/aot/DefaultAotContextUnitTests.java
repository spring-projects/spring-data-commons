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
package org.springframework.data.aot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests targeting {@link DefaultAotContext};
 *
 * @author Christoph Strobl
 */
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class DefaultAotContextUnitTests {

	@Mock BeanFactory beanFactory;

	@Mock AotMappingContext mappingContext;

	MockEnvironment mockEnvironment = new MockEnvironment();

	@Test // GH-3387
	void doesNotRegisterReflectionWhenThereIsNothingToRegister() {

		DefaultAotContext context = new DefaultAotContext(beanFactory, mockEnvironment, mappingContext);
		context.typeConfiguration(Dummy.class, it -> {
			// no specific action
		});

		TestGenerationContext generationContext = new TestGenerationContext();
		context.contributeTypeConfigurations(generationContext);

		assertThat(generationContext.getRuntimeHints()).matches(reflection().onType(Dummy.class).negate());
	}

	@Test // GH-3387
	void doesNotRegisterReflectionWithCategoryAccordingly() {

		DefaultAotContext context = new DefaultAotContext(beanFactory, mockEnvironment, mappingContext);
		context.typeConfiguration(Dummy.class, it -> it.forReflectiveAccess(MemberCategory.ACCESS_DECLARED_FIELDS));

		TestGenerationContext generationContext = new TestGenerationContext();
		context.contributeTypeConfigurations(generationContext);

		assertThat(generationContext.getRuntimeHints())
				.matches(reflection().onType(Dummy.class).withAnyMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS));
	}

	@Test // GH-3387
	void registerReflectionIfThereIsAnAtReflectiveAnnotation() throws NoSuchMethodException {

		DefaultAotContext context = new DefaultAotContext(beanFactory, mockEnvironment, mappingContext);
		context.typeConfiguration(DummyWithAtReflective.class, it -> {

		});

		TestGenerationContext generationContext = new TestGenerationContext();
		context.contributeTypeConfigurations(generationContext);

		assertThat(generationContext.getRuntimeHints())
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("reflectiveAnnotated")))
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("getValue")).negate())
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("justAMethod")).negate());
	}

	@Test // GH-3387
	void registerReflectionForGetterSetterIfDataBindingRequested() throws NoSuchMethodException {

		DefaultAotContext context = new DefaultAotContext(beanFactory, mockEnvironment, mappingContext);
		context.typeConfiguration(DummyWithAtReflective.class, AotTypeConfiguration::forDataBinding);

		TestGenerationContext generationContext = new TestGenerationContext();
		context.contributeTypeConfigurations(generationContext);

		assertThat(generationContext.getRuntimeHints())
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("reflectiveAnnotated")))
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("getValue")))
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("justAMethod")).negate());
	}

	@Test // GH-3387
	void registerReflectionIfThereIsAnAtReflectiveAnnotationInTheSuperType() throws NoSuchMethodException {
		DefaultAotContext context = new DefaultAotContext(beanFactory, mockEnvironment, mappingContext);
		context.typeConfiguration(ExtendingDummyWithAtReflective.class, it -> {

		});

		TestGenerationContext generationContext = new TestGenerationContext();
		context.contributeTypeConfigurations(generationContext);

		assertThat(generationContext.getRuntimeHints())
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("reflectiveAnnotated")))
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("justAMethod")).negate())
				.matches(reflection().onMethodInvocation(DummyWithAtReflective.class.getMethod("getValue")).negate());
	}

	static class Dummy {

		String value;

		public List<String> justAMethod() {
			return null;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	static class DummyWithAtReflective extends Dummy {

		@Reflective
		public List<String> reflectiveAnnotated() {
			return null;
		}
	}

	static class ExtendingDummyWithAtReflective extends DummyWithAtReflective {}

}
