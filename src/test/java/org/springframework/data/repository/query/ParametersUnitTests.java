/*
 * Copyright 2008-2021 the original author or authors.
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

import rx.Single;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for {@link Parameters}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class ParametersUnitTests {

	private Method valid;

	@BeforeEach
	void setUp() throws SecurityException, NoSuchMethodException {

		valid = SampleDao.class.getMethod("valid", String.class);
	}

	@Test
	void checksValidMethodCorrectly() throws Exception {

		Method validWithPageable = SampleDao.class.getMethod("validWithPageable", String.class, Pageable.class);
		Method validWithSort = SampleDao.class.getMethod("validWithSort", String.class, Sort.class);

		new DefaultParameters(valid);
		new DefaultParameters(validWithPageable);
		new DefaultParameters(validWithSort);
	}

	@Test
	void rejectsInvalidMethodWithParamMissing() throws Exception {

		Method method = SampleDao.class.getMethod("invalidParamMissing", String.class, String.class);

		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultParameters(method));
	}

	@Test
	void rejectsNullMethod() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultParameters(null));
	}

	@Test
	void detectsNamedParameterCorrectly() throws Exception {

		Parameters<?, ?> parameters = getParametersFor("validWithSort", String.class, Sort.class);

		Parameter parameter = parameters.getParameter(0);

		assertThat(parameter.isNamedParameter()).isTrue();
		assertThat(parameter.getPlaceholder()).isEqualTo(":username");

		parameter = parameters.getParameter(1);

		assertThat(parameter.isNamedParameter()).isFalse();
		assertThat(parameter.isSpecialParameter()).isTrue();
	}

	@Test
	void calculatesPlaceholderPositionCorrectly() throws Exception {

		Method method = SampleDao.class.getMethod("validWithSortFirst", Sort.class, String.class);

		Parameters<?, ?> parameters = new DefaultParameters(method);
		assertThat(parameters.getBindableParameter(0).getIndex()).isEqualTo(1);

		method = SampleDao.class.getMethod("validWithSortInBetween", String.class, Sort.class, String.class);

		parameters = new DefaultParameters(method);

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

	@Test // DATACMNS-731
	void detectsExplicitlyNamedParameter() throws Exception {

		Parameter parameter = getParametersFor("valid", String.class).getBindableParameter(0);

		assertThat(parameter.getName()).isNotNull();
		assertThat(parameter.isExplicitlyNamed()).isTrue();
	}

	@Test // DATACMNS-731
	void doesNotConsiderParameterExplicitlyNamedEvenIfNamePresent() throws Exception {

		Parameter parameter = getParametersFor("validWithSortFirst", Sort.class, String.class).getBindableParameter(0);

		Object methodParameter = ReflectionTestUtils.getField(parameter, "parameter");
		ReflectionTestUtils.setField(methodParameter, "parameterName", "name");

		assertThat(parameter.getName()).isNotNull();
		assertThat(parameter.isExplicitlyNamed()).isFalse();
	}

	@Test // DATACMNS-89
	void detectsDynamicProjectionParameter() throws Exception {

		Parameters<?, Parameter> parameters = getParametersFor("dynamicBind", Class.class, Class.class, Class.class);

		assertThat(parameters.getParameter(0).isDynamicProjectionParameter()).isTrue();
		assertThat(parameters.getParameter(1).isDynamicProjectionParameter()).isFalse();
		assertThat(parameters.getParameter(2).isDynamicProjectionParameter()).isFalse();
	}

	@Test // DATACMNS-863
	void unwrapsOptionals() throws Exception {

		Parameters<?, Parameter> parameters = getParametersFor("methodWithOptional", Optional.class);

		assertThat(parameters.getParameter(0).getType()).isEqualTo(String.class);
	}

	@Test // DATACMNS-836
	void keepsReactiveStreamsWrapper() throws Exception {

		Parameters<?, Parameter> parameters = getParametersFor("methodWithPublisher", Publisher.class);

		assertThat(parameters.getParameter(0).getType()).isAssignableFrom(Publisher.class);
	}

	@Test // DATACMNS-836
	void keepsRxJavaWrapper() throws Exception {

		Parameters<?, Parameter> parameters = getParametersFor("methodWithSingle", Single.class);

		assertThat(parameters.getParameter(0).getType()).isAssignableFrom(Single.class);
	}

	@Test // DATACMNS-1383
	void acceptsCustomPageableParameter() throws Exception {

		Parameters<?, Parameter> parameters = getParametersFor("customPageable", SomePageable.class);

		assertThat(parameters.hasPageableParameter()).isTrue();
	}

	private Parameters<?, Parameter> getParametersFor(String methodName, Class<?>... parameterTypes)
			throws SecurityException, NoSuchMethodException {

		Method method = SampleDao.class.getMethod(methodName, parameterTypes);

		return new DefaultParameters(method);
	}

	static class User {

	}

	static interface SampleDao {

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

		Page<Object> customPageable(SomePageable pageable);
	}

	interface SomePageable extends Pageable {}
}
