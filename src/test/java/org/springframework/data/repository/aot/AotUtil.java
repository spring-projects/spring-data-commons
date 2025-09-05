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
package org.springframework.data.repository.aot;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.repository.config.RepositoryRegistrationAotContribution;
import org.springframework.data.repository.config.RepositoryRegistrationAotProcessor;

/**
 * Utility class to create {@link RepositoryRegistrationAotContribution} instances for a given configuration class.
 *
 * @author Mark Paluch
 */
class AotUtil {

	static RepositoryRegistrationAotContributionBuilder contributionFor(Class<?> configuration) {
		return contributionFor(configuration, new AnnotationConfigApplicationContext());
	}

	static RepositoryRegistrationAotContributionBuilder contributionFor(Class<?> configuration,
			AnnotationConfigApplicationContext applicationContext) {

		applicationContext.register(configuration);
		applicationContext.refreshForAotProcessing(new RuntimeHints());

		return repositoryTypes -> {

			BeanRegistrationAotContribution beanContribution = null;

			for (Class<?> repositoryType : repositoryTypes) {

				String[] repositoryBeanNames = applicationContext.getBeanNamesForType(repositoryType);

				assertThat(repositoryBeanNames)
						.describedAs("Unable to find repository [%s] in configuration [%s]", repositoryType, configuration)
						.hasSize(1);

				String repositoryBeanName = repositoryBeanNames[0];

				ConfigurableListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();

				RepositoryRegistrationAotProcessor repositoryAotProcessor = applicationContext
						.getBean(RepositoryRegistrationAotProcessor.class);

				repositoryAotProcessor.setBeanFactory(beanFactory);

				RegisteredBean bean = RegisteredBean.of(beanFactory, repositoryBeanName);
				beanContribution = repositoryAotProcessor.processAheadOfTime(bean);
			}

			assertThat(beanContribution).isInstanceOf(RepositoryRegistrationAotContribution.class);
			return (RepositoryRegistrationAotContribution) beanContribution;
		};
	}

	@FunctionalInterface
	interface RepositoryRegistrationAotContributionBuilder {
		default RepositoryRegistrationAotContribution forRepository(Class<?> repositoryInterface) {
			return forRepositories(repositoryInterface);
		}

		RepositoryRegistrationAotContribution forRepositories(Class<?>... repositoryInterface);
	}
}
