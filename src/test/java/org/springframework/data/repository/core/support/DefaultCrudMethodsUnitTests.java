/*
 * Copyright 2013 the original author or authors.
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
import java.lang.reflect.Method;

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
 */
public class DefaultCrudMethodsUnitTests {

	@Test
	public void detectsMethodsOnCrudRepository() throws Exception {

		Class<DomainCrudRepository> type = DomainCrudRepository.class;

		assertFindAllMethodOn(type, type.getMethod("findAll"));
		assertDeleteMethodOn(type, type.getMethod("delete", Serializable.class));
		assertSaveMethodOn(type, true);
	}

	@Test
	public void detectsMethodsOnPagingAndSortingRepository() throws Exception {

		Class<DomainPagingAndSortingRepository> type = DomainPagingAndSortingRepository.class;

		assertFindAllMethodOn(type, type.getMethod("findAll", Pageable.class));
		assertDeleteMethodOn(type, type.getMethod("delete", Serializable.class));
		assertSaveMethodOn(type, true);
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

	private static CrudMethods getMethodsFor(Class<?> repositoryInterface) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryInterface);
		RepositoryInformation information = new DefaultRepositoryInformation(metadata, PagingAndSortingRepository.class,
				null);

		return new DefaultCrudMethods(information);
	}

	private static void assertFindAllMethodOn(Class<?> type, Method method) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasFindAllMethod(), is(method != null));
		assertThat(methods.getFindAllMethod(), is(method));
	}

	private static void assertDeleteMethodOn(Class<?> type, Method method) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasDelete(), is(method != null));
		assertThat(methods.getDeleteMethod(), is(method));
	}

	private static void assertSaveMethodOn(Class<?> type, boolean present) {

		CrudMethods methods = getMethodsFor(type);

		assertThat(methods.hasSaveMethod(), is(present));
		assertThat(methods.getSaveMethod(), is(present ? notNullValue() : nullValue()));
	}

	interface Domain {}

	interface DomainCrudRepository extends CrudRepository<Domain, Long> {}

	interface DomainPagingAndSortingRepository extends PagingAndSortingRepository<Domain, Long> {}

	interface RepositoryWithCustomSortingAndPagingFindAll extends Repository<Domain, Serializable> {

		Iterable<Domain> findAll(Sort sort);

		Iterable<Domain> findAll(Pageable pageable);
	}

	interface RepositoryWithInvalidPagingFindAll extends Repository<Domain, Serializable> {

		Iterable<Domain> findAll(Object pageable);
	}

	interface RepositoryWithIterableDeleteOnly extends Repository<Domain, Serializable> {

		void delete(Iterable<? extends Domain> entities);
	}
}
