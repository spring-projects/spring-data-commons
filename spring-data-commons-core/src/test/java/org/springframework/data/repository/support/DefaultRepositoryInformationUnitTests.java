package org.springframework.data.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.DefaultRepositoryMetadataUnitTests.DummyGenericRepositorySupport;

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
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, CrudRepository.class, customImplementation.getClass());
		
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


	interface BaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {
		
		T findBySomething(String something);
	}
	
	interface ConcreteRepository extends BaseRepository<User, Integer> {
		
		User findBySomethingDifferent(String somethingDifferent);
	}
}
