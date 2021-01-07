/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.repository.cdi;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.config.ImplementationLookupConfiguration;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.PropertiesBasedNamedQueries;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Unit tests for {@link CdiRepositoryBean}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Ariel Carrera
 * @author Kyrylo Merzlikin
 */
@ExtendWith(MockitoExtension.class)
class CdiRepositoryBeanUnitTests {

	static final String PASSIVATION_ID = "javax.enterprise.inject.Default:org.springframework.data.repository.cdi.CdiRepositoryBeanUnitTests$SampleRepository";

	static final Set<Annotation> NO_ANNOTATIONS = emptySet();
	static final Set<Annotation> SINGLE_ANNOTATION = singleton(
			new CdiRepositoryExtensionSupport.DefaultAnnotationLiteral());

	@Mock BeanManager beanManager;
	@Mock RepositoryFactorySupport repositoryFactory;

	@Test
	void voidRejectsNullQualifiers() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DummyCdiRepositoryBean<>(null, SampleRepository.class, beanManager));
	}

	@Test
	void voidRejectsNullRepositoryType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DummyCdiRepositoryBean<>(NO_ANNOTATIONS, null, beanManager));
	}

	@Test
	void voidRejectsNullBeanManager() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DummyCdiRepositoryBean<>(NO_ANNOTATIONS, SampleRepository.class, null));
	}

	@Test
	void returnsBasicMetadata() {

		DummyCdiRepositoryBean<SampleRepository> bean = new DummyCdiRepositoryBean<>(NO_ANNOTATIONS, SampleRepository.class,
				beanManager);

		assertThat(bean.getBeanClass()).isEqualTo(SampleRepository.class);
		assertThat(bean.getName()).isEqualTo(SampleRepository.class.getName());
		assertThat(bean.isNullable()).isFalse();
	}

	@Test
	void returnsAllImplementedTypes() {

		DummyCdiRepositoryBean<SampleRepository> bean = new DummyCdiRepositoryBean<>(NO_ANNOTATIONS, SampleRepository.class,
				beanManager);

		Set<Type> types = bean.getTypes();
		assertThat(types).containsExactlyInAnyOrder(SampleRepository.class, Repository.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void detectsStereotypes() {

		DummyCdiRepositoryBean<StereotypedSampleRepository> bean = new DummyCdiRepositoryBean<>(NO_ANNOTATIONS,
				StereotypedSampleRepository.class, beanManager);

		assertThat(bean.getStereotypes()).containsExactly(StereotypeAnnotation.class);
	}

	@Test // DATACMNS-299
	@SuppressWarnings("rawtypes")
	void scopeDefaultsToApplicationScoped() {

		Bean<SampleRepository> bean = new DummyCdiRepositoryBean<>(NO_ANNOTATIONS, SampleRepository.class, beanManager);
		assertThat(bean.getScope()).isEqualTo(ApplicationScoped.class);
	}

	@Test // DATACMNS-322
	void createsPassivationId() {

		CdiRepositoryBean<SampleRepository> bean = new DummyCdiRepositoryBean<>(SINGLE_ANNOTATION, SampleRepository.class,
				beanManager
		);

		assertThat(bean.getId()).isEqualTo(PASSIVATION_ID);
	}

	@Test // DATACMNS-764, DATACMNS-1754
	void passesCorrectBeanNameToTheImplementationDetector() {

		CustomRepositoryImplementationDetector detector = mock(CustomRepositoryImplementationDetector.class);

		CdiRepositoryBean<SampleRepository> bean = new CdiRepositoryBean<SampleRepository>(SINGLE_ANNOTATION,
				SampleRepository.class, beanManager, Optional.of(detector)) {

			@Override
			protected SampleRepository create(CreationalContext<SampleRepository> creationalContext,
					Class<SampleRepository> repositoryType, Optional<Object> customImplementation) {
				return null;
			}
		};

		bean.create(mock(CreationalContext.class), SampleRepository.class);

		ArgumentCaptor<ImplementationLookupConfiguration> captor = ArgumentCaptor
				.forClass(ImplementationLookupConfiguration.class);

		verify(detector).detectCustomImplementation(captor.capture());

		ImplementationLookupConfiguration configuration = captor.getValue();

		assertThat(configuration.getImplementationBeanName()).isEqualTo("cdiRepositoryBeanUnitTests.SampleRepositoryImpl");
		assertThat(configuration.getImplementationClassName()).isEqualTo("SampleRepositoryImpl");
	}

	@Test // DATACMNS-1233
	void appliesRepositoryConfiguration() {

		DummyCdiRepositoryBean<SampleRepository> bean = new DummyCdiRepositoryBean<SampleRepository>(NO_ANNOTATIONS,
				SampleRepository.class, beanManager) {
			@Override
			protected CdiRepositoryConfiguration lookupConfiguration(BeanManager beanManager, Set qualifiers) {
				return RepositoryConfiguration.INSTANCE;
			}
		};

		bean.applyConfiguration(repositoryFactory);

		verify(repositoryFactory).setEvaluationContextProvider(QueryMethodEvaluationContextProvider.DEFAULT);
		verify(repositoryFactory).setNamedQueries(PropertiesBasedNamedQueries.EMPTY);
		verify(repositoryFactory).setRepositoryBaseClass(String.class);
		verify(repositoryFactory).setQueryLookupStrategyKey(Key.CREATE);
		verify(repositoryFactory).addRepositoryProxyPostProcessor(DummyRepositoryProxyPostProcessor.INSTANCE);
		verify(repositoryFactory).addQueryCreationListener(DummyQueryCreationListener.INSTANCE);
	}

	static class DummyCdiRepositoryBean<T> extends CdiRepositoryBean<T> {

		DummyCdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {
			super(qualifiers, repositoryType, beanManager);
		}

		@Override
		protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType,
				Optional<Object> customImplementation) {
			return null;
		}
	}

	@Named("namedRepository")
	interface SampleRepository extends Repository<Object, Serializable> {

	}

	@StereotypeAnnotation
	interface StereotypedSampleRepository {

	}

	enum RepositoryConfiguration implements CdiRepositoryConfiguration {

		INSTANCE;

		@Override
		public Optional<QueryMethodEvaluationContextProvider> getEvaluationContextProvider() {
			return Optional.of(QueryMethodEvaluationContextProvider.DEFAULT);
		}

		@Override
		public Optional<NamedQueries> getNamedQueries() {
			return Optional.of(PropertiesBasedNamedQueries.EMPTY);
		}

		@Override
		public Optional<Key> getQueryLookupStrategy() {
			return Optional.of(Key.CREATE);
		}

		@Override
		public Optional<Class<?>> getRepositoryBeanClass() {
			return Optional.of(String.class);
		}

		@Override
		public List<RepositoryProxyPostProcessor> getRepositoryProxyPostProcessors() {
			return singletonList(DummyRepositoryProxyPostProcessor.INSTANCE);
		}

		@Override
		public List<QueryCreationListener<?>> getQueryCreationListeners() {
			return singletonList(DummyQueryCreationListener.INSTANCE);
		}

	}

	static class DummyRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {

		static final DummyRepositoryProxyPostProcessor INSTANCE = new DummyRepositoryProxyPostProcessor();

		@Override
		public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {}
	}

	static class DummyQueryCreationListener implements QueryCreationListener<RepositoryQuery> {

		static final DummyQueryCreationListener INSTANCE = new DummyQueryCreationListener();

		@Override
		public void onCreation(RepositoryQuery query) {}
	}
}
