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

import java.util.Arrays;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.ProtectedAccess;
import org.springframework.aot.hint.ClassProxyHint;
import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.javapoet.support.MultiStatement;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class CodeContributionAssert extends AbstractAssert<CodeContributionAssert, CodeContribution>
		implements CodeContribution {

	public CodeContributionAssert(CodeContribution contribution) {
		super(contribution, CodeContributionAssert.class);
	}

	public CodeContributionAssert doesNotContributeReflectionFor(Class<?>... types) {

		for (Class<?> type : types) {
			assertThat(this.actual.runtimeHints().reflection().getTypeHint(type))
					.describedAs("Reflection entry found for %s", type).isNull();
		}
		return this;
	}

	public CodeContributionAssert contributesReflectionFor(Class<?>... types) {

		for (Class<?> type : types) {
			assertThat(this.actual.runtimeHints().reflection().getTypeHint(type))
					.describedAs("No reflection entry found for %s", type).isNotNull();
		}
		return this;
	}

	public CodeContributionAssert contributesJdkProxyFor(Class<?> entryPoint) {
		assertThat(jdkProxiesFor(entryPoint).findFirst()).describedAs("No jdk proxy found for %s", entryPoint).isPresent();
		return this;
	}

	public CodeContributionAssert doesNotContributeJdkProxyFor(Class<?> entryPoint) {
		assertThat(jdkProxiesFor(entryPoint).findFirst()).describedAs("Found jdk proxy matching %s though it should not be present.", entryPoint).isNotPresent();
		return this;
	}

	public CodeContributionAssert doesNotContributeJdkProxy(Class<?>... proxyInterfaces) {

		assertThat(jdkProxiesFor(proxyInterfaces[0])).describedAs("Found jdk proxy matching %s though it should not be present.", Arrays.asList(proxyInterfaces)).noneSatisfy(it -> {
			new JdkProxyAssert(it).matches(proxyInterfaces);
		});
		return this;
	}

	public CodeContributionAssert contributesJdkProxy(Class<?>... proxyInterfaces) {

		assertThat(jdkProxiesFor(proxyInterfaces[0])).describedAs("Unable to find jdk proxy matching %s", Arrays.asList(proxyInterfaces)).anySatisfy(it -> {
			new JdkProxyAssert(it).matches(proxyInterfaces);
		});

		return this;
	}

	private Stream<JdkProxyHint> jdkProxiesFor(Class<?> entryPoint) {
		return this.actual.runtimeHints().proxies().jdkProxies().filter(jdkProxyHint -> {
			return jdkProxyHint.getProxiedInterfaces().get(0).getCanonicalName().equals(entryPoint.getCanonicalName());
		});
	}

	public CodeContributionAssert contributesClassProxy(Class<?>... proxyInterfaces) {

		assertThat(classProxiesFor(proxyInterfaces[0])).describedAs("Unable to find jdk proxy matching %s", Arrays.asList(proxyInterfaces)).anySatisfy(it -> {
			new ClassProxyAssert(it).matches(proxyInterfaces);
		});

		return this;
	}

	private Stream<ClassProxyHint> classProxiesFor(Class<?> entryPoint) {
		return this.actual.runtimeHints().proxies().classProxies().filter(jdkProxyHint -> {
			return jdkProxyHint.getProxiedInterfaces().get(0).getCanonicalName().equals(entryPoint.getCanonicalName());
		});
	}

	public MultiStatement statements() {
		return actual.statements();
	}

	public RuntimeHints runtimeHints() {
		return actual.runtimeHints();
	}

	public ProtectedAccess protectedAccess() {
		return actual.protectedAccess();
	}
}
