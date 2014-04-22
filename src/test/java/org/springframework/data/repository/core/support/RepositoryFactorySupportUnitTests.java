/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.springframework.data.repository.core.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.SpringVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

/**
 * Unit tests for {@link RepositoryFactorySupport}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryFactorySupportUnitTests {

	DummyRepositoryFactory factory;

	@Mock PagingAndSortingRepository<Object, Serializable> backingRepo;
	@Mock ObjectRepositoryCustom customImplementation;

	@Mock MyQueryCreationListener listener;
	@Mock PlainQueryCreationListener otherListener;

	@Before
	public void setUp() {
		factory = new DummyRepositoryFactory(backingRepo);
	}

	@Test
	public void invokesCustomQueryCreationListenerForSpecialRepositoryQueryOnly() throws Exception {

		factory.addQueryCreationListener(listener);
		factory.addQueryCreationListener(otherListener);

		factory.getRepository(ObjectRepository.class);

		verify(listener, times(1)).onCreation(Mockito.any(MyRepositoryQuery.class));
		verify(otherListener, times(2)).onCreation(Mockito.any(RepositoryQuery.class));
	}

	@Test
	public void routesCallToRedeclaredMethodIntoTarget() {

		ObjectRepository repository = factory.getRepository(ObjectRepository.class);
		repository.save(repository);

		verify(backingRepo, times(1)).save(Mockito.any(Object.class));
	}

	@Test
	public void invokesCustomMethodIfItRedeclaresACRUDOne() {

		ObjectRepository repository = factory.getRepository(ObjectRepository.class, customImplementation);
		repository.findOne(1);

		verify(customImplementation, times(1)).findOne(1);
		verify(backingRepo, times(0)).findOne(1);
	}

	@Test
	public void createsRepositoryInstanceWithCustomIntermediateRepository() {

		CustomRepository repository = factory.getRepository(CustomRepository.class);
		Pageable pageable = new PageRequest(0, 10);
		repository.findAll(pageable);

		verify(backingRepo, times(1)).findAll(pageable);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createsProxyForAnnotatedRepository() {

		Class<?> repositoryInterface = AnnotatedRepository.class;
		Class<? extends Repository<?, ?>> foo = (Class<? extends Repository<?, ?>>) repositoryInterface;

		assertThat(factory.getRepository(foo), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-341
	 */
	@Test
	public void usesDefaultClassLoaderIfNullConfigured() {

		factory.setBeanClassLoader(null);
		assertThat(ReflectionTestUtils.getField(factory, "classLoader"), is((Object) ClassUtils.getDefaultClassLoader()));
	}

	/**
	 * @see DATACMNS-489
	 */
	@Test
	public void wrapsExecutionResultIntoFutureIfConfigured() throws Exception {

		// TODO: Remove once Spring 3.2.9 is released.
		Assume.assumeThat(SpringVersion.getVersion().startsWith("4"), is(true));

		final Object reference = new Object();

		when(factory.queryOne.execute(Mockito.any(Object[].class))).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				return reference;
			}
		});

		AsyncRepository repository = factory.getRepository(AsyncRepository.class);

		AsyncAnnotationBeanPostProcessor processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());
		repository = (AsyncRepository) processor.postProcessAfterInitialization(repository, null);

		Future<Object> future = repository.findByFirstname("Foo");

		assertThat(future.isDone(), is(false));

		while (!future.isDone()) {
			Thread.sleep(300);
		}

		assertThat(future.get(), is(reference));

		verify(factory.queryOne, times(1)).execute(Mockito.any(Object[].class));
	}

	interface ObjectRepository extends Repository<Object, Serializable>, ObjectRepositoryCustom {

		Object findByClass(Class<?> clazz);

		Object findByFoo();

		Object save(Object entity);
	}

	interface ObjectRepositoryCustom {

		Object findOne(Serializable id);
	}

	interface PlainQueryCreationListener extends QueryCreationListener<RepositoryQuery> {

	}

	interface MyQueryCreationListener extends QueryCreationListener<MyRepositoryQuery> {

	}

	interface MyRepositoryQuery extends RepositoryQuery {

	}

	interface ReadOnlyRepository<T, ID extends Serializable> extends Repository<T, ID> {

		T findOne(ID id);

		Iterable<T> findAll();

		Page<T> findAll(Pageable pageable);

		List<T> findAll(Sort sort);

		boolean exists(ID id);

		long count();
	}

	interface CustomRepository extends ReadOnlyRepository<Object, Long> {

	}

	@RepositoryDefinition(domainClass = Object.class, idClass = Long.class)
	interface AnnotatedRepository {

	}

	interface AsyncRepository extends Repository<Object, Long> {

		@Async
		Future<Object> findByFirstname(String firstname);
	}
}
