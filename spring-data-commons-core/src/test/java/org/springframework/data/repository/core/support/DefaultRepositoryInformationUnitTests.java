package org.springframework.data.repository.core.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRepositoryInformationUnitTests {

	@SuppressWarnings("rawtypes")
	static final Class<DummyGenericRepositorySupport> REPOSITORY = DummyGenericRepositorySupport.class;

	@Mock
	FooRepositoryCustom customImplementation;

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
		assertThat(queryMethods, is(Matchers.<Method> iterableWithSize(2)));
	}

	private Method getMethodFrom(Class<?> type, String name) {
		for (Method method : type.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		return null;
	}

	interface FooRepository extends CrudRepository<User, Integer>, FooRepositoryCustom {

		// Redeclared method
		User findOne(Integer primaryKey);

		// Not a redeclared method
		User findOne(Long primaryKey);
	}

	interface FooRepositoryCustom {

		User save(User user);
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		public String getAddress() {

			return null;
		}
	}

	interface BaseRepository<S, ID extends Serializable> extends CrudRepository<S, ID> {

		S findBySomething(String something);

		<K extends S> K save(K entity);

		void delete(S entity);
	}

	interface ConcreteRepository extends BaseRepository<User, Integer> {

		User findBySomethingDifferent(String somethingDifferent);
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
}
