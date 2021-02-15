/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.support.DummyEntityInformation;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.DummyRepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.ClassUtils;

/**
 * Unit tests for {@link Repositories}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jan Zeppenfeld
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoriesUnitTests {

	GenericApplicationContext context;

	@BeforeEach
	void setUp() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("addressRepository", getRepositoryBeanDefinition(AddressRepository.class));
		beanFactory.registerBeanDefinition("personRepository", getRepositoryBeanDefinition(PersonRepository.class));

		context = new GenericApplicationContext(beanFactory);
		context.refresh();
	}

	private AbstractBeanDefinition getRepositoryBeanDefinition(Class<?> repositoryInterface) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DummyRepositoryFactoryBean.class);
		builder.addConstructorArgValue(repositoryInterface);

		return builder.getBeanDefinition();
	}

	@Test
	void doesNotConsiderCrudRepositoriesOnly() {

		Repositories repositories = new Repositories(context);

		assertThat(repositories.hasRepositoryFor(Person.class)).isTrue();
		assertThat(repositories.hasRepositoryFor(Address.class)).isTrue();
	}

	@Test
	void doesNotFindInformationForNonManagedDomainClass() {
		Repositories repositories = new Repositories(context);
		assertThat(repositories.hasRepositoryFor(String.class)).isFalse();
		assertThat(repositories.getRepositoryFor(String.class)).isNotPresent();
	}

	@Test
	void rejectsNullBeanFactory() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Repositories(null));
	}

	@Test // DATACMNS-256
	void exposesPersistentEntityForDomainTypes() {

		Repositories repositories = new Repositories(context);
		assertThat(repositories.getPersistentEntity(Person.class)).isNotNull();
		assertThat(repositories.getPersistentEntity(Address.class)).isNotNull();
	}

	@Test // DATACMNS-634
	void findsRepositoryForSubTypes() {
		assertThat(new Repositories(context).getPersistentEntity(AdvancedAddress.class)).isNotNull();
	}

	@Test // DATACMNS-673
	void discoversRepositoryForAlternativeDomainType() {

		RepositoryMetadata metadata = new CustomRepositoryMetadata(SampleRepository.class);
		RepositoryFactoryInformation<?, ?> information = new SampleRepoFactoryInformation<>(metadata);

		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("foo", information);
		context.refresh();

		Repositories repositories = new Repositories(context);

		assertThat(repositories.getRepositoryFor(Sample.class)).isNotNull();
		assertThat(repositories.getRepositoryFor(SampleEntity.class)).isNotNull();

		context.close();
	}

	@Test // DATACMNS-794
	void exposesRepositoryFactoryInformationForRepository() {

		Optional<RepositoryInformation> information = new Repositories(context)
				.getRepositoryInformation(PersonRepository.class);

		assertThat(information)
				.hasValueSatisfying(it -> assertThat(it.getRepositoryInterface()).isEqualTo(PersonRepository.class));
	}

	@Test // DATACMNS-1215
	void exposesRepositoryForProxyType() {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new Person());
		factory.setProxyTargetClass(true);

		Object proxy = factory.getProxy();

		assertThat(ClassUtils.isCglibProxy(proxy)).isTrue();

		Repositories repositories = new Repositories(context);

		assertThat(repositories.hasRepositoryFor(proxy.getClass())).isTrue();
		assertThat(repositories.getRepositoryFor(proxy.getClass())).isNotEmpty();
	}

	@Test // DATACMNS-1448
	void keepsPrimaryRepositoryInCaseOfMultipleOnes() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("first", getRepositoryBeanDefinition(FirstRepository.class));

		AbstractBeanDefinition definition = getRepositoryBeanDefinition(PrimaryRepository.class);
		definition.setPrimary(true);

		beanFactory.registerBeanDefinition("primary", definition);
		beanFactory.registerBeanDefinition("third", getRepositoryBeanDefinition(ThirdRepository.class));

		context = new GenericApplicationContext(beanFactory);
		context.refresh();

		Repositories repositories = new Repositories(beanFactory);

		assertThat(repositories.getRepositoryFor(SomeEntity.class)).hasValueSatisfying(it -> {
			assertThat(it).isInstanceOf(PrimaryRepository.class);
		});
	}

	@Test // DATACMNS-1142
	void keepsPrimaryRepositoryInCaseOfMultipleOnesIfContextIsNotAConfigurableListableBeanFactory() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("first", getRepositoryBeanDefinition(FirstRepository.class));

		AbstractBeanDefinition definition = getRepositoryBeanDefinition(PrimaryRepository.class);
		definition.setPrimary(true);

		beanFactory.registerBeanDefinition("primary", definition);
		beanFactory.registerBeanDefinition("third", getRepositoryBeanDefinition(ThirdRepository.class));

		context = new GenericApplicationContext(beanFactory);
		context.refresh();

		Repositories repositories = new Repositories(context);

		assertThat(repositories.getRepositoryFor(SomeEntity.class)).hasValueSatisfying(it -> {
			assertThat(it).isInstanceOf(PrimaryRepository.class);
		});
	}

	class Person {}

	class Address {}

	class AdvancedAddress extends Address {}

	interface PersonRepository extends CrudRepository<Person, Long> {}

	interface AddressRepository extends Repository<Address, Long> {}

	static class SampleRepoFactoryInformation<T, S extends Serializable> implements RepositoryFactoryInformation<T, S> {

		private final RepositoryMetadata repositoryMetadata;
		private final SampleMappingContext mappingContext;

		SampleRepoFactoryInformation(Class<?> repositoryInterface) {
			this(new DefaultRepositoryMetadata(repositoryInterface));
		}

		SampleRepoFactoryInformation(RepositoryMetadata metadata) {
			this.repositoryMetadata = metadata;
			this.mappingContext = new SampleMappingContext();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public EntityInformation<T, S> getEntityInformation() {
			return new DummyEntityInformation(repositoryMetadata.getDomainType());
		}

		public RepositoryInformation getRepositoryInformation() {
			return new DummyRepositoryInformation(repositoryMetadata);
		}

		public PersistentEntity<?, ?> getPersistentEntity() {
			return mappingContext.getRequiredPersistentEntity(repositoryMetadata.getDomainType());
		}

		public List<QueryMethod> getQueryMethods() {
			return Collections.emptyList();
		}
	}

	static class CustomRepositoryMetadata extends DefaultRepositoryMetadata {

		private final Class<?> domainType;

		/**
		 * @param repositoryInterface
		 */
		CustomRepositoryMetadata(Class<?> repositoryInterface) {

			super(repositoryInterface);

			String domainType = super.getDomainType().getName().concat("Entity");

			try {
				this.domainType = ClassUtils.forName(domainType, CustomRepositoryMetadata.class.getClassLoader());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.DefaultRepositoryMetadata#getDomainType()
		 */
		@Override
		public Class<?> getDomainType() {
			return this.domainType;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.AbstractRepositoryMetadata#getAlternativeDomainTypes()
		 */
		@Override
		public Set<Class<?>> getAlternativeDomainTypes() {
			return Collections.singleton(super.getDomainType());
		}
	}

	interface Sample {}

	static class SampleEntity implements Sample {}

	interface SampleRepository extends Repository<Sample, Long> {}

	interface SomeEntity {}

	interface FirstRepository extends Repository<SomeEntity, Long> {}

	@Primary
	interface PrimaryRepository extends Repository<SomeEntity, Long> {}

	interface ThirdRepository extends Repository<SomeEntity, Long> {}
}
