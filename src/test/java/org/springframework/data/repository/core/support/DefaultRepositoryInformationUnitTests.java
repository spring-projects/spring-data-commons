/*
 * Copyright 2013-2016 the original author or authors.
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

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsEmptyIterable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadataUnitTests.DummyGenericRepositorySupport;

/**
 * Unit tests for {@link DefaultRepositoryInformation}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRepositoryInformationUnitTests {

	@SuppressWarnings("rawtypes") static final Class<DummyGenericRepositorySupport> REPOSITORY = DummyGenericRepositorySupport.class;

	@Mock FooRepositoryCustom customImplementation;

	@Test
	public void discoversRepositoryBaseClassMethod() throws Exception {

		Method method = FooRepository.class.getMethod("findOne", Integer.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, REPOSITORY, null);

		Method reference = information.getTargetClassMethod(method);
		assertEquals(REPOSITORY, reference.getDeclaringClass());
		assertThat(reference.getName(), is("findOne"));
	}

	@Test
	public void discoveresNonRepositoryBaseClassMethod() throws Exception {

		Method method = FooRepository.class.getMethod("findOne", Long.class);

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		assertThat(information.getTargetClassMethod(method), is(method));
	}

	@Test
	public void discoversCustomlyImplementedCrudMethod() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				customImplementation.getClass());

		Method source = FooRepositoryCustom.class.getMethod("save", User.class);
		Method expected = customImplementation.getClass().getMethod("save", User.class);

		assertThat(information.getTargetClassMethod(source), is(expected));
	}

	@Test
	public void considersIntermediateMethodsAsFinderMethods() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		assertThat(information.hasCustomMethod(), is(false));
	}

	@Test
	public void discoversIntermediateMethodsAsBackingMethods() throws NoSuchMethodException, SecurityException {

		DefaultRepositoryMetadata metadata = new DefaultRepositoryMetadata(CustomRepository.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata,
				PagingAndSortingRepository.class, null);

		Method method = CustomRepository.class.getMethod("findAll", Pageable.class);
		assertThat(information.isBaseClassMethod(method), is(true));

		method = getMethodFrom(CustomRepository.class, "exists");
		assertThat(information.isBaseClassMethod(method), is(true));

		Matcher<Iterable<Method>> empty = iterableWithSize(0);
		assertThat(information.getQueryMethods(), is(empty));
	}

	/**
	 * @see DATACMNS-151
	 */
	@Test
	public void doesNotConsiderManuallyDefinedSaveMethodAQueryMethod() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(CustomRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, PagingAndSortingRepository.class,
				null);
		assertThat(information.getQueryMethods(), is(IsEmptyIterable.<Method> emptyIterable()));
	}

	/**
	 * @see DATACMNS-151
	 */
	@Test
	public void doesNotConsiderRedeclaredSaveMethodAQueryMethod() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		Method saveMethod = BaseRepository.class.getMethod("save", Object.class);
		Method deleteMethod = BaseRepository.class.getMethod("delete", Object.class);

		Iterable<Method> queryMethods = information.getQueryMethods();

		assertThat(queryMethods, not(hasItem(saveMethod)));
		assertThat(queryMethods, not(hasItem(deleteMethod)));
	}

	@Test
	public void onlyReturnsMostConcreteQueryMethod() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		Method intermediateMethod = BaseRepository.class.getMethod("genericMethodToOverride", String.class);
		Method concreteMethod = ConcreteRepository.class.getMethod("genericMethodToOverride", String.class);

		Iterable<Method> queryMethods = information.getQueryMethods();

		assertThat(queryMethods, hasItem(concreteMethod));
		assertThat(queryMethods, not(hasItem(intermediateMethod)));
	}

	/**
	 * @see DATACMNS-193
	 */
	@Test
	public void detectsQueryMethodCorrectly() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		Method queryMethod = getMethodFrom(ConcreteRepository.class, "findBySomethingDifferent");

		assertThat(information.getQueryMethods(), hasItem(queryMethod));
		assertThat(information.isQueryMethod(queryMethod), is(true));
	}

	/**
	 * @see DATACMNS-364
	 */
	@Test
	public void ignoresCrudMethodsAnnotatedWithQuery() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		Method method = BaseRepository.class.getMethod("findOne", Serializable.class);

		assertThat(information.getQueryMethods(), hasItem(method));
	}

	/**
	 * @see DATACMNS-385
	 */
	@Test
	public void findsTargetSaveForIterableIfEntityImplementsIterable() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(BossRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		Method method = BossRepository.class.getMethod("save", Iterable.class);
		Method reference = CrudRepository.class.getMethod("save", Iterable.class);

		assertThat(information.getTargetClassMethod(method), is(reference));
	}

	/**
	 * @see DATACMNS-441
	 */
	@Test
	public void getQueryShouldNotReturnAnyBridgeMethods() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(CustomDefaultRepositoryMethodsRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		for (Method method : information.getQueryMethods()) {
			assertFalse(method.isBridge());
		}
	}

	/**
	 * @see DATACMNS-854
	 */
	@Test
	public void discoversCustomlyImplementedCrudMethodWithGenerics() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				customImplementation.getClass());

		Method source = FooRepositoryCustom.class.getMethod("exists", Object.class);
		Method expected = customImplementation.getClass().getMethod("exists", Object.class);

		assertThat(information.getTargetClassMethod(source), is(expected));
	}

	/**
	 * @see DATACMNS-912
	 */
	@Test
	public void discoversCustomlyImplementedCrudMethodWithGenericParameters() throws Exception {

		SampleRepositoryImpl customImplementation = new SampleRepositoryImpl();
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(SampleRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, RepositoryFactorySupport.class,
				customImplementation.getClass());

		Method customBaseRepositoryMethod = SampleRepository.class.getMethod("save", Object.class);
		assertThat(information.isCustomMethod(customBaseRepositoryMethod), is(true));
	}

	/**
	 * @see DATACMNS-943
	 * @throws Exception
	 */
	@Test
	public void usesCorrectSaveOverload() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(DummyRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, null);

		Method method = DummyRepository.class.getMethod("save", Iterable.class);

		assertThat(information.getTargetClassMethod(method), is(CrudRepository.class.getMethod("save", Iterable.class)));
	}

	private static Method getMethodFrom(Class<?> type, String name) {

		for (Method method : type.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		return null;
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@QueryAnnotation
	@interface MyQuery {
	}

	interface FooRepository extends CrudRepository<User, Integer>, FooRepositoryCustom {

		// Redeclared method
		User findOne(Integer primaryKey);

		// Not a redeclared method
		User findOne(Long primaryKey);
	}

	interface FooSuperInterfaceWithGenerics<T> {
		boolean exists(T id);
	}

	interface FooRepositoryCustom extends FooSuperInterfaceWithGenerics<User> {
		User save(User user);
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		public String getAddress() {

			return null;
		}
	}

	static class Boss implements Iterable<User> {

		@Override
		public Iterator<User> iterator() {
			return Collections.<User> emptySet().iterator();
		}
	}

	interface BaseRepository<S, ID extends Serializable> extends CrudRepository<S, ID> {

		S findBySomething(String something);

		S genericMethodToOverride(String something);

		<K extends S> K save(K entity);

		void delete(S entity);

		@MyQuery
		S findOne(ID id);
	}

	interface ConcreteRepository extends BaseRepository<User, Integer> {

		User findBySomethingDifferent(String somethingDifferent);

		User genericMethodToOverride(String something);
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

		Object save(Object object);
	}

	interface BossRepository extends CrudRepository<Boss, Long> {}

	interface CustomDefaultRepositoryMethodsRepository extends CrudRepository<User, Integer> {

		@MyQuery
		List<User> findAll();
	}

	interface SampleRepository extends CrudRepository<Sample, Long> {}

	static class SampleRepositoryImpl {

		public <S extends Sample> S save(S entity) {
			return entity;
		}
	}

	static class Sample {}

	interface DummyRepository extends CrudRepository<User, Integer> {

		@Override
		<S extends User> List<S> save(Iterable<S> entites);
	}
}
