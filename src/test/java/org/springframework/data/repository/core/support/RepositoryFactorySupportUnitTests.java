/*
 * Copyright 2011-2024 the original author or authors.
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

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DummyRepositoryFactory.MyRepositoryQuery;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult.State;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.sample.User;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.TransactionalProxy;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Unit tests for {@link RepositoryFactorySupport}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Ariel Carrera
 * @author Johannes Englmeier
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryFactorySupportUnitTests {

	DummyRepositoryFactory factory;

	@Mock CrudRepository<Object, Object> backingRepo;
	@Mock ObjectRepositoryCustom customImplementation;

	@Mock MyQueryCreationListener listener;
	@Mock PlainQueryCreationListener otherListener;

	@Mock RepositoryProxyPostProcessor repositoryPostProcessor;
	@Mock RepositoryMethodInvocationListener invocationListener;

	@BeforeEach
	void setUp() {
		factory = new DummyRepositoryFactory(backingRepo);
	}

	@Test
	void invokesCustomQueryCreationListenerForSpecialRepositoryQueryOnly() {

		Mockito.reset(factory.strategy);

		when(factory.strategy.resolveQuery(any(Method.class), any(RepositoryMetadata.class), any(ProjectionFactory.class),
				any(NamedQueries.class))).thenReturn(factory.queryOne, factory.queryTwo);

		factory.addQueryCreationListener(listener);
		factory.addQueryCreationListener(otherListener);

		factory.getRepository(ObjectRepository.class);

		verify(listener, times(1)).onCreation(any(MyRepositoryQuery.class));
		verify(otherListener, times(3)).onCreation(any(RepositoryQuery.class));
	}

	@Test // DATACMNS-1538
	void invokesCustomRepositoryProxyPostProcessor() {

		factory.addRepositoryProxyPostProcessor(repositoryPostProcessor);
		factory.getRepository(ObjectRepository.class);

		verify(repositoryPostProcessor, times(1)).postProcess(any(ProxyFactory.class), any(RepositoryInformation.class));
	}

	@Test // DATACMNS-1764
	void routesCallToRedeclaredMethodIntoTarget() {

		factory.addInvocationListener(invocationListener);

		var repository = factory.getRepository(ObjectRepository.class);
		repository.save(repository);

		verify(backingRepo, times(1)).save(any(Object.class));
		verify(invocationListener).afterInvocation(any());
	}

	@Test
	void invokesCustomMethodIfItRedeclaresACRUDOne() {

		var repository = factory.getRepository(ObjectRepository.class, customImplementation);
		repository.findById(1);

		verify(customImplementation, times(1)).findById(1);
		verify(backingRepo, times(0)).findById(1);
	}

	@Test // DATACMNS-102
	void invokesCustomMethodCompositionMethodIfItRedeclaresACRUDOne() {

		var repository = factory.getRepository(ObjectRepository.class, RepositoryFragments.just(customImplementation));
		repository.findById(1);

		verify(customImplementation, times(1)).findById(1);
		verify(backingRepo, times(0)).findById(1);
	}

	@Test
	void createsRepositoryInstanceWithCustomIntermediateRepository() {

		var repository = factory.getRepository(CustomRepository.class);

		when(backingRepo.findAll()).thenReturn(new PageImpl<>(Collections.emptyList()));
		repository.findAll();

		verify(backingRepo, times(1)).findAll();
	}

	@Test
	@SuppressWarnings("unchecked")
	void createsProxyForAnnotatedRepository() {

		Class<?> repositoryInterface = AnnotatedRepository.class;
		var foo = (Class<? extends Repository<?, ?>>) repositoryInterface;

		assertThat(factory.getRepository(foo)).isNotNull();
	}

	@Test // DATACMNS-341
	void usesDefaultClassLoaderIfNullConfigured() {

		factory.setBeanClassLoader(null);
		assertThat(ReflectionTestUtils.getField(factory, "classLoader")).isEqualTo(ClassUtils.getDefaultClassLoader());
	}

	@Test // DATACMNS-489, DATACMNS-1764
	void wrapsExecutionResultIntoFutureIfConfigured() throws Exception {

		final var reference = new Object();

		when(factory.queryOne.execute(any(Object[].class))).then(invocation -> {
			Thread.sleep(500);
			return reference;
		});

		factory.addInvocationListener(invocationListener);

		var repository = factory.getRepository(ConvertingRepository.class);

		var processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());
		repository = (ConvertingRepository) processor.postProcessAfterInitialization(repository, null);

		var future = repository.findByFirstname("Foo");

		assertThat(future.isDone()).isFalse();

		while (!future.isDone()) {
			Thread.sleep(300);
		}

		assertThat(future.get()).isEqualTo(reference);

		verify(factory.queryOne, times(1)).execute(any(Object[].class));

		var captor = ArgumentCaptor.forClass(RepositoryMethodInvocation.class);
		verify(invocationListener).afterInvocation(captor.capture());
	}

	@Test // DATACMNS-1764, DATACMNS-1764
	void capturesFailureFromInvocation() {

		when(factory.queryOne.execute(any(Object[].class))).thenThrow(new IllegalStateException());

		factory.addInvocationListener(invocationListener);

		var repository = factory.getRepository(ConvertingRepository.class);

		try {
			repository.findByLastname("Foo");
			fail("Missing exception");
		} catch (IllegalStateException e) {}

		var captor = ArgumentCaptor.forClass(RepositoryMethodInvocation.class);
		verify(invocationListener).afterInvocation(captor.capture());

		var repositoryMethodInvocation = captor.getValue();
		assertThat(repositoryMethodInvocation.getDuration(TimeUnit.NANOSECONDS)).isGreaterThan(0);
		assertThat(repositoryMethodInvocation.getResult().getState()).isEqualTo(State.ERROR);
		assertThat(repositoryMethodInvocation.getResult().getError()).isInstanceOf(IllegalStateException.class);
	}

	@Test // GH-3090
	void capturesRepositoryMetadata() {

		record Metadata(RepositoryMethodContext context, MethodInvocation methodInvocation) {
		}

		when(factory.queryOne.execute(any(Object[].class)))
				.then(invocation -> new Metadata(RepositoryMethodContext.currentMethod(),
						ExposeInvocationInterceptor.currentInvocation()));

		factory.setExposeMetadata(true);

		var repository = factory.getRepository(ObjectRepository.class);
		var metadataByLastname = repository.findMetadataByLastname();

		assertThat(metadataByLastname).isInstanceOf(Metadata.class);

		Metadata metadata = (Metadata) metadataByLastname;
		assertThat(metadata.context().getMethod().getName()).isEqualTo("findMetadataByLastname");
		assertThat(metadata.context().getRepository().getDomainType()).isEqualTo(Object.class);
		assertThat(metadata.methodInvocation().getMethod().getName()).isEqualTo("findMetadataByLastname");
	}

	@Test // GH-3090
	void capturesRepositoryMetadataWithMetadataAccess() {

		record Metadata(RepositoryMethodContext context, MethodInvocation methodInvocation) {
		}

		when(factory.queryOne.execute(any(Object[].class)))
				.then(invocation -> new Metadata(RepositoryMethodContext.currentMethod(),
						ExposeInvocationInterceptor.currentInvocation()));

		var repository = factory.getRepository(ObjectRepository.class, new RepositoryMetadataAccess() {});
		var metadataByLastname = repository.findMetadataByLastname();

		assertThat(metadataByLastname).isInstanceOf(Metadata.class);

		Metadata metadata = (Metadata) metadataByLastname;
		assertThat(metadata.context().getMethod().getName()).isEqualTo("findMetadataByLastname");
		assertThat(metadata.context().getRepository().getDomainType()).isEqualTo(Object.class);
		assertThat(metadata.methodInvocation().getMethod().getName()).isEqualTo("findMetadataByLastname");
	}

	@Test // DATACMNS-509, DATACMNS-1764
	void convertsWithSameElementType() {

		var names = singletonList("Dave");

		factory.addInvocationListener(invocationListener);
		when(factory.queryOne.execute(any(Object[].class))).thenReturn(names);

		var repository = factory.getRepository(ConvertingRepository.class);
		var result = repository.convertListToStringSet();

		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isEqualTo("Dave");

		var captor = ArgumentCaptor.forClass(RepositoryMethodInvocation.class);
		verify(invocationListener).afterInvocation(captor.capture());
	}

	@Test // DATACMNS-509
	void convertsCollectionToOtherCollectionWithElementSuperType() {

		var names = singletonList("Dave");

		when(factory.queryOne.execute(any(Object[].class))).thenReturn(names);

		var repository = factory.getRepository(ConvertingRepository.class);
		var result = repository.convertListToObjectSet();

		assertThat(result).containsExactly("Dave");
	}

	@Test // DATACMNS-656
	void rejectsNullRepositoryProxyPostProcessor() {

		assertThatThrownBy( //
				() -> factory.addRepositoryProxyPostProcessor(null)) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessageContaining(RepositoryProxyPostProcessor.class.getSimpleName());
	}

	@Test // DATACMNS-715, SPR-13109
	void addsTransactionProxyInterfaceIfAvailable() {
		assertThat(factory.getRepository(SimpleRepository.class)).isInstanceOf(TransactionalProxy.class);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoCompletableFutureIfConfigured() throws Exception {

		var reference = new User();

		expect(prepareConvertingRepository(reference).findOneByFirstname("Foo"), reference);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoListenableFutureIfConfigured() throws Exception {

		var reference = new User();

		expect(prepareConvertingRepository(reference).findOneByLastname("Foo"), reference);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoCompletableFutureWithEntityCollectionIfConfigured() throws Exception {

		var reference = singletonList(new User());

		expect(prepareConvertingRepository(reference).readAllByFirstname("Foo"), reference);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoListenableFutureWithEntityCollectionIfConfigured() throws Exception {

		var reference = singletonList(new User());

		expect(prepareConvertingRepository(reference).readAllByLastname("Foo"), reference);
	}

	@Test // DATACMNS-763
	@SuppressWarnings("rawtypes")
	void rejectsRepositoryBaseClassWithInvalidConstructor() {

		var information = mock(RepositoryInformation.class);
		doReturn(CustomRepositoryBaseClass.class).when(information).getRepositoryBaseClass();
		var entityInformation = mock(EntityInformation.class);

		assertThatThrownBy( //
				() -> factory.getTargetRepositoryViaReflection(information, entityInformation, "Foo")) //
				.isInstanceOf(IllegalStateException.class) //
				.hasMessageContaining(entityInformation.getClass().getName()) //
				.hasMessageContaining(String.class.getName());
	}

	@Test
	void callsStaticMethodOnInterface() {

		var repository = factory.getRepository(ObjectRepository.class, customImplementation);

		assertThat(repository.staticMethodDelegate()).isEqualTo("OK");

		verifyNoInteractions(customImplementation);
		verifyNoInteractions(backingRepo);
	}

	@Test // DATACMNS-1154
	void considersRequiredReturnValue() {

		var repository = factory.getRepository(KotlinUserRepository.class);

		assertThatThrownBy( //
				() -> repository.findById("")) //
				.isInstanceOf(EmptyResultDataAccessException.class) //
				.hasMessageContaining("Result must not be null");

		assertThat(repository.findByUsername("")).isNull();
	}

	@Test // DATACMNS-1154
	void considersRequiredParameter() {

		var repository = factory.getRepository(ObjectRepository.class);

		assertThatThrownBy( //
				() -> repository.findByClass(null)) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessageContaining("must not be null");
	}

	@Test // DATACMNS-1154
	void shouldAllowVoidMethods() {

		var repository = factory.getRepository(ObjectRepository.class, backingRepo);

		repository.deleteAll();

		verify(backingRepo).deleteAll();
	}

	@Test // DATACMNS-1154
	void considersRequiredKotlinParameter() {

		var repository = factory.getRepository(KotlinUserRepository.class);

		assertThatThrownBy( //
				() -> repository.findById(null)) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessageContaining("must not be null"); //
	}

	@Test // DATACMNS-1154
	void considersRequiredKotlinNullableParameter() {

		var repository = factory.getRepository(KotlinUserRepository.class);

		assertThat(repository.findByOptionalId(null)).isNull();
	}

	@Test // DATACMNS-1197
	void considersNullabilityForKotlinInterfaceProperties() {

		var repository = factory.getRepository(KotlinUserRepository.class);

		assertThatThrownBy(repository::getFindRouteQuery).isInstanceOf(EmptyResultDataAccessException.class);
	}

	@Test // DATACMNS-1832
	void callsApplicationStartupOnRepositoryInitialization() {

		factory.getRepository(ObjectRepository.class, backingRepo);

		var startup = factory.getApplicationStartup();

		var orderedInvocation = Mockito.inOrder(startup);
		orderedInvocation.verify(startup).start("spring.data.repository.init");
		orderedInvocation.verify(startup).start("spring.data.repository.metadata");
		orderedInvocation.verify(startup).start("spring.data.repository.composition");
		orderedInvocation.verify(startup).start("spring.data.repository.target");
		orderedInvocation.verify(startup).start("spring.data.repository.proxy");
	}

	@Test // GH-2341
	void dummyRepositoryShouldsupportQuerydsl() {
		factory.getRepository(WithQuerydsl.class, backingRepo);
	}

	@Test // GH-2341
	void dummyRepositoryNotSupportingReactiveQuerydslShouldRaiseException() {
		assertThatThrownBy(() -> factory.getRepository(WithReactiveQuerydsl.class, backingRepo))
				.isInstanceOf(UnsupportedFragmentException.class).hasMessage(
						"Repository org.springframework.data.repository.core.support.RepositoryFactorySupportUnitTests$WithReactiveQuerydsl implements org.springframework.data.repository.query.ReactiveQueryByExampleExecutor but DummyRepositoryFactory does not support Reactive Query by Example");
	}

	@Test // GH-2341
	void dummyRepositoryNotSupportingQbeShouldRaiseException() {
		assertThatThrownBy(() -> factory.getRepository(WithQbe.class, backingRepo))
				.hasMessageContaining("does not support Query by Example");
	}

	@Test // GH-2341
	void dummyRepositoryNotSupportingReactiveQbeShouldRaiseException() {
		assertThatThrownBy(() -> factory.getRepository(WithReactiveQbe.class, backingRepo))
				.hasMessageContaining("does not support Reactive Query by Example");
	}

	@Test // GH-2341, GH-2395
	void derivedQueryMethodCannotBeImplemented() {

		var factory = new DummyRepositoryFactory(backingRepo) {
			@Override
			protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
					QueryMethodEvaluationContextProvider evaluationContextProvider) {
				return Optional.of((method, metadata, factory, namedQueries) -> {
					new PartTree(method.getName(), method.getReturnType());
					return null;
				});
			}
		};

		assertThatThrownBy(() -> factory.getRepository(WithQueryMethodUsingInvalidProperty.class))
				.isInstanceOf(QueryCreationException.class).hasMessageContaining("findAllByName")
				.hasMessageContaining("No property 'name' found for type 'Object'");
	}

	private ConvertingRepository prepareConvertingRepository(final Object expectedValue) {

		when(factory.queryOne.execute(any(Object[].class))).then(invocation -> {
			Thread.sleep(200);
			return expectedValue;
		});

		var processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());

		return (ConvertingRepository) processor
				.postProcessAfterInitialization(factory.getRepository(ConvertingRepository.class), null);
	}

	private void expect(Future<?> future, Object value) throws Exception {

		assertThat(future.isDone()).isFalse();

		while (!future.isDone()) {
			Thread.sleep(50);
		}

		assertThat(future.get()).isEqualTo(value);

		verify(factory.queryOne, times(1)).execute(any(Object[].class));
	}

	interface SimpleRepository extends Repository<Object, Serializable> {}

	interface ObjectRepository extends Repository<Object, Object>, ObjectRepositoryCustom {

		@Nullable
		Object findByClass(Class<?> clazz);

		@Nullable
		Object findByFoo();

		@Nullable
		Object save(Object entity);

		Object findMetadataByLastname();

		static String staticMethod() {
			return "OK";
		}

		default String staticMethodDelegate() {
			return staticMethod();
		}
	}

	interface ObjectRepositoryCustom {

		@Nullable
		Object findById(Object id);

		void deleteAll();
	}

	interface PlainQueryCreationListener extends QueryCreationListener<RepositoryQuery> {

	}

	interface MyQueryCreationListener extends QueryCreationListener<MyRepositoryQuery> {

	}

	interface ReadOnlyRepository<T, ID extends Serializable> extends Repository<T, ID> {

		Optional<T> findById(ID id);

		Iterable<T> findAll();

		Page<T> findAll(Pageable pageable);

		List<T> findAll(Sort sort);

		boolean existsById(ID id);

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

		Future<Object> findByLastname(String lastname);

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

		CustomRepositoryBaseClass(EntityInformation<?, ?> information) {}
	}

	interface WithQuerydsl extends Repository<Object, Long>, QuerydslPredicateExecutor<Object> {

	}

	interface WithReactiveQuerydsl extends Repository<Object, Long>, ReactiveQueryByExampleExecutor<Object> {

	}

	interface WithQbe extends Repository<Object, Long>, QueryByExampleExecutor<Object> {

	}

	interface WithReactiveQbe extends Repository<Object, Long>, ReactiveQueryByExampleExecutor<Object> {

	}

	interface WithQueryMethodUsingInvalidProperty extends Repository<Object, Long> {

		Object findAllByName();

	}

}
