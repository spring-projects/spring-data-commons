package org.springframework.data.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.support.DefaultRepositoryMetadataUnitTests.DummyGenericRepositorySupport;

/**
 * @author Oliver Gierke
 */
public class DefaultRepositoryInformationUnitTests {

	@SuppressWarnings("rawtypes")
	static final Class<DummyGenericRepositorySupport> REPOSITORY = DummyGenericRepositorySupport.class;

	@Test
	public void discoversRepositoryBaseClassMethod() throws Exception {

		Method method = FooDao.class.getMethod("findOne", Integer.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooDao.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, REPOSITORY);

		Method reference = information.getBaseClassMethodFor(method);
		assertEquals(REPOSITORY, reference.getDeclaringClass());
		assertThat(reference.getName(), is("findOne"));
	}

	@Test
	public void discoveresNonRepositoryBaseClassMethod() throws Exception {

		Method method = FooDao.class.getMethod("findOne", Long.class);

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(FooDao.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, Repository.class);

		assertThat(information.getBaseClassMethodFor(method), is(method));
	}

	private static interface FooDao extends Repository<User, Integer> {

		// Redeclared method
		User findOne(Integer primaryKey);

		// Not a redeclared method
		User findOne(Long primaryKey);
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		public String getAddress() {

			return null;
		}
	}
}
