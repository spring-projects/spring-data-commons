/*
 * Copyright 2011-2012 the original author or authors.
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

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collection;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.ClassUtils;

/**
 * Unit tests for {@link DefaultRepositoryMetadata}.
 * 
 * @author Oliver Gierke
 */
public class DefaultRepositoryMetadataUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullRepositoryInterface() {

		new DefaultRepositoryMetadata(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonInterface() {
		new DefaultRepositoryMetadata(Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonRepositoryInterface() {
		new DefaultRepositoryMetadata(Collection.class);
	}

	@Test
	public void looksUpDomainClassCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);
		assertEquals(User.class, metadata.getDomainType());

		metadata = new DefaultRepositoryMetadata(SomeDao.class);
		assertEquals(User.class, metadata.getDomainType());
	}

	@Test
	public void findsDomainClassOnExtensionOfDaoInterface() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ExtensionOfUserCustomExtendedDao.class);
		assertEquals(User.class, metadata.getDomainType());
	}

	@Test
	public void detectsParameterizedEntitiesCorrectly() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(GenericEntityRepository.class);
		assertEquals(GenericEntity.class, metadata.getDomainType());
	}

	@Test
	public void looksUpIdClassCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		assertEquals(Integer.class, metadata.getIdType());
	}

	@SuppressWarnings("unused")
	private class User {

		private String firstname;

		public String getAddress() {

			return null;
		}
	}

	static interface UserRepository extends CrudRepository<User, Integer> {

	}

	/**
	 * Sample interface to serve two purposes:
	 * <ol>
	 * <li>Check that {@link ClassUtils#getDomainClass(Class)} skips non {@link GenericDao} interfaces</li>
	 * <li>Check that {@link ClassUtils#getDomainClass(Class)} traverses interface hierarchy</li>
	 * </ol>
	 * 
	 * @author Oliver Gierke
	 */
	private interface SomeDao extends Serializable, UserRepository {

		Page<User> findByFirstname(Pageable pageable, String firstname);
	}

	/**
	 * Sample interface to test recursive lookup of domain class.
	 * 
	 * @author Oliver Gierke
	 */
	static interface ExtensionOfUserCustomExtendedDao extends UserCustomExtendedRepository {

	}

	static interface UserCustomExtendedRepository extends CrudRepository<User, Integer> {

	}

	static abstract class DummyGenericRepositorySupport<T, ID extends Serializable> implements CrudRepository<T, ID> {

		public T findOne(ID id) {

			return null;
		}
	}

	/**
	 * Helper class to reproduce #256.
	 * 
	 * @author Oliver Gierke
	 */
	static class GenericEntity<T> {
	}

	static interface GenericEntityRepository extends CrudRepository<GenericEntity<String>, Long> {

	}
}
