/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
 in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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

/**
 * Unit tests for {@link Repositories}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoriesUnitTests {

	GenericApplicationContext context;

	@Rule public ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("addressRepository", getRepositoryBeanDefinition(AddressRepository.class));
		beanFactory.registerBeanDefinition("personRepository", getRepositoryBeanDefinition(PersonRepository.class));

		context = new GenericApplicationContext(beanFactory);
		context.refresh();
	}

	private AbstractBeanDefinition getRepositoryBeanDefinition(Class<?> repositoryInterface) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DummyRepositoryFactoryBean.class);
		builder.addPropertyValue("repositoryInterface", repositoryInterface);

		return builder.getBeanDefinition();
	}

	@Test
	public void doesNotConsiderCrudRepositoriesOnly() {

		Repositories repositories = new Repositories(context);

		assertThat(repositories.hasRepositoryFor(Person.class), is(true));
		assertThat(repositories.hasRepositoryFor(Address.class), is(true));
	}

	@Test
	public void doesNotFindInformationForNonManagedDomainClass() {
		Repositories repositories = new Repositories(context);
		assertThat(repositories.hasRepositoryFor(String.class), is(false));
		assertThat(repositories.getRepositoryFor(String.class), is(nullValue()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullBeanFactory() {
		new Repositories(null);
	}

	/**
	 * @see DATACMNS-256
	 */
	@Test
	public void exposesPersistentEntityForDomainTypes() {

		Repositories repositories = new Repositories(context);
		assertThat(repositories.getPersistentEntity(Person.class), is(notNullValue()));
		assertThat(repositories.getPersistentEntity(Address.class), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-374
	 */
	@Test
	public void shouldThrowMeaningfulExceptionWhenTheRepositoryForAGivenDomainClassCannotBeFound() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(containsString("No repository found for domain class: "));

		Repositories repositories = new Repositories(context);
		repositories.getCrudInvoker(EntityWithoutRepository.class);
	}

	class Person {

	}

	class Address {

	}

	class EntityWithoutRepository {

	}

	interface PersonRepository extends CrudRepository<Person, Long> {

	}

	interface AddressRepository extends Repository<Address, Long> {

	}

	static class SampleRepoFactoryInformation<T, S extends Serializable> implements RepositoryFactoryInformation<T, S> {

		private final RepositoryMetadata repositoryMetadata;
		private final SampleMappingContext mappingContext;

		public SampleRepoFactoryInformation(Class<?> repositoryInterface) {
			this.repositoryMetadata = new DefaultRepositoryMetadata(repositoryInterface);
			this.mappingContext = new SampleMappingContext();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public EntityInformation<T, S> getEntityInformation() {
			return new DummyEntityInformation(repositoryMetadata.getDomainType());
		}

		public RepositoryInformation getRepositoryInformation() {
			return new DummyRepositoryInformation(repositoryMetadata.getRepositoryInterface());
		}

		public PersistentEntity<?, ?> getPersistentEntity() {
			return mappingContext.getPersistentEntity(repositoryMetadata.getDomainType());
		}

		public List<QueryMethod> getQueryMethods() {
			return Collections.emptyList();
		}
	}
}
