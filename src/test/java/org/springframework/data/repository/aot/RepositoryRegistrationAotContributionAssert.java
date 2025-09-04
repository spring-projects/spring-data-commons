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
package org.springframework.data.repository.aot;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.data.aot.CodeContributionAssert;
import org.springframework.data.repository.config.RepositoryRegistrationAotContribution;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * AssertJ {@link AbstractAssert Assertion} for {@link RepositoryRegistrationAotContribution}.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @since 3.0
 */
public class RepositoryRegistrationAotContributionAssert
		extends AbstractAssert<RepositoryRegistrationAotContributionAssert, RepositoryRegistrationAotContribution> {

	public static RepositoryRegistrationAotContributionAssert assertThatContribution(
			RepositoryRegistrationAotContribution actual) {

		return new RepositoryRegistrationAotContributionAssert(actual);
	}

	/**
	 * Create the assertion object.
	 *
	 * @param actual
	 */
	public RepositoryRegistrationAotContributionAssert(RepositoryRegistrationAotContribution actual) {
		super(actual, RepositoryRegistrationAotContributionAssert.class);
	}

	/**
	 * Verifies that the actual repository type is equal to the given one.
	 *
	 * @param expected
	 * @return {@code this} assertion object.
	 */
	public RepositoryRegistrationAotContributionAssert targetRepositoryTypeIs(Class<?> expected) {

		assertThat(getRepositoryInformation().getRepositoryInterface()).isEqualTo(expected);

		return this.myself;
	}

	/**
	 * Verifies that the actual repository has no repository fragments.
	 *
	 * @return {@code this} assertion object.
	 */
	public RepositoryRegistrationAotContributionAssert hasNoFragments() {

		assertThat(getRepositoryInformation().getFragments()).isEmpty();

		return this;
	}

	/**
	 * Verifies that the actual repository has repository fragments.
	 *
	 * @return {@code this} assertion object.
	 */
	public RepositoryRegistrationAotContributionAssert hasFragments() {

		assertThat(getRepositoryInformation().getFragments()).isNotEmpty();

		return this;
	}

	/**
	 * Verifies that the actual repository fragments satisfy the given {@link Consumer}.
	 *
	 * @return {@code this} assertion object.
	 */
	public RepositoryRegistrationAotContributionAssert verifyFragments(Consumer<Set<RepositoryFragment<?>>> consumer) {

		assertThat(getRepositoryInformation().getFragments()).satisfies(it -> consumer.accept(new LinkedHashSet<>(it)));

		return this;
	}

	public RepositoryRegistrationAotContributionAssert codeContributionSatisfies(
			ThrowingConsumer<CodeContributionAssert> assertWith) {

		BeanRegistrationCode mockBeanRegistrationCode = mock(BeanRegistrationCode.class);

		GenerationContext generationContext = new TestGenerationContext(Object.class);

		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();

		try {
			Class<?> handlerClass = Class.forName("org.springframework.context.aot.CglibClassHandler");
			Constructor<?> constructor = handlerClass.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			Object handler = BeanUtils.instantiateClass(constructor, generationContext);

			Method withCglibClassHandler = generator.getClass().getDeclaredMethod("withCglibClassHandler", handlerClass,
					Supplier.class);
			withCglibClassHandler.setAccessible(true);
			withCglibClassHandler.invoke(generator, handler, new Supplier<Object>() {

				@Override
				public Object get() {

					actual.applyTo(generationContext, mockBeanRegistrationCode);
					return null;
				}
			});

			assertWith.accept(new CodeContributionAssert(generationContext));
		} catch (Throwable o_O) {
			fail(o_O.getMessage(), o_O);
		}

		return this;
	}

	private RepositoryInformation getRepositoryInformation() {

		assertThat(this.actual).describedAs("No repository interface found on null bean contribution").isNotNull();

		assertThat(this.actual.getRepositoryInformation())
				.describedAs("No repository interface found on null repository information").isNotNull();

		return this.actual.getRepositoryInformation();
	}
}
