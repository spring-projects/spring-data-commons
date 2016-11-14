/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.repository.support.RepositoryInvocationTestUtils.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.support.CrudRepositoryInvokerUnitTests.Person;
import org.springframework.data.repository.support.CrudRepositoryInvokerUnitTests.PersonRepository;
import org.springframework.data.repository.support.RepositoryInvocationTestUtils.VerifyingMethodInterceptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Integration tests for {@link ReflectionRepositoryInvoker}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflectionRepositoryInvokerUnitTests {

	static final Page<Person> EMPTY_PAGE = new PageImpl<Person>(Collections.<Person> emptyList());

	ConversionService conversionService;

	@Before
	public void setUp() {
		this.conversionService = new DefaultFormattingConversionService();
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesSaveMethodCorrectly() throws Exception {

		ManualCrudRepository repository = mock(ManualCrudRepository.class);
		Method method = ManualCrudRepository.class.getMethod("save", Domain.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeSave(new Domain());
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesFindOneCorrectly() throws Exception {

		ManualCrudRepository repository = mock(ManualCrudRepository.class);
		Method method = ManualCrudRepository.class.getMethod("findOne", Long.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindOne("1");
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindOne(1L);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesDeleteWithDomainCorrectly() throws Exception {

		RepoWithDomainDeleteAndFindOne repository = mock(RepoWithDomainDeleteAndFindOne.class);
		when(repository.findOne(1L)).thenReturn(new Domain());

		Method findOneMethod = RepoWithDomainDeleteAndFindOne.class.getMethod("findOne", Long.class);
		Method deleteMethod = RepoWithDomainDeleteAndFindOne.class.getMethod("delete", Domain.class);

		getInvokerFor(repository, expectInvocationOf(findOneMethod, deleteMethod)).invokeDelete(1L);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesFindAllWithoutParameterCorrectly() throws Exception {

		Method method = ManualCrudRepository.class.getMethod("findAll");
		ManualCrudRepository repository = mock(ManualCrudRepository.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll((Pageable) null);
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(new PageRequest(0, 10));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll((Sort) null);
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(new Sort("foo"));
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesFindAllWithSortCorrectly() throws Exception {

		Method method = RepoWithFindAllWithSort.class.getMethod("findAll", Sort.class);
		RepoWithFindAllWithSort repository = mock(RepoWithFindAllWithSort.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll((Pageable) null);
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(new PageRequest(0, 10));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll((Sort) null);
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(new Sort("foo"));
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesFindAllWithPageableCorrectly() throws Exception {

		Method method = RepoWithFindAllWithPageable.class.getMethod("findAll", Pageable.class);
		RepoWithFindAllWithPageable repository = mock(RepoWithFindAllWithPageable.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll((Pageable) null);
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(new PageRequest(0, 10));
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesQueryMethod() throws Exception {

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
		parameters.add("firstName", "John");

		Method method = PersonRepository.class.getMethod("findByFirstName", String.class, Pageable.class);
		PersonRepository repository = mock(PersonRepository.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeQueryMethod(method, parameters, null, null);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void considersFormattingAnnotationsOnQueryMethodParameters() throws Exception {

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
		parameters.add("date", "2013-07-18T10:49:00.000+02:00");

		Method method = PersonRepository.class.getMethod("findByCreatedUsingISO8601Date", Date.class, Pageable.class);
		PersonRepository repository = mock(PersonRepository.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeQueryMethod(method, parameters, null, null);
	}

	/**
	 * @see DATAREST-335, DATAREST-346, DATACMNS-589
	 */
	@Test
	public void invokesOverriddenDeleteMethodCorrectly() throws Exception {

		MyRepo repository = mock(MyRepo.class);
		Method method = CustomRepo.class.getMethod("delete", Long.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeDelete("1");
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsInvocationOfMissingDeleteMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasDeleteMethod()).isFalse();
		invoker.invokeDelete(1L);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsInvocationOfMissingFindOneMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasFindOneMethod()).isFalse();
		invoker.invokeFindOne(1L);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsInvocationOfMissingFindAllMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasFindAllMethod()).isFalse();
		invoker.invokeFindAll((Pageable) null);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsInvocationOfMissingSaveMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasSaveMethod()).isFalse();
		invoker.invokeSave(new Object());
	}

	/**
	 * @see DATACMNS-647
	 */
	@Test
	public void translatesCollectionRequestParametersCorrectly() throws Exception {

		for (String[] ids : Arrays.asList(new String[] { "1,2" }, new String[] { "1", "2" })) {

			MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
			parameters.put("ids", Arrays.asList(ids));

			Method method = PersonRepository.class.getMethod("findByIdIn", Collection.class);
			PersonRepository repository = mock(PersonRepository.class);

			getInvokerFor(repository, expectInvocationOf(method)).invokeQueryMethod(method, parameters, null, null);
		}
	}

	/**
	 * @see DATACMNS-700
	 */
	@Test
	public void failedParameterConversionCapturesContext() throws Exception {

		RepositoryInvoker invoker = getInvokerFor(mock(SimpleRepository.class));

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.add("value", "value");

		Method method = SimpleRepository.class.getMethod("findByClass", int.class);

		try {
			invoker.invokeQueryMethod(method, parameters, null, null);
		} catch (QueryMethodParameterConversionException o_O) {

			assertThat(o_O.getParameter()).isEqualTo(new MethodParameters(method).getParameters().get(0));
			assertThat(o_O.getSource()).isEqualTo("value");
			assertThat(o_O.getCause()).isInstanceOf(ConversionFailedException.class);
		}
	}

	private static RepositoryInvoker getInvokerFor(Object repository) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repository.getClass().getInterfaces()[0]);
		GenericConversionService conversionService = new DefaultFormattingConversionService();

		return new ReflectionRepositoryInvoker(repository, metadata, conversionService);
	}

	private static RepositoryInvoker getInvokerFor(Object repository, VerifyingMethodInterceptor interceptor) {
		return getInvokerFor(getVerifyingRepositoryProxy(repository, interceptor));
	}

	interface MyRepo extends CustomRepo, CrudRepository<Domain, Long> {}

	class Domain {}

	interface CustomRepo {
		void delete(Long id);
	}

	interface EmptyRepository extends Repository<Domain, Long> {}

	interface ManualCrudRepository extends Repository<Domain, Long> {

		Domain findOne(Long id);

		Iterable<Domain> findAll();

		<T extends Domain> T save(T entity);

		void delete(Long id);
	}

	interface RepoWithFindAllWithoutParameters extends Repository<Domain, Long> {

		List<Domain> findAll();
	}

	interface RepoWithFindAllWithPageable extends Repository<Domain, Long> {

		Page<Domain> findAll(Pageable pageable);
	}

	interface RepoWithFindAllWithSort extends Repository<Domain, Long> {

		Page<Domain> findAll(Sort sort);
	}

	interface RepoWithDomainDeleteAndFindOne extends Repository<Domain, Long> {

		Domain findOne(Long id);

		void delete(Domain entity);
	}

	interface SimpleRepository extends Repository<Domain, Long> {

		Domain findByClass(@Param("value") int value);
	}
}
