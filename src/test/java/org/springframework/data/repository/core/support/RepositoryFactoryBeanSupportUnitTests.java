/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link RepositoryFactoryBeanSupport}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class RepositoryFactoryBeanSupportUnitTests {

	@Test // DATACMNS-341
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void setsConfiguredClassLoaderOnRepositoryFactory() {

		ClassLoader classLoader = mock(ClassLoader.class);

		RepositoryFactoryBeanSupport factoryBean = new DummyRepositoryFactoryBean(SampleRepository.class);
		factoryBean.setBeanClassLoader(classLoader);
		factoryBean.setLazyInit(true);
		factoryBean.afterPropertiesSet();

		Object factory = ReflectionTestUtils.getField(factoryBean, "factory");
		assertThat(ReflectionTestUtils.getField(factory, "classLoader")).isEqualTo(classLoader);
	}

	@Test // DATACMNS-432
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void initializationFailsWithMissingRepositoryInterface() {

		assertThatIllegalArgumentException()//
				.isThrownBy(() -> new DummyRepositoryFactoryBean(null))//
				.withMessageContaining("Repository interface");
	}

	@Test // DATACMNS-1117
	void returnsRepositoryInformationForFragmentSetup() {

		RepositoryFactoryBeanSupport<SampleWithQuerydslRepository, Object, Long> factoryBean = //
				new DummyRepositoryFactoryBean<>(SampleWithQuerydslRepository.class);
		factoryBean.afterPropertiesSet();

		RepositoryInformation information = factoryBean.getRepositoryInformation();

		assertThat(information.getQueryMethods()).isEmpty();
	}

	@Test // DATACMNS-1688
	void customizesRepositoryFactory() {

		RepositoryFactoryBeanSupport<SampleRepository, Object, Long> factoryBean = //
				new DummyRepositoryFactoryBean<>(SampleRepository.class);
		factoryBean.addRepositoryFactoryCustomizer(repositoryFactory -> repositoryFactory
				.addRepositoryProxyPostProcessor((factory, repositoryInformation) -> factory.addAdvice(new MethodInterceptor() {
					@Nullable
					@Override
					public Object invoke(@Nonnull MethodInvocation invocation) {
						throw new UnsupportedOperationException();
					}
				})));
		factoryBean.afterPropertiesSet();

		try {
			factoryBean.getObject().someMethod();
			fail("Missing UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			// all good
		}
	}

	@Test // DATACMNS-1345
	void reportsMappingContextUnavailableForPersistentEntityLookup() {

		RepositoryFactoryBeanSupport<SampleRepository, Object, Long> bean = new RepositoryFactoryBeanSupport<SampleRepository, Object, Long>(
				SampleRepository.class) {

			@Override
			protected RepositoryFactorySupport createRepositoryFactory() {
				return new DummyRepositoryFactory(mock(SampleRepository.class));
			}
		};

		bean.afterPropertiesSet();

		assertThatIllegalStateException() //
				.isThrownBy(() -> bean.getPersistentEntity());
	}

	interface SampleRepository extends Repository<Object, Long> {

		void someMethod();

	}

	interface SampleWithQuerydslRepository extends Repository<Object, Long>, QuerydslPredicateExecutor<Object> {}
}
