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
package org.springframework.data.repository.aot;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.repository.aot.RepositoryRegistrationAotContributionAssert.*;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.DecoratingProxy;
import org.springframework.data.aot.sample.ConfigWithCustomImplementation;
import org.springframework.data.aot.sample.ConfigWithCustomRepositoryBaseClass;
import org.springframework.data.aot.sample.ConfigWithFragments;
import org.springframework.data.aot.sample.ConfigWithQueryMethods;
import org.springframework.data.aot.sample.ConfigWithQueryMethods.ProjectionInterface;
import org.springframework.data.aot.sample.ConfigWithQuerydslPredicateExecutor;
import org.springframework.data.aot.sample.ConfigWithQuerydslPredicateExecutor.Person;
import org.springframework.data.aot.sample.ConfigWithSimpleCrudRepository;
import org.springframework.data.aot.sample.ConfigWithTransactionManagerPresent;
import org.springframework.data.aot.sample.ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository;
import org.springframework.data.aot.sample.QConfigWithQuerydslPredicateExecutor_Person;
import org.springframework.data.aot.sample.ReactiveConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.RepositoryRegistrationAotContribution;
import org.springframework.data.repository.config.RepositoryRegistrationAotProcessor;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.transaction.interceptor.TransactionalProxy;

/**
 * Integration Tests for {@link RepositoryRegistrationAotProcessor}.
 *
 * @author Christoph Strobl
 * @author John Blum
 */
public class RepositoryRegistrationAotProcessorIntegrationTests {

