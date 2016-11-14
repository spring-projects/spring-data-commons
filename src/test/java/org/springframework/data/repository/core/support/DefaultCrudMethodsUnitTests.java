/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Unit tests dor {@link DefaultCrudMethods}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class DefaultCrudMethodsUnitTests {

	@Test
	public void detectsMethodsOnCrudRepository() throws Exception {

		Class<DomainCrudRepository> type = DomainCrudRepository.class;

		assertFindAllMethodOn(type, type.getMethod("findAll"));
		assertDeleteMethodOn(type, type.getMethod("delete", Serializable.class));
		assertSaveMethodPresent(type, true);
	}

	@Test
	public void detectsMethodsOnPagingAndSortingRepository() throws Exception {

		Class<DomainPagingAndSortingRepository> type = DomainPagingAndSortingRepository.class;

		assertFindAllMethodOn(type, type.getMethod("findAll", Pageable.class));
		assertDeleteMethodOn(type, type.getMethod("delete", Serializable.class));
		assertSaveMethodPresent(type, true);
	}

	@Test
	public void detectsFindAllWithSortParameterOnSortingRepository() throws Exception {

		Class<RepositoryWithCustomSortingFindAll> type = RepositoryWithCustomSortingFindAll.class;

		assertFindAllMethodOn(type, type.getMethod("findAll", Sort.class));
		assertSaveMethodPresent(type, false);
	}

	/**
	 * @see DATACMNS-393
	 */
	@Test
	public void selectsFindAllWithSortParameterOnRepositoryAmongUnsuitableAlternatives() throws Exception {

		Class<RepositoryWithInvalidPagingFallbackToSortFindAll> type = RepositoryWithInvalidPagingFallbackToSortFindAll.class;

		assertFindAllMethodOn(type, type.getMethod("findAll", Sort.class));
		assertSaveMethodPresent(type, false);
	}

	@Test
	public void detectsMethodsOnCustomRepository() throws Exception {

		Class<RepositoryWithCustomSortingAndPagingFindAll> type = RepositoryWithCustomSortingAndPagingFindAll.class;
		assertFindAllMethodOn(type, type.getMethod("findAll", Pageable.class));

		Class<RepositoryWithIterableDeleteOnly> type1 = RepositoryWithIterableDeleteOnly.class;
		assertDeleteMethodOn(type1, type1.getMethod("delete", Iterable.class));
	}

	@Test
	public void doesNotDetectInvalidlyDeclaredMethods() throws Exception {

		Class<RepositoryWithInvalidPagingFindAll> type = RepositoryWithInvalidPagingFindAll.class;
		assertFindAllMethodOn(type, null);
	}

	/**
	 * @see DATACMNS-393
	 */
	@Test
	public void detectsOverloadedMethodsCorrectly() throws Exception {

		Class<RepositoryWithAllCrudMethodOverloaded> type = RepositoryWithAllCrudMethodOverloaded.class;
		assertFindOneMethodOn(type, type.getDeclaredMethod("findOne", Long.class));
		assertDeleteMethodOn(type, type.getDeclaredMethod("delete", Long.class));
		assertSaveMethodOn(type, type.getDeclaredMethod("save", Domain.class));
		assertFindAllMethodOn(type, type.getDeclaredMethod("findAll"));
	}

	/**
	 * @see DATACMNS-393
	 */
	@Test
	public void ignoresWrongOverloadedMethods() throws Exception {

		Class<RepositoryWithAllCrudMethodOverloadedWrong> type = RepositoryWithAllCrudMethodOverloadedWrong.class;
		assertFindOneMethodOn(type, CrudRepository.class.getDeclaredMethod("findOne", Serializable.class));
		assertDeleteMethodOn(type, CrudRepository.class.getDeclaredMethod("delete", Serializable.class));
		assertSaveMethodOn(type, CrudRepository.class.getDeclaredMethod("save", Object.class));
		assertFindAllMethodOn(type, CrudRepository.class.getDeclaredMethod("findAll"));
	}

	/**
	 * @see DATACMNS-464
	 */
	@Test
	public void detectsCustomSaveMethod() throws Exception {
		assertSaveMethodOn(RepositoryWithCustomSave.class, RepositoryWithCustomSave.class.getMethod("save", Domain.class));
	}

	/**
	 * @see DATACMNS-539
	 */
	@Test
	public void detectsOverriddenDeleteMethodForEntity() throws Exception {

		assertDeleteMethodOn(RepositoryWithDeleteMethodForEntityOverloaded.class,
				RepositoryWithDeleteMethodForEntityOverloaded.class.getMethod("delete", Domain.class));
	}

	/**
	 * @see DATACMNS-619
	 */
	@Test
	public void exposedMethodsAreAccessible() {

		CrudMethods methods = getMethodsFor(RepositoryWithAllCrudMethodOverloaded.class);

		assertThat(methods.getSaveMethod().isAccessible()).isTrue();
		assertThat(methods.getDeleteMethod().isAccessible()).isTrue();
		assertThat(methods.getFindAllMethod().isAccessible()).isTrue();
		assertThat(methods.getFindOneMethod().isAccessible()).isTrue();
	}

	private static CrudMethods getMethodsFor(Class<?> repositoryInterface) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryInterface);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, PagingAndSortingRepository.class,
				null);

		return new DefaultCrudMethods(information);
	}

	private static void assertFindAllMethodOn(Class<?> type, Method method) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasFindAllMethod()).isEqualTo(method != null);
		assertThat(methods.getFindAllMethod()).isEqualTo(method);
	}

	private static void assertFindOneMethodOn(Class<?> type, Method method) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasFindOneMethod()).isEqualTo(method != null);
		assertThat(methods.getFindOneMethod()).isEqualTo(method);
	}

	private static void assertDeleteMethodOn(Class<?> type, Method method) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasDelete()).isEqualTo(method != null);
		assertThat(methods.getDeleteMethod()).isEqualTo(method);
	}

	private static void assertSaveMethodOn(Class<?> type, Method method) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasSaveMethod()).isEqualTo(method != null);
		assertThat(methods.getSaveMethod()).isEqualTo(method);
	}

	private static void assertSaveMethodPresent(Class<?> type, boolean present) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasSaveMethod()).isEqualTo(present);
		assertThat(methods.getSaveMethod()).matches(method -> present ? method != null : method == null);
	}

	interface Domain {}

	interface DomainCrudRepository extends CrudRepository<Domain, Long> {}

	interface DomainPagingAndSortingRepository extends PagingAndSortingRepository<Domain, Long> {}

	interface RepositoryWithCustomSave extends Repository<Domain, Serializable> {

		Domain save(Domain domain);
	}

	interface RepositoryWithCustomSortingAndPagingFindAll extends Repository<Domain, Serializable> {

		Iterable<Domain> findAll(Sort sort);

		Iterable<Domain> findAll(Pageable pageable);
	}

	/**
	 * @see DATACMNS-393
	 */
	interface RepositoryWithCustomSortingFindAll extends Repository<Domain, Serializable> {

		Iterable<Domain> findAll(Sort sort);
	}

	interface RepositoryWithInvalidPagingFindAll extends Repository<Domain, Serializable> {

		Iterable<Domain> findAll(Object pageable);
	}

	/**
	 * @see DATACMNS-393
	 */
	interface RepositoryWithInvalidPagingFallbackToSortFindAll extends Repository<Domain, Serializable> {

		Iterable<Domain> findAll(Object pageable);

		Iterable<Domain> findAll(Sort sort);
	}

	interface RepositoryWithIterableDeleteOnly extends Repository<Domain, Serializable> {

		void delete(Iterable<? extends Domain> entities);
	}

	/**
	 * @see DATACMNS-393
	 */
	interface RepositoryWithAllCrudMethodOverloaded extends CrudRepository<Domain, Long> {

		List<Domain> findAll();

		<S extends Domain> S save(S entity);

		void delete(Long id);

		Optional<Domain> findOne(Long id);
	}

	/**
	 * @see DATACMNS-393
	 */
	interface RepositoryWithAllCrudMethodOverloadedWrong extends CrudRepository<Domain, Long> {

		List<Domain> findAll(String s);

		Domain save(Serializable entity);

		void delete(String o);

		Domain findOne(Domain o);
	}

	/**
	 * @see DATACMNS-539
	 */
	interface RepositoryWithDeleteMethodForEntityOverloaded extends CrudRepository<Domain, Long> {

		void delete(Domain entity);
	}
}
