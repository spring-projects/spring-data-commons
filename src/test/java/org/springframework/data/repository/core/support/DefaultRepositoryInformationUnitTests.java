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

import lombok.experimental.Delegate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultRepositoryInformationUnitTests {

	@SuppressWarnings("rawtypes") static final Class<DummyGenericRepositorySupport> REPOSITORY = DummyGenericRepositorySupport.class;

	@Mock FooRepositoryCustom customImplementation;

	@Test
	void discoversRepositoryBaseClassMethod() throws Exception {

		Method method = FooRepository.class.getMethod("findById", Integer.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, REPOSITORY,
				RepositoryComposition.empty().withMethodLookup(MethodLookups.forRepositoryTypes(metadata)));

		Method reference = information.getTargetClassMethod(method);
		assertThat(reference.getDeclaringClass()).isEqualTo(REPOSITORY);
		assertThat(reference.getName()).isEqualTo("findById");
	}

	@Test
	void discoveresNonRepositoryBaseClassMethod() throws Exception {

		Method method = FooRepository.class.getMethod("findById", Long.class);

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty().withMethodLookup(MethodLookups.forRepositoryTypes(metadata)));

		assertThat(information.getTargetClassMethod(method)).isEqualTo(method);
	}

	@Test
	void discoversCustomlyImplementedCrudMethod() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.just(customImplementation));

		Method source = FooRepositoryCustom.class.getMethod("save", User.class);
		Method expected = customImplementation.getClass().getMethod("save", User.class);

		assertThat(information.getTargetClassMethod(source)).isEqualTo(expected);
	}

	@Test
	void considersIntermediateMethodsAsFinderMethods() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		assertThat(information.hasCustomMethod()).isFalse();
	}

	@Test
	void discoversIntermediateMethodsAsBackingMethods() throws NoSuchMethodException, SecurityException {

		DefaultRepositoryMetadata metadata = new DefaultRepositoryMetadata(CustomRepository.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata,
				PagingAndSortingRepository.class, RepositoryComposition.empty());

		Method method = CustomRepository.class.getMethod("findAll", Pageable.class);
		assertThat(information.isBaseClassMethod(method)).isTrue();

		method = getMethodFrom(CustomRepository.class, "existsById");

		assertThat(information.isBaseClassMethod(method)).isTrue();
		assertThat(information.getQueryMethods()).isEmpty();
	}

	@Test // DATACMNS-151
	void doesNotConsiderManuallyDefinedSaveMethodAQueryMethod() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(CustomRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, PagingAndSortingRepository.class,
				RepositoryComposition.empty());

		assertThat(information.getQueryMethods()).isEmpty();
	}

	@Test // DATACMNS-151
	void doesNotConsiderRedeclaredSaveMethodAQueryMethod() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		Method saveMethod = BaseRepository.class.getMethod("save", Object.class);
		Method deleteMethod = BaseRepository.class.getMethod("delete", Object.class);

		Iterable<Method> queryMethods = information.getQueryMethods();

		assertThat(queryMethods).doesNotContain(saveMethod, deleteMethod);
	}

	@Test
	void onlyReturnsMostConcreteQueryMethod() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		Method intermediateMethod = BaseRepository.class.getMethod("genericMethodToOverride", String.class);
		Method concreteMethod = ConcreteRepository.class.getMethod("genericMethodToOverride", String.class);

		Iterable<Method> queryMethods = information.getQueryMethods();

		assertThat(queryMethods).contains(concreteMethod);
		assertThat(queryMethods).doesNotContain(intermediateMethod);
	}

	@Test // DATACMNS-193
	void detectsQueryMethodCorrectly() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		Method queryMethod = getMethodFrom(ConcreteRepository.class, "findBySomethingDifferent");

		assertThat(information.getQueryMethods()).contains(queryMethod);
		assertThat(information.isQueryMethod(queryMethod)).isTrue();
	}

	@Test // DATACMNS-364
	void ignoresCrudMethodsAnnotatedWithQuery() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		Method method = BaseRepository.class.getMethod("findById", Object.class);

		assertThat(information.getQueryMethods()).contains(method);
	}

	@Test // DATACMNS-385
	void findsTargetSaveForIterableIfEntityImplementsIterable() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(BossRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		Method method = BossRepository.class.getMethod("saveAll", Iterable.class);
		Method reference = CrudRepository.class.getMethod("saveAll", Iterable.class);

		assertThat(information.getTargetClassMethod(method)).isEqualTo(reference);
	}

	@Test // DATACMNS-441
	void getQueryShouldNotReturnAnyBridgeMethods() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(CustomDefaultRepositoryMethodsRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.empty());

		assertThat(information.getQueryMethods()).allMatch(method -> !method.isBridge());
	}

	@Test // DATACMNS-854
	void discoversCustomlyImplementedCrudMethodWithGenerics() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.just(customImplementation));

		Method source = FooRepositoryCustom.class.getMethod("exists", Object.class);
		Method expected = customImplementation.getClass().getMethod("exists", Object.class);

		assertThat(information.getTargetClassMethod(source)).isEqualTo(expected);
	}

	@Test // DATACMNS-912
	void discoversCustomlyImplementedCrudMethodWithGenericParameters() throws Exception {

		GenericsSaveRepositoryImpl customImplementation = new GenericsSaveRepositoryImpl();
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(GenericsSaveRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, RepositoryFactorySupport.class,
				RepositoryComposition.just(customImplementation).withMethodLookup(MethodLookups.forRepositoryTypes(metadata)));

		Method customBaseRepositoryMethod = GenericsSaveRepository.class.getMethod("save", Object.class);
		assertThat(information.isCustomMethod(customBaseRepositoryMethod)).isTrue();
	}

	@Test // DATACMNS-939
	void ignoresStaticMethod() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.just(customImplementation));

		Method method = FooRepository.class.getMethod("staticMethod");

		assertThat(information.getQueryMethods()).doesNotContain(method);
	}

	@Test // DATACMNS-939
	void ignoresDefaultMethod() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class,
				RepositoryComposition.just(customImplementation));

		Method method = FooRepository.class.getMethod("defaultMethod");

		assertThat(information.getQueryMethods()).doesNotContain(method);
	}

	@Test // DATACMNS-943
	void usesCorrectSaveOverload() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(DummyRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, DummyRepositoryImpl.class,
				RepositoryComposition.empty());

		Method method = DummyRepository.class.getMethod("saveAll", Iterable.class);

		assertThat(information.getTargetClassMethod(method))
				.isEqualTo(DummyRepositoryImpl.class.getMethod("saveAll", Iterable.class));
	}

	@Test // DATACMNS-1008, DATACMNS-912, DATACMNS-854
	void discoversCustomlyImplementedCrudMethodWithoutGenericParameters() throws Exception {

		SimpleSaveRepositoryImpl customImplementation = new SimpleSaveRepositoryImpl();
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(SimpleSaveRepository.class);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, RepositoryFactorySupport.class,
				RepositoryComposition.just(customImplementation).withMethodLookup(MethodLookups.forRepositoryTypes(metadata)));

		Method customBaseRepositoryMethod = SimpleSaveRepository.class.getMethod("save", Object.class);
		assertThat(information.isCustomMethod(customBaseRepositoryMethod)).isTrue();
	}

	private static Method getMethodFrom(Class<?> type, String name) {

		return Arrays.stream(type.getMethods())//
				.filter(method -> method.getName().equals(name))//
				.findFirst()//
				.orElseThrow(() -> new IllegalStateException("No method found with name ".concat(name).concat("!")));
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@QueryAnnotation
	@interface MyQuery {
	}

	interface FooRepository extends CrudRepository<User, Integer>, FooRepositoryCustom {

		// Redeclared method
		Optional<User> findById(Integer primaryKey);

		// Not a redeclared method
		User findById(Long primaryKey);

		static void staticMethod() {}

		default void defaultMethod() {}
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

		String getAddress() {

			return null;
		}
	}

	static class Boss implements Iterable<User> {

		@Override
		public Iterator<User> iterator() {
			return Collections.<User> emptySet().iterator();
		}
	}

	interface BaseRepository<S, ID> extends CrudRepository<S, ID> {

		S findBySomething(String something);

		S genericMethodToOverride(String something);

		<K extends S> K save(K entity);

		void delete(S entity);

		@MyQuery
		Optional<S> findById(ID id);
	}

	interface ConcreteRepository extends BaseRepository<User, Integer> {

		User findBySomethingDifferent(String somethingDifferent);

		User genericMethodToOverride(String something);
	}

	interface ReadOnlyRepository<T, ID> extends Repository<T, ID> {

		T findById(ID id);

		Iterable<T> findAll();

		Page<T> findAll(Pageable pageable);

		List<T> findAll(Sort sort);

		boolean existsById(ID id);

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

	// DATACMNS-854, DATACMNS-912

	interface GenericsSaveRepository extends CrudRepository<Sample, Long> {}

	static class GenericsSaveRepositoryImpl {

		public <T extends Sample> T save(T entity) {
			return entity;
		}
	}

	static class Sample {}

	interface DummyRepository extends CrudRepository<User, Integer> {

		@Override
		<S extends User> List<S> saveAll(Iterable<S> entites);
	}

	static class DummyRepositoryImpl<T, ID> implements CrudRepository<T, ID> {

		private @Delegate CrudRepository<T, ID> delegate;
	}

	// DATACMNS-1008, DATACMNS-854, DATACMNS-912

	interface SimpleSaveRepository extends CrudRepository<Sample, Long> {}

	static class SimpleSaveRepositoryImpl {

		public Sample save(Sample entity) {
			return entity;
		}
	}
}
