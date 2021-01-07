/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.spel;

import static org.assertj.core.api.Assertions.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ReactiveEvaluationContextExtension;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link ReactiveExtensionAwareEvaluationContextProvider}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class ReactiveExtensionAwareEvaluationContextProviderUnitTests {

	SpelExpressionParser PARSER = new SpelExpressionParser();

	@BeforeEach
	void beforeEach() {
		GenericModelRoot.creationCounter.set(0);
		SecurityExpressionRoot.creationCounter.set(0);
	}

	@Test // DATACMNS-1108
	void shouldResolveExtension() {

		Expression expression = PARSER.parseExpression("hasRole('ROLE_ADMIN') ? '%' : principal.name");
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expression);

		ReactiveExtensionAwareEvaluationContextProvider provider = new ReactiveExtensionAwareEvaluationContextProvider(
				Arrays.asList(SampleReactiveExtension.INSTANCE, GenericExtension.INSTANCE));

		provider.getEvaluationContextLater(new Object[0], dependencies).map(expression::getValue) //
				.as(StepVerifier::create) //
				.expectNext("Walter") //
				.verifyComplete();

		assertThat(GenericModelRoot.creationCounter).hasValue(1);
		assertThat(SecurityExpressionRoot.creationCounter).hasValue(1);
	}

	@Test // DATACMNS-1108
	void shouldLoadGenericExtensionOnly() {

		Expression expression = PARSER.parseExpression("isKnown('FOO')");
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expression);

		ReactiveExtensionAwareEvaluationContextProvider provider = new ReactiveExtensionAwareEvaluationContextProvider(
				Arrays.asList(SampleReactiveExtension.INSTANCE, GenericExtension.INSTANCE));

		provider.getEvaluationContextLater(new Object[0], dependencies).map(expression::getValue) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		assertThat(GenericModelRoot.creationCounter).hasValue(1);
		assertThat(SecurityExpressionRoot.creationCounter).hasValue(0);
	}

	@Test // DATACMNS-1108
	void unknownMethodShouldLoadGenericExtensionOnly() {

		Expression expression = PARSER.parseExpression("unknown('FOO')");
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expression);

		ReactiveExtensionAwareEvaluationContextProvider provider = new ReactiveExtensionAwareEvaluationContextProvider(
				Arrays.asList(SampleReactiveExtension.INSTANCE, GenericExtension.INSTANCE));

		provider.getEvaluationContextLater(new Object[0], dependencies).map(expression::getValue) //
				.as(StepVerifier::create) //
				.verifyError(SpelEvaluationException.class);

		assertThat(GenericModelRoot.creationCounter).hasValue(1);
		assertThat(SecurityExpressionRoot.creationCounter).hasValue(0);
	}

	@Test // DATACMNS-1108
	void genericReactiveExtensionIsAlwaysObtained() {

		Expression expression = PARSER.parseExpression("1+1");
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expression);

		ReactiveExtensionAwareEvaluationContextProvider provider = new ReactiveExtensionAwareEvaluationContextProvider(
				Arrays.asList(SampleReactiveExtension.INSTANCE, GenericReactiveExtension.INSTANCE));

		provider.getEvaluationContextLater(new Object[0], dependencies).map(expression::getValue) //
				.as(StepVerifier::create) //
				.expectNext(2) //
				.verifyComplete();

		assertThat(GenericModelRoot.creationCounter).hasValue(1);
		assertThat(SecurityExpressionRoot.creationCounter).hasValue(0);
	}

	@Test // DATACMNS-1108
	void doesNotLoadExtensionForDirectCall() {

		Expression expression = PARSER.parseExpression(
				"T(org.springframework.data.spel.ReactiveExtensionAwareEvaluationContextProviderUnitTests.WithStaticRole).hasRole('ADMIN')");
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expression);

		ReactiveExtensionAwareEvaluationContextProvider provider = new ReactiveExtensionAwareEvaluationContextProvider(
				Arrays.asList(SampleReactiveExtension.INSTANCE, GenericReactiveExtension.INSTANCE));

		provider.getEvaluationContextLater(new Object[0], dependencies).map(expression::getValue) //
				.as(StepVerifier::create) //
				.expectNext(true) //
				.verifyComplete();

		assertThat(GenericModelRoot.creationCounter).hasValue(1);
		assertThat(SecurityExpressionRoot.creationCounter).hasValue(0);
	}

	@Test // DATACMNS-1108
	void loadsExtensionEvenWhenRootObjectMethodMatches() {

		Expression expression = PARSER.parseExpression("principal.name");
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expression);

		ReactiveExtensionAwareEvaluationContextProvider provider = new ReactiveExtensionAwareEvaluationContextProvider(
				Arrays.asList(SampleReactiveExtension.INSTANCE, GenericReactiveExtension.INSTANCE));

		provider.getEvaluationContextLater(new WithRole(), dependencies).map(expression::getValue) //
				.as(StepVerifier::create) //
				.expectNext("Walter") //
				.verifyComplete();

		assertThat(GenericModelRoot.creationCounter).hasValue(1);
		assertThat(SecurityExpressionRoot.creationCounter).hasValue(1);
	}

	public static class WithStaticRole {

		public static boolean hasRole(String arg) {
			return arg.equals("ADMIN");
		}
	}

	public static class WithRole {

		public Object getPrincipal() {
			return new MyPrincipal("Jesse");
		}
	}

	/**
	 * Sample reactive extension exposing a concrete type.
	 */
	enum SampleReactiveExtension implements ReactiveEvaluationContextExtension {

		INSTANCE;

		@Override
		public Mono<ExpressiveExtension> getExtension() {
			return Mono.just(ExpressiveExtension.INSTANCE);
		}

		@Override
		public String getExtensionId() {
			return "reactive";
		}
	}

	/**
	 * Sample reactive extension exposing a generic type.
	 */
	enum GenericReactiveExtension implements ReactiveEvaluationContextExtension {

		INSTANCE;

		@Override
		public Mono<EvaluationContextExtension> getExtension() {
			return Mono.just(GenericExtension.INSTANCE);
		}

		@Override
		public String getExtensionId() {
			return "reactive";
		}
	}

	/**
	 * Extension exposing a concrete root object type.
	 */
	enum ExpressiveExtension implements EvaluationContextExtension {

		INSTANCE;

		@Override
		public String getExtensionId() {
			return "expressive";
		}

		@Override
		public SecurityExpressionRoot getRootObject() {
			return new SecurityExpressionRoot();
		}
	}

	/**
	 * Extension without exposing a concrete extension type.
	 */
	enum GenericExtension implements EvaluationContextExtension {

		INSTANCE;

		@Override
		public String getExtensionId() {
			return "generic";
		}

		@Override
		public Object getRootObject() {
			return new GenericModelRoot();
		}
	}

	public static class GenericModelRoot {

		static AtomicInteger creationCounter = new AtomicInteger();

		GenericModelRoot() {
			creationCounter.incrementAndGet();
		}

		public boolean isKnown(String role) {
			return role.equals("FOO");
		}
	}

	public static class SecurityExpressionRoot {

		static AtomicInteger creationCounter = new AtomicInteger();

		SecurityExpressionRoot() {
			creationCounter.incrementAndGet();
		}

		public boolean hasRole(String role) {
			return role.equals("ADMIN");
		}

		public Object getPrincipal() {
			return new MyPrincipal("Walter");
		}
	}

	static class MyPrincipal {

		private final String name;

		public MyPrincipal(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
