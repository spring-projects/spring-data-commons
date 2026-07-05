/*
 * Copyright 2008-present the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import io.reactivex.rxjava3.core.Single;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.Similarity;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for {@link Parameters}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class ParametersUnitTests {

	private Method valid;
	private RepositoryMetadata metadata;

	@BeforeEach
	void setUp() throws SecurityException, NoSuchMethodException {

		valid = SampleDao.class.getMethod("valid", String.class);
		metadata = new DefaultRepositoryMetadata(SampleDao.class);
	}

	@Test
	void checksValidMethodCorrectly() throws Exception {

		var validWithPageable = SampleDao.class.getMethod("validWithPageable", String.class, Pageable.class);
		var validWithSort = SampleDao.class.getMethod("validWithSort", String.class, Sort.class);

		new DefaultParameters(ParametersSource.of(valid));
		new DefaultParameters(ParametersSource.of(validWithPageable));
		new DefaultParameters(ParametersSource.of(validWithSort));
	}

	@Test
	void rejectsNullMethod() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultParameters((ParametersSource) null));
	}

	@Test
	void detectsNamedParameterCorrectly() throws Exception {

		Parameters<?, ?> parameters = getParametersFor("validWithSort", String.class, Sort.class);

		var parameter = parameters.getParameter(0);

		assertThat(parameter.isNamedParameter()).isTrue();
		assertThat(parameter.getPlaceholder()).isEqualTo(":username");

		parameter = parameters.getParameter(1);

		assertThat(parameter.isNamedParameter()).isFalse();
		assertThat(parameter.isSpecialParameter()).isTrue();
	}

	@Test
	void calculatesPlaceholderPositionCorrectly() throws Exception {

		var method = SampleDao.class.getMethod("validWithSortFirst", Sort.class, String.class);

		Parameters<?, ?> parameters = new DefaultParameters(ParametersSource.of(method));
		assertThat(parameters.getBindableParameter(0).getIndex()).isEqualTo(1);

		method = SampleDao.class.getMethod("validWithSortInBetween", String.class, Sort.class, String.class);

		parameters = new DefaultParameters(ParametersSource.of(method));

		assertThat(parameters.getBindableParameter(0).getIndex()).isEqualTo(0);
		assertThat(parameters.getBindableParameter(1).getIndex()).isEqualTo(2);
	}

	@Test
	void detectsEmptyParameterListCorrectly() throws Exception {

		Parameters<?, ?> parameters = getParametersFor("emptyParameters");
		assertThat(parameters.hasParameterAt(0)).isFalse();
	}

	@Test
	void detectsPageableParameter() throws Exception {
		Parameters<?, ?> parameters = getParametersFor("validWithPageable", String.class, Pageable.class);
		assertThat(parameters.getPageableIndex()).isEqualTo(1);
	}

	@Test
	void detectsSortParameter() throws Exception {
		Parameters<?, ?> parameters = getParametersFor("validWithSort", String.class, Sort.class);
		assertThat(parameters.getSortIndex()).isEqualTo(1);
	}

	@Test // DATACMNS-520
	void doesNotRejectParameterIfPageableComesFirst() throws Exception {
		getParametersFor("validWithPageableFirst", Pageable.class, String.class);
	}

	@Test // DATACMNS-731, GH-3124
	void detectsExplicitlyNamedParameter() throws Exception {

		var parameter = getParametersFor("valid", String.class).getBindableParameter(0);

		assertThat(parameter.getName()).isNotEmpty();
		assertThat(parameter.getRequiredName()).isNotNull();
		assertThat(parameter.isExplicitlyNamed()).isTrue();
	}

	@Test // DATACMNS-731
	void doesNotConsiderParameterExplicitlyNamedEvenIfNamePresent() throws Exception {

		var parameter = getParametersFor("validWithSortFirst", Sort.class, String.class).getBindableParameter(0);

		var methodParameter = ReflectionTestUtils.getField(parameter, "parameter");
		ReflectionTestUtils.setField(methodParameter, "parameterName", "name");

		assertThat(parameter.getName()).isNotEmpty();
		assertThat(parameter.isExplicitlyNamed()).isFalse();
	}

	@Test // DATACMNS-89
	void detectsDynamicProjectionParameter() throws Exception {

		var parameters = getParametersFor("dynamicBind", Class.class, Class.class, Class.class);

		assertThat(parameters.getParameter(0).isDynamicProjectionParameter()).isTrue();
		assertThat(parameters.getParameter(1).isDynamicProjectionParameter()).isFalse();
		assertThat(parameters.getParameter(2).isDynamicProjectionParameter()).isFalse();
	}

	@Test // GH-3020
	void detectsDynamicParametrizedProjectionParameter() throws Exception {

		var method = ParametrizedRepository.class.getMethod("dynamicBind", Class.class);
		var parameters = new DefaultParameters(
				ParametersSource.of(new DefaultRepositoryMetadata(ParametrizedRepository.class), method));

		assertThat(parameters.getParameter(0).isDynamicProjectionParameter()).isTrue();
	}

	@Test // DATACMNS-863
	void unwrapsOptionals() throws Exception {

		var parameters = getParametersFor("methodWithOptional", Optional.class);

		assertThat(parameters.getParameter(0).getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-836
	void keepsReactiveStreamsWrapper() throws Exception {

		var parameters = getParametersFor("methodWithPublisher", Publisher.class);

		assertThat(parameters.getParameter(0).getType()).isAssignableFrom(Publisher.class);
	}

	@Test // DATACMNS-836
	void keepsRxJavaWrapper() throws Exception {

		var parameters = getParametersFor("methodWithSingle", Single.class);

		assertThat(parameters.getParameter(0).getType()).isAssignableFrom(Single.class);
	}

	@Test // DATACMNS-1383
	void acceptsCustomPageableParameter() throws Exception {

		var parameters = getParametersFor("customPageable", SomePageable.class);

		assertThat(parameters.hasPageableParameter()).isTrue();
	}

	@Test // GH-2151
	void acceptsScrollPositionSubtypeParameter() throws Exception {

		var parameters = getParametersFor("customScrollPosition", OffsetScrollPosition.class);

		assertThat(parameters.hasScrollPositionParameter()).isTrue();
	}

	@Test // GH-2827
	void acceptsLimitParameter() throws Exception {

		var parameters = getParametersFor("withResultLimit", String.class, Limit.class);

		assertThat(parameters.hasLimitParameter()).isTrue();
		assertThat(parameters.getLimitIndex()).isOne();
	}

	@Test // GH-2995
	void considersGenericType() throws Exception {

		var method = TypedInterface.class.getMethod("foo", Object.class);

		var parameters = new DefaultParameters(
				ParametersSource.of(new DefaultRepositoryMetadata(TypedInterface.class), method));

		assertThat(parameters.getParameter(0).getType()).isEqualTo(Long.class);
	}

	@Test // GH-
	void considersScoreRange() throws Exception {

		var parameters = getParametersFor("methodWithScoreRange", Range.class);

		assertThat(parameters.hasScoreRangeParameter()).isTrue();
	}

	@Test // GH-
	void considersSimilarityRange() throws Exception {

		var parameters = getParametersFor("methodWithSimilarityRange", Range.class);

		assertThat(parameters.hasScoreRangeParameter()).isTrue();
	}

	private Parameters<?, Parameter> getParametersFor(String methodName, Class<?>... parameterTypes)
			throws SecurityException, NoSuchMethodException {

		var method = SampleDao.class.getMethod(methodName, parameterTypes);

		return new DefaultParameters(ParametersSource.of(metadata, method));
	}

	static class User {

	}

	interface SampleDao extends Repository<User, String> {

		User valid(@Param("username") String username);

		User invalidParamMissing(@Param("username") String username, String lastname);

		User validWithPageable(@Param("username") String username, Pageable pageable);

		User validWithPageableFirst(Pageable pageable, @Param("username") String username);

		User validWithSort(@Param("username") String username, Sort sort);

		User validWithSortFirst(Sort sort, String username);

		User validWithSortInBetween(String firstname, Sort sort, String lastname);

		User emptyParameters();

		<T> T dynamicBind(Class<T> type, Class<?> one, Class<Object> two);

		void methodWithOptional(Optional<String> optional);

		void methodWithPublisher(Publisher<String> publisher);

		void methodWithSingle(Single<String> single);

		void methodWithScoreRange(Range<Score> single);

		void methodWithSimilarityRange(Range<Similarity> single);

		Page<Object> customPageable(SomePageable pageable);

		Window<Object> customScrollPosition(OffsetScrollPosition request);

		List<User> withResultLimit(String criteria, Limit limit);
	}

	interface SomePageable extends Pageable {}

	interface Intermediate<T, ID> extends Repository<T, ID> {
		void foo(ID id);
	}

	interface TypedInterface extends Intermediate<User, Long> {}

	interface GenericRepository<T, ID> extends Repository<T, ID> {
		<P extends Projection<T>> Optional<P> dynamicBind(Class<P> type);
	}

	interface ParametrizedRepository extends GenericRepository<User, Long> {}

	interface Projection<T> {}

}
