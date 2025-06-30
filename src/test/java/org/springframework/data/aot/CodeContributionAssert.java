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
package org.springframework.data.aot;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

/**
 * AssertJ {@link AbstractAssert Assertion} for code contributions originating from Spring Data Repository
 * infrastructure AOT processing.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("UnusedReturnValue")
public class CodeContributionAssert extends AbstractAssert<CodeContributionAssert, GenerationContext> {

	public CodeContributionAssert(GenerationContext contribution) {
		super(contribution, CodeContributionAssert.class);
	}

	public CodeContributionAssert contributesReflectionFor(Class<?>... types) {

		for (Class<?> type : types) {
			assertThat(this.actual.getRuntimeHints()).describedAs("No reflection entry found for [%s]", type)
					.matches(RuntimeHintsPredicates.reflection().onType(type));
		}

		return this;
	}

	public CodeContributionAssert contributesReflectionFor(TypeReference typeReference) {

		assertThat(this.actual.getRuntimeHints()).describedAs(() -> {

			return "Existing hints: " + System.lineSeparator() + this.actual().getRuntimeHints().reflection().typeHints()
					.map(TypeHint::toString).map(" - "::concat).collect(Collectors.joining(System.lineSeparator()));

		}).matches(RuntimeHintsPredicates.reflection().onType(typeReference),
				String.format("No reflection entry found for [%s]", typeReference));

		return this;
	}

	public CodeContributionAssert contributesReflectionFor(String... types) {

		for (String type : types) {
			assertThat(this.actual.getRuntimeHints()).describedAs("No reflection entry found for [%s]", type)
					.matches(RuntimeHintsPredicates.reflection().onType(TypeReference.of(type)));
		}

		return this;
	}

	public CodeContributionAssert contributesReflectionFor(Method... methods) {

		for (Method method : methods) {
			assertThat(this.actual.getRuntimeHints()).describedAs("No reflection entry found for [%s]", method)
					.matches(RuntimeHintsPredicates.reflection().onMethod(method));
		}

		return this;
	}

	public CodeContributionAssert doesNotContributeReflectionFor(Class<?>... types) {

		for (Class<?> type : types) {
			assertThat(this.actual.getRuntimeHints()).describedAs("Reflection entry found for [%s]", type)
					.matches(RuntimeHintsPredicates.reflection().onType(type).negate());
		}

		return this;
	}

	public CodeContributionAssert contributesJdkProxyFor(Class<?> entryPoint) {

		assertThat(jdkProxiesFor(entryPoint).findFirst()).describedAs("No JDK proxy found for [%s]", entryPoint)
				.isPresent();

		return this;
	}

	public CodeContributionAssert doesNotContributeJdkProxyFor(Class<?> entryPoint) {

		assertThat(jdkProxiesFor(entryPoint).findFirst())
				.describedAs("Found JDK proxy matching [%s] though it should not be present", entryPoint).isNotPresent();

		return this;
	}

	public CodeContributionAssert contributesJdkProxy(Class<?>... proxyInterfaces) {

		assertThat(jdkProxiesFor(proxyInterfaces[0]))
				.describedAs("Unable to find JDK proxy matching [%s]", Arrays.asList(proxyInterfaces))
				.anySatisfy(it -> new JdkProxyAssert(it).matches(proxyInterfaces));

		return this;
	}

	public CodeContributionAssert doesNotContributeJdkProxy(Class<?>... proxyInterfaces) {

		assertThat(jdkProxiesFor(proxyInterfaces[0]))
				.describedAs("Found JDK proxy matching [%s] though it should not be present", Arrays.asList(proxyInterfaces))
				.noneSatisfy(it -> new JdkProxyAssert(it).matches(proxyInterfaces));

		return this;
	}

	private Stream<JdkProxyHint> jdkProxiesFor(Class<?> entryPoint) {

		return this.actual.getRuntimeHints().proxies().jdkProxyHints().filter(jdkProxyHint -> jdkProxyHint
				.getProxiedInterfaces().get(0).getCanonicalName().equals(entryPoint.getCanonicalName()));
	}

}
