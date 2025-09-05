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
package org.springframework.data.repository.cdi;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.spel.EvaluationContextProvider;

/**
 * Unit tests for {@link CdiRepositoryBean}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Ariel Carrera
 * @author Kyrylo Merzlikin
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CdiRepositoryBeanUnitTests {

	static final String PASSIVATION_ID = "jakarta.enterprise.inject.Default:org.springframework.data.repository.cdi.CdiRepositoryBeanUnitTests$SampleRepository";

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

		var bean = new DummyCdiRepositoryBean<SampleRepository>(NO_ANNOTATIONS, SampleRepository.class,
				beanManager);

		assertThat(bean.getBeanClass()).isEqualTo(SampleRepository.class);
		assertThat(bean.getName()).isEqualTo(SampleRepository.class.getName());
		assertThat(bean.isNullable()).isFalse();
	}

	@Test
	void returnsAllImplementedTypes() {

		var bean = new DummyCdiRepositoryBean<SampleRepository>(NO_ANNOTATIONS, SampleRepository.class,
				beanManager);

		var types = bean.getTypes();
		assertThat(types).containsExactlyInAnyOrder(SampleRepository.class, Repository.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void detectsStereotypes() {

		var bean = new DummyCdiRepositoryBean<StereotypedSampleRepository>(NO_ANNOTATIONS,
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

		var detector = mock(CustomRepositoryImplementationDetector.class);

		var bean = new CdiRepositoryBean<SampleRepository>(SINGLE_ANNOTATION,
				SampleRepository.class, beanManager, Optional.of(detector)) {

			@Override
			protected SampleRepository create(CreationalContext<SampleRepository> creationalContext,
					Class<SampleRepository> repositoryType) {
				return create(() -> new DummyRepositoryFactory(new Object()), repositoryType);
			}
		};

		bean.create(mock(CreationalContext.class), SampleRepository.class);

		var captor = ArgumentCaptor
				.forClass(ImplementationLookupConfiguration.class);

		verify(detector, times(2)).detectCustomImplementation(captor.capture());

		var configuration = captor.getAllValues().get(0);

		assertThat(configuration.getImplementationBeanName()).isEqualTo("cdiRepositoryBeanUnitTests.SampleRepositoryImpl");
		assertThat(configuration.getImplementationClassName()).isEqualTo("SampleRepositoryImpl");
	}

	@Test // DATACMNS-1233
	void appliesRepositoryConfiguration() {

		var bean = new DummyCdiRepositoryBean<SampleRepository>(NO_ANNOTATIONS,
				SampleRepository.class, beanManager) {
			@Override
			protected CdiRepositoryConfiguration lookupConfiguration(BeanManager beanManager, Set qualifiers) {
				return RepositoryConfiguration.INSTANCE;
			}
		};

		bean.applyConfiguration(repositoryFactory);

		verify(repositoryFactory).setEvaluationContextProvider(EvaluationContextProvider.DEFAULT);
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
		protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {
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
		public Optional<EvaluationContextProvider> getEvaluationContextProvider() {
			return Optional.of(EvaluationContextProvider.DEFAULT);
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
