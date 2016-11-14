/*
 * Copyright 2011-2013 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.ClassUtils;

import com.google.common.base.Optional;

/**
 * Unit tests for {@link DefaultRepositoryMetadata}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
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

	/**
	 * @see DATACMNS-406
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnparameterizedRepositoryInterface() {
		new DefaultRepositoryMetadata(Repository.class);
	}

	@Test
	public void looksUpDomainClassCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);
		assertThat(metadata.getDomainType()).isEqualTo(User.class);

		metadata = new DefaultRepositoryMetadata(SomeDao.class);
		assertThat(metadata.getDomainType()).isEqualTo(User.class);
	}

	@Test
	public void findsDomainClassOnExtensionOfDaoInterface() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ExtensionOfUserCustomExtendedDao.class);
		assertThat(metadata.getDomainType()).isEqualTo(User.class);
	}

	@Test
	public void detectsParameterizedEntitiesCorrectly() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(GenericEntityRepository.class);
		assertThat(metadata.getDomainType()).isEqualTo(GenericEntity.class);
	}

	@Test
	public void looksUpIdClassCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);
		assertThat(metadata.getIdType()).isEqualTo(Integer.class);
	}

	/**
	 * @see DATACMNS-442
	 */
	@Test
	public void detectsIdTypeOnIntermediateRepository() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		assertThat(metadata.getIdType()).isEqualTo(Long.class);
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void discoversDomainTypeOnReturnTypeWrapper() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(OptionalRepository.class);

		Method method = OptionalRepository.class.getMethod("findByEmailAddress", String.class);
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void discoversDomainTypeOnNestedReturnTypeWrapper() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(OptionalRepository.class);

		Method method = OptionalRepository.class.getMethod("findByLastname", String.class);
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	/**
	 * @see DATACMNS-501
	 */
	@Test
	public void discoversDomainAndIdTypeForIntermediateRepository() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(IdTypeFixingRepository.class);

		assertThat(metadata.getDomainType()).isEqualTo(Object.class);
		assertThat(metadata.getIdType()).isEqualTo(Long.class);
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

		public java.util.Optional<T> findOne(ID id) {
			return java.util.Optional.empty();
		}
	}

	/**
	 * Helper class to reproduce #256.
	 * 
	 * @author Oliver Gierke
	 */
	static class GenericEntity<T> {}

	static interface GenericEntityRepository extends CrudRepository<GenericEntity<String>, Long> {}

	static interface IdTypeFixingRepository<T> extends Repository<T, Long> {

	}

	static interface ConcreteRepository extends IdTypeFixingRepository<User> {

	}

	static interface OptionalRepository extends Repository<User, Long> {

		Optional<User> findByEmailAddress(String emailAddress);

		// Contrived example but to make sure recursive wrapper resolution works
		Optional<Optional<User>> findByLastname(String lastname);
	}
}