	@Test // GH-2593
	void simpleRepositoryNoTxManagerNoKotlinNoReactiveNoComponent() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithSimpleCrudRepository.class).forRepository(ConfigWithSimpleCrudRepository.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithSimpleCrudRepository.MyRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(ConfigWithSimpleCrudRepository.MyRepo.class) // repository interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(ConfigWithSimpleCrudRepository.Person.class) // repository domain type
							.contributesJdkProxy(ConfigWithSimpleCrudRepository.MyRepo.class, SpringProxy.class, Advised.class,
									DecoratingProxy.class) //
							.contributesJdkProxy(ConfigWithSimpleCrudRepository.MyRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class)
							.doesNotContributeJdkProxy(ConfigWithSimpleCrudRepository.MyRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test // GH-2593
	void simpleRepositoryWithTxManagerNoKotlinNoReactiveNoComponent() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithTransactionManagerPresent.class).forRepository(ConfigWithTransactionManagerPresent.MyTxRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithTransactionManagerPresent.MyTxRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(ConfigWithTransactionManagerPresent.MyTxRepo.class) // repository
																																																		// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(ConfigWithTransactionManagerPresent.Person.class) // repository domain type

							// proxies
							.contributesJdkProxy(ConfigWithTransactionManagerPresent.MyTxRepo.class, SpringProxy.class, Advised.class,
									DecoratingProxy.class)
							.contributesJdkProxy(ConfigWithTransactionManagerPresent.MyTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class)
							.doesNotContributeJdkProxy(ConfigWithTransactionManagerPresent.MyTxRepo.class, Repository.class,
									TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test // GH-2593
	void simpleRepositoryWithTxManagerNoKotlinNoReactiveButComponent() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.class).forRepository(
						ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(
						ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution
							.contributesReflectionFor(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class) // repository
							// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.Person.class) // domain type

							// proxies
							.contributesJdkProxy(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class,
									SpringProxy.class, Advised.class, DecoratingProxy.class)
							.contributesJdkProxy(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class,
									Repository.class, TransactionalProxy.class, Advised.class, DecoratingProxy.class)
							.contributesJdkProxy(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class,
									Repository.class, TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test // GH-2593
	void contributesFragmentsCorrectly() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithFragments.class).forRepository(ConfigWithFragments.RepositoryWithFragments.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithFragments.RepositoryWithFragments.class) //
				.hasFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(ConfigWithFragments.RepositoryWithFragments.class) // repository
							// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(ConfigWithFragments.Person.class) // repository domain type

							// fragments
							.contributesReflectionFor(ConfigWithFragments.CustomImplInterface1.class,
									ConfigWithFragments.CustomImplInterface1Impl.class)
							.contributesReflectionFor(ConfigWithFragments.CustomImplInterface2.class,
									ConfigWithFragments.CustomImplInterface2Impl.class)

							// proxies
							.contributesJdkProxy(ConfigWithFragments.RepositoryWithFragments.class, SpringProxy.class, Advised.class,
									DecoratingProxy.class)
							.doesNotContributeJdkProxy(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class,
									Repository.class, TransactionalProxy.class, Advised.class, DecoratingProxy.class)
							.doesNotContributeJdkProxy(
									ConfigWithTransactionManagerPresentAndAtComponentAnnotatedRepository.MyComponentTxRepo.class,
									Repository.class, TransactionalProxy.class, Advised.class, DecoratingProxy.class, Serializable.class);
				});
	}

	@Test // GH-2593
	void contributesCustomImplementationCorrectly() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithCustomImplementation.class)
						.forRepository(ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class) //
				.hasFragments() //
				.codeContributionSatisfies(contribution -> { //
					contribution.contributesReflectionFor(ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class) // repository
							// interface
							.contributesReflectionFor(PagingAndSortingRepository.class) // base repository
							.contributesReflectionFor(ConfigWithCustomImplementation.Person.class) // repository domain type

							// fragments
							.contributesReflectionFor(ConfigWithCustomImplementation.CustomImplInterface.class,
									ConfigWithCustomImplementation.RepositoryWithCustomImplementationImpl.class);

				});
	}

	@Test // GH-2593
	void contributesDomainTypeAndReachableTypesCorrectly() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithSimpleCrudRepository.class).forRepository(ConfigWithSimpleCrudRepository.MyRepo.class);

		assertThatContribution(repositoryBeanContribution).codeContributionSatisfies(
				contribution -> contribution.contributesReflectionFor(ConfigWithSimpleCrudRepository.Person.class,
						ConfigWithSimpleCrudRepository.Address.class));
	}

	@Test // GH-2593
	void contributesReactiveRepositoryCorrectly() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(ReactiveConfig.class)
				.forRepository(ReactiveConfig.CustomerRepositoryReactive.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ReactiveConfig.CustomerRepositoryReactive.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					// interface
					contribution.contributesReflectionFor(ReactiveConfig.CustomerRepositoryReactive.class) // repository
							.contributesReflectionFor(ReactiveSortingRepository.class) // base repo class
							.contributesReflectionFor(ReactiveConfig.Person.class); // repository domain type
				});
	}

	@Test // GH-2593
	void contributesRepositoryBaseClassCorrectly() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithCustomRepositoryBaseClass.class)
						.forRepository(ConfigWithCustomRepositoryBaseClass.CustomerRepositoryWithCustomBaseRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.targetRepositoryTypeIs(ConfigWithCustomRepositoryBaseClass.CustomerRepositoryWithCustomBaseRepo.class) //
				.hasNoFragments() //
				.codeContributionSatisfies(contribution -> { //
					// interface
					contribution
							.contributesReflectionFor(ConfigWithCustomRepositoryBaseClass.CustomerRepositoryWithCustomBaseRepo.class) // repository
							.contributesReflectionFor(ConfigWithCustomRepositoryBaseClass.RepoBaseClass.class) // base repo class
							.contributesReflectionFor(ConfigWithCustomRepositoryBaseClass.Person.class); // repository domain type
				});
	}

	@Test // GH-2593
	void contributesTypesFromQueryMethods() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithQueryMethods.class).forRepository(ConfigWithQueryMethods.CustomerRepositoryWithQueryMethods.class);

		assertThatContribution(repositoryBeanContribution)
				.codeContributionSatisfies(contribution -> contribution.contributesReflectionFor(ProjectionInterface.class));
	}

	@Test // GH-2593
	void contributesProxiesForPotentialProjections() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithQueryMethods.class).forRepository(ConfigWithQueryMethods.CustomerRepositoryWithQueryMethods.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {

					contribution.contributesJdkProxyFor(ProjectionInterface.class);
					contribution.doesNotContributeJdkProxyFor(Page.class);
					contribution.doesNotContributeJdkProxyFor(ConfigWithQueryMethods.Person.class);
				});
	}

	@Test // GH-2593
	void doesNotCareAboutNonDataAnnotations() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithSimpleCrudRepository.class).forRepository(ConfigWithSimpleCrudRepository.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {
					contribution.doesNotContributeReflectionFor(javax.annotation.Nullable.class);
					contribution.doesNotContributeJdkProxyFor(javax.annotation.Nullable.class);
				});
	}

	@Test // GH-2721
	void registersQTypeIfPresent() {

		RepositoryRegistrationAotContribution repositoryBeanContribution = computeAotConfiguration(
				ConfigWithQuerydslPredicateExecutor.class).forRepository(ConfigWithQuerydslPredicateExecutor.MyRepo.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {
					contribution.contributesReflectionFor(Person.class);
					contribution.contributesReflectionFor(
							QConfigWithQuerydslPredicateExecutor_Person.class);
				});
	}

	RepositoryRegistrationAotContributionBuilder computeAotConfiguration(Class<?> configuration) {
		return computeAotConfiguration(configuration, new AnnotationConfigApplicationContext());
	}

	RepositoryRegistrationAotContributionBuilder computeAotConfiguration(Class<?> configuration,
			AnnotationConfigApplicationContext applicationContext) {

		applicationContext.register(configuration);
		applicationContext.refreshForAotProcessing(new RuntimeHints());

		return repositoryType -> {

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

			BeanRegistrationAotContribution beanContribution = repositoryAotProcessor.processAheadOfTime(bean);

			assertThat(beanContribution).isInstanceOf(RepositoryRegistrationAotContribution.class);

			return (RepositoryRegistrationAotContribution) beanContribution;
		};
	}

	@FunctionalInterface
	interface RepositoryRegistrationAotContributionBuilder {
		RepositoryRegistrationAotContribution forRepository(Class<?> repositoryInterface);
	}
}
