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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.repository.support.RepositoryInvocationTestUtils.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.support.CrudRepositoryInvokerUnitTests.PersonRepository;
import org.springframework.data.repository.support.RepositoryInvocationTestUtils.*;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Integration tests for {@link ReflectionRepositoryInvoker}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class ReflectionRepositoryInvokerUnitTests {

	ConversionService conversionService;

	@BeforeEach
	void setUp() {
		this.conversionService = new DefaultFormattingConversionService();
	}

	@Test // DATACMNS-589
	void invokesSaveMethodCorrectly() throws Exception {

		ManualCrudRepository repository = mock(ManualCrudRepository.class);
		Method method = ManualCrudRepository.class.getMethod("save", Domain.class);

		when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

		getInvokerFor(repository, expectInvocationOf(method)).invokeSave(new Domain());
	}

	@Test // DATACMNS-589
	void invokesFindOneCorrectly() throws Exception {

		ManualCrudRepository repository = mock(ManualCrudRepository.class);
		Method method = ManualCrudRepository.class.getMethod("findById", Long.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindById("1");
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindById(1L);
	}

	@Test // DATACMNS-589
	void invokesDeleteWithDomainCorrectly() throws Exception {

		RepoWithDomainDeleteAndFindOne repository = mock(RepoWithDomainDeleteAndFindOne.class);
		when(repository.findById(1L)).thenReturn(new Domain());

		Method findOneMethod = RepoWithDomainDeleteAndFindOne.class.getMethod("findById", Long.class);
		Method deleteMethod = RepoWithDomainDeleteAndFindOne.class.getMethod("delete", Domain.class);

		getInvokerFor(repository, expectInvocationOf(findOneMethod, deleteMethod)).invokeDeleteById(1L);
	}

	@Test // DATACMNS-589
	void invokesFindAllWithoutParameterCorrectly() throws Exception {

		Method method = ManualCrudRepository.class.getMethod("findAll");
		ManualCrudRepository repository = mock(ManualCrudRepository.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Pageable.unpaged());
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(PageRequest.of(0, 10));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.unsorted());
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.by("foo"));
	}

	@Test // DATACMNS-589
	void invokesFindAllWithSortCorrectly() throws Exception {

		Method method = RepoWithFindAllWithSort.class.getMethod("findAll", Sort.class);
		RepoWithFindAllWithSort repository = mock(RepoWithFindAllWithSort.class);

		when(repository.findAll(any())).thenReturn(Page.empty());

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Pageable.unpaged());
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(PageRequest.of(0, 10));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.unsorted());
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.by("foo"));
	}

	@Test // DATACMNS-589
	void invokesFindAllWithPageableCorrectly() throws Exception {

		Method method = RepoWithFindAllWithPageable.class.getMethod("findAll", Pageable.class);
		RepoWithFindAllWithPageable repository = mock(RepoWithFindAllWithPageable.class);

		when(repository.findAll(any())).thenReturn(Page.empty());

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Pageable.unpaged());
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(PageRequest.of(0, 10));
	}

	@Test // DATACMNS-589
	void invokesQueryMethod() throws Exception {

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		parameters.add("firstName", "John");

		Method method = PersonRepository.class.getMethod("findByFirstName", String.class, Pageable.class);
		PersonRepository repository = mock(PersonRepository.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeQueryMethod(method, parameters, Pageable.unpaged(),
				Sort.unsorted());
	}

	@Test // DATACMNS-589
	void considersFormattingAnnotationsOnQueryMethodParameters() throws Exception {

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		parameters.add("date", "2013-07-18T10:49:00.000+02:00");

		Method method = PersonRepository.class.getMethod("findByCreatedUsingISO8601Date", Date.class, Pageable.class);
		PersonRepository repository = mock(PersonRepository.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeQueryMethod(method, parameters, Pageable.unpaged(),
				Sort.unsorted());
	}

	@Test // DATAREST-335, DATAREST-346, DATACMNS-589
	void invokesOverriddenDeleteMethodCorrectly() throws Exception {

		MyRepo repository = mock(MyRepo.class);
		Method method = CustomRepo.class.getMethod("deleteById", Long.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeDeleteById("1");
	}

	@Test // DATACMNS-589
	void rejectsInvocationOfMissingDeleteMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasDeleteMethod()).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> invoker.invokeDeleteById(1L));
	}

	@Test // DATACMNS-589
	void rejectsInvocationOfMissingFindOneMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasFindOneMethod()).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> invoker.invokeFindById(1L));
	}

	@Test // DATACMNS-589
	void rejectsInvocationOfMissingFindAllMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasFindAllMethod()).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> invoker.invokeFindAll(Sort.unsorted()));
	}

	@Test // DATACMNS-589
	void rejectsInvocationOfMissingSaveMethod() {

		RepositoryInvoker invoker = getInvokerFor(mock(EmptyRepository.class));

		assertThat(invoker.hasSaveMethod()).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> invoker.invokeSave(new Object()));
	}

	@Test // DATACMNS-647
	void translatesCollectionRequestParametersCorrectly() throws Exception {

		for (String[] ids : Arrays.asList(new String[] { "1,2" }, new String[] { "1", "2" })) {

			MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
			parameters.put("ids", Arrays.asList(ids));

			Method method = PersonRepository.class.getMethod("findByIdIn", Collection.class);
			PersonRepository repository = mock(PersonRepository.class);

			getInvokerFor(repository, expectInvocationOf(method)).invokeQueryMethod(method, parameters, Pageable.unpaged(),
					Sort.unsorted());
		}
	}

	@Test // DATACMNS-700
	void failedParameterConversionCapturesContext() throws Exception {

		RepositoryInvoker invoker = getInvokerFor(mock(SimpleRepository.class));

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
		parameters.add("value", "value");

		Method method = SimpleRepository.class.getMethod("findByClass", int.class);

		try {
			invoker.invokeQueryMethod(method, parameters, Pageable.unpaged(), Sort.unsorted());
		} catch (QueryMethodParameterConversionException o_O) {

			assertThat(o_O.getParameter()).isEqualTo(new MethodParameters(method).getParameters().get(0));
			assertThat(o_O.getSource()).isEqualTo("value");
			assertThat(o_O.getCause()).isInstanceOf(ConversionFailedException.class);
		}
	}

	@Test // DATACMNS-867
	void convertsWrapperTypeToJdkOptional() {

		GuavaRepository mock = mock(GuavaRepository.class);
		when(mock.findById(any())).thenReturn(com.google.common.base.Optional.of(new Domain()));

		RepositoryInvoker invoker = getInvokerFor(mock);

		Optional<Object> invokeFindOne = invoker.invokeFindById(1L);

		assertThat(invokeFindOne).isPresent();
	}

	@Test // DATACMNS-867
	void wrapsSingleElementCollectionIntoOptional() throws Exception {

		ManualCrudRepository mock = mock(ManualCrudRepository.class);
		when(mock.findAll()).thenReturn(Arrays.asList(new Domain()));

		Method method = ManualCrudRepository.class.getMethod("findAll");

		Optional<Object> result = getInvokerFor(mock).invokeQueryMethod(method, new LinkedMultiValueMap<>(),
				Pageable.unpaged(), Sort.unsorted());

		assertThat(result).hasValueSatisfying(it -> {
			assertThat(it).isInstanceOf(Collection.class);
		});
	}

	@Test // DATACMNS-1277
	void invokesFindByIdBeforeDeletingOnOverride() {

		DeleteByEntityOverrideSubRepository mock = mock(DeleteByEntityOverrideSubRepository.class);
		doReturn(Optional.of(new Domain())).when(mock).findById(any());

		getInvokerFor(mock).invokeDeleteById(1L);

		verify(mock).findById(1L);
		verify(mock).delete(any(Domain.class));
	}

	@Test // DATACMNS-1277
	void invokesDeleteByIdOnOverride() {

		DeleteByIdOverrideSubRepository mock = mock(DeleteByIdOverrideSubRepository.class);

		getInvokerFor(mock).invokeDeleteById(1L);

		verify(mock).deleteById(1L);
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
		void deleteById(Long id);
	}

	interface EmptyRepository extends Repository<Domain, Long> {}

	interface ManualCrudRepository extends Repository<Domain, Long> {

		Domain findById(Long id);

		Iterable<Domain> findAll();

		<T extends Domain> T save(T entity);

		void deleteById(Long id);
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

		Domain findById(Long id);

		void delete(Domain entity);
	}

	interface SimpleRepository extends Repository<Domain, Long> {

		Domain findByClass(@Param("value") int value);
	}

	interface GuavaRepository extends Repository<Domain, Long> {

		com.google.common.base.Optional<Domain> findById(Long id);
	}

	// DATACMNS-1277
	interface DeleteByEntityOverrideRepository<T, ID> extends CrudRepository<T, ID> {
		@Override
		void delete(T entity);
	}

	interface DeleteByEntityOverrideSubRepository extends DeleteByEntityOverrideRepository<Domain, Long> {}

	// DATACMNS-1277
	interface DeleteByIdOverrideRepository<T, ID> extends Repository<T, ID> {
		void deleteById(ID entity);
	}

	interface DeleteByIdOverrideSubRepository extends DeleteByIdOverrideRepository<Domain, Long> {}
}
