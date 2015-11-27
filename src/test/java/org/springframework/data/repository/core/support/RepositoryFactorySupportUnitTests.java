/*
 * Copyright 2011-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.sample.User;
import org.springframework.data.util.Version;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Unit tests for {@link RepositoryFactorySupport}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryFactorySupportUnitTests {

	static final Version SPRING_VERSION = Version.parse(SpringVersion.getVersion());
	static final Version FOUR_DOT_TWO = new Version(4, 2);

	public @Rule ExpectedException exception = ExpectedException.none();

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

		Mockito.reset(factory.strategy);

		when(factory.strategy.resolveQuery(Mockito.any(Method.class), Mockito.any(RepositoryMetadata.class),
				Mockito.any(ProjectionFactory.class), Mockito.any(NamedQueries.class))).thenReturn(factory.queryOne,
						factory.queryTwo);

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

		final Object reference = new Object();

		when(factory.queryOne.execute(Mockito.any(Object[].class))).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				return reference;
			}
		});

		ConvertingRepository repository = factory.getRepository(ConvertingRepository.class);

		AsyncAnnotationBeanPostProcessor processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());
		repository = (ConvertingRepository) processor.postProcessAfterInitialization(repository, null);

		Future<Object> future = repository.findByFirstname("Foo");

		assertThat(future.isDone(), is(false));

		while (!future.isDone()) {
			Thread.sleep(300);
		}

		assertThat(future.get(), is(reference));

		verify(factory.queryOne, times(1)).execute(Mockito.any(Object[].class));
	}

	/**
	 * @see DATACMNS-509
	 */
	@Test
	public void convertsWithSameElementType() {

		List<String> names = Collections.singletonList("Dave");

		when(factory.queryOne.execute(Mockito.any(Object[].class))).thenReturn(names);

		ConvertingRepository repository = factory.getRepository(ConvertingRepository.class);
		Set<String> result = repository.convertListToStringSet();

		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is("Dave"));
	}

	/**
	 * @see DATACMNS-509
	 */
	@Test
	public void convertsCollectionToOtherCollectionWithElementSuperType() {

		List<String> names = Collections.singletonList("Dave");

		when(factory.queryOne.execute(Mockito.any(Object[].class))).thenReturn(names);

		ConvertingRepository repository = factory.getRepository(ConvertingRepository.class);
		Set<Object> result = repository.convertListToObjectSet();

		assertThat(result, hasSize(1));
		assertThat(result.iterator().next(), is((Object) "Dave"));
	}

	/**
	 * @see DATACMNS-656
	 */
	@Test
	public void rejectsNullRepositoryProxyPostProcessor() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(RepositoryProxyPostProcessor.class.getSimpleName());

		factory.addRepositoryProxyPostProcessor(null);
	}

	/**
	 * @see DATACMNS-715, SPR-13109
	 */
	@Test
	public void addsTransactionProxyInterfaceIfAvailable() throws Exception {

		try {

			Class<?> type = ClassUtils.forName("org.springframework.transaction.interceptor.TransactionalProxy", null);

			SimpleRepository repository = factory.getRepository(SimpleRepository.class);
			assertThat(repository, is(instanceOf(type)));

		} catch (ClassNotFoundException o_O) {
			Assume.assumeFalse(true);
		}
	}

	/**
	 * @see @see DATACMNS-714
	 */
	@Test
	public void wrapsExecutionResultIntoCompletableFutureIfConfigured() throws Exception {

		assumeThat(SPRING_VERSION.isGreaterThanOrEqualTo(FOUR_DOT_TWO), is(true));

		User reference = new User();

		expect(prepareConvertingRepository(reference).findOneByFirstname("Foo"), reference);
	}

	/**
	 * @see @see DATACMNS-714
	 */
	@Test
	public void wrapsExecutionResultIntoListenableFutureIfConfigured() throws Exception {

		assumeThat(SPRING_VERSION.isGreaterThanOrEqualTo(FOUR_DOT_TWO), is(true));

		User reference = new User();

		expect(prepareConvertingRepository(reference).findOneByLastname("Foo"), reference);
	}

	/**
	 * @see @see DATACMNS-714
	 */
	@Test
	public void wrapsExecutionResultIntoCompletableFutureWithEntityCollectionIfConfigured() throws Exception {

		assumeThat(SPRING_VERSION.isGreaterThanOrEqualTo(FOUR_DOT_TWO), is(true));

		List<User> reference = Arrays.asList(new User());

		expect(prepareConvertingRepository(reference).readAllByFirstname("Foo"), reference);
	}

	/**
	 * @see DATACMNS-714
	 */
	@Test
	public void wrapsExecutionResultIntoListenableFutureWithEntityCollectionIfConfigured() throws Exception {

		assumeThat(SPRING_VERSION.isGreaterThanOrEqualTo(FOUR_DOT_TWO), is(true));

		List<User> reference = Arrays.asList(new User());

		expect(prepareConvertingRepository(reference).readAllByLastname("Foo"), reference);
	}

	/**
	 * @see DATACMNS-763
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void rejectsRepositoryBaseClassWithInvalidConstructor() {

		RepositoryInformation information = mock(RepositoryInformation.class);
		doReturn(CustomRepositoryBaseClass.class).when(information).getRepositoryBaseClass();
		EntityInformation entityInformation = mock(EntityInformation.class);

		exception.expect(IllegalStateException.class);
		exception.expectMessage(entityInformation.getClass().getName());
		exception.expectMessage(String.class.getName());

		factory.getTargetRepositoryViaReflection(information, entityInformation, "Foo");
	}

	private ConvertingRepository prepareConvertingRepository(final Object expectedValue) {

		when(factory.queryOne.execute(Mockito.any(Object[].class))).then(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(200);
				return expectedValue;
			}
		});

		AsyncAnnotationBeanPostProcessor processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());

		return (ConvertingRepository) processor
				.postProcessAfterInitialization(factory.getRepository(ConvertingRepository.class), null);
	}

	private void expect(Future<?> future, Object value) throws Exception {

		assertThat(future.isDone(), is(false));

		while (!future.isDone()) {
			Thread.sleep(50);
		}

		assertThat(future.get(), is(value));

		verify(factory.queryOne, times(1)).execute(Mockito.any(Object[].class));
	}

	interface SimpleRepository extends Repository<Object, Serializable> {}

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

	interface ConvertingRepository extends Repository<Object, Long> {

		Set<String> convertListToStringSet();

		Set<Object> convertListToObjectSet();

		@Async
		Future<Object> findByFirstname(String firstname);

		// DATACMNS-714
		@Async
		CompletableFuture<User> findOneByFirstname(String firstname);

		// DATACMNS-714
		@Async
		CompletableFuture<List<User>> readAllByFirstname(String firstname);

		// DATACMNS-714
		@Async
		ListenableFuture<User> findOneByLastname(String lastname);

		// DATACMNS-714
		@Async
		ListenableFuture<List<User>> readAllByLastname(String lastname);
	}

	static class CustomRepositoryBaseClass {

		public CustomRepositoryBaseClass(EntityInformation<?, ?> information) {}
	}
}
