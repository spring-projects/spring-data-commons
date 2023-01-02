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
import static org.springframework.data.aot.BeanRegistrationContributionAssert.*;

import reactor.core.publisher.Mono;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.ReactiveAuditorAware;

/**
 * @author Christoph Strobl
 */
class AuditingBeanRegistrationAotProcessorUnitTests {

	DefaultListableBeanFactory beanFactory;

	@BeforeEach
	void beforeEach() {
		beanFactory = new DefaultListableBeanFactory();
	}

	@Test // GH-2593
	void contributesProxyForAuditorAwareInterface() {

		String beanName = "auditorAware";
		beanFactory.registerBeanDefinition("auditorAware",
				BeanDefinitionBuilder.rootBeanDefinition(MyAuditorAware.class).getBeanDefinition());

		BeanRegistrationAotContribution beanRegistrationAotContribution = new AuditingBeanRegistrationAotProcessor()
				.processAheadOfTime(RegisteredBean.of(beanFactory, beanName));
		assertThatAotContribution(beanRegistrationAotContribution).codeContributionSatisfies(code -> {
			code.contributesJdkProxyFor(AuditorAware.class);
		});
	}

	@Test // GH-2593
	void contributesProxyForReactiveAuditorAwareInterface() {

		String beanName = "auditorAware";
		beanFactory.registerBeanDefinition("auditorAware",
				BeanDefinitionBuilder.rootBeanDefinition(MyReactiveAuditorAware.class).getBeanDefinition());

		BeanRegistrationAotContribution beanRegistrationAotContribution = new AuditingBeanRegistrationAotProcessor()
				.processAheadOfTime(RegisteredBean.of(beanFactory, beanName));
		assertThatAotContribution(beanRegistrationAotContribution).codeContributionSatisfies(code -> {
			code.contributesJdkProxyFor(ReactiveAuditorAware.class);
		});
	}

	@Test // GH-2593
	void ignoresNonAuditorAware() {

		String beanName = "auditorAware";
		beanFactory.registerBeanDefinition("auditorAware",
				BeanDefinitionBuilder.rootBeanDefinition(Nothing.class).getBeanDefinition());

		BeanRegistrationAotContribution beanRegistrationAotContribution = new AuditingBeanRegistrationAotProcessor()
				.processAheadOfTime(RegisteredBean.of(beanFactory, beanName));
		assertThat(beanRegistrationAotContribution).isNull();
	}

	static class Nothing {
		public Optional getCurrentAuditor() {
			return Optional.empty();
		}
	}

	static class MyAuditorAware implements AuditorAware {

		@Override
		public Optional getCurrentAuditor() {
			return Optional.empty();
		}
	}

	static class MyReactiveAuditorAware implements ReactiveAuditorAware {

		@Override
		public Mono getCurrentAuditor() {
			return null;
		}
	}
}
