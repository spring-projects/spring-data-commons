/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.UriComponents;

/**
 * Unit tests for {@link PagedResourcesAssemblerArgumentResolver}.
 *
 * @author Oliver Gierke
 * @author Réda Housni Alaoui
 * @since 1.7
 */
class PagedResourcesAssemblerArgumentResolverUnitTests {

	PagedResourcesAssemblerArgumentResolver resolver;

	@BeforeEach
	void setUp() {

		WebTestUtils.initWebTest();

		HateoasPageableHandlerMethodArgumentResolver hateoasPageableHandlerMethodArgumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		this.resolver = new PagedResourcesAssemblerArgumentResolver(hateoasPageableHandlerMethodArgumentResolver, null);
	}

	@Test // DATACMNS-418
	void createsPlainAssemblerWithoutContext() throws Exception {

		Method method = Controller.class.getMethod("noContext", PagedResourcesAssembler.class);
		Object result = resolver.resolveArgument(new MethodParameter(method, 0), null, TestUtils.getWebRequest(), null);

		assertThat(result).isInstanceOf(PagedResourcesAssembler.class);
		assertThat(result).isNotInstanceOf(MethodParameterAwarePagedResourcesAssembler.class);
	}

	@Test // DATACMNS-418
	void selectsUniquePageableParameter() throws Exception {

		Method method = Controller.class.getMethod("unique", PagedResourcesAssembler.class, Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test // DATACMNS-418
	void selectsUniquePageableParameterForQualifiedAssembler() throws Exception {

		Method method = Controller.class.getMethod("unnecessarilyQualified", PagedResourcesAssembler.class, Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test // DATACMNS-418
	void selectsUniqueQualifiedPageableParameter() throws Exception {

		Method method = Controller.class.getMethod("qualifiedUnique", PagedResourcesAssembler.class, Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test // DATACMNS-418
	void selectsQualifiedPageableParameter() throws Exception {

		Method method = Controller.class.getMethod("qualified", PagedResourcesAssembler.class, Pageable.class,
				Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test // DATACMNS-418
	void rejectsAmbiguousPageableParameters() throws Exception {
		assertRejectsAmbiguity("unqualifiedAmbiguity");
	}

	@Test // DATACMNS-418
	void rejectsAmbiguousPageableParametersForQualifiedAssembler() throws Exception {
		assertRejectsAmbiguity("assemblerQualifiedAmbiguity");
	}

	@Test // DATACMNS-418
	void rejectsAmbiguityWithoutMatchingQualifiers() throws Exception {
		assertRejectsAmbiguity("noMatchingQualifiers");
	}

	@Test // DATACMNS-419
	void doesNotFailForTemplatedMethodMapping() throws Exception {

		Method method = Controller.class.getMethod("methodWithPathVariable", PagedResourcesAssembler.class);
		Object result = resolver.resolveArgument(new MethodParameter(method, 0), null, TestUtils.getWebRequest(), null);

		assertThat(result).isNotNull();
	}

	@Test // DATACMNS-513
	void detectsMappingOfInvokedSubType() throws Exception {

		Method method = Controller.class.getMethod("methodWithMapping", PagedResourcesAssembler.class);

		// Simulate HandlerMethod.HandlerMethodParameter.getDeclaringClass()
		// as it's returning the invoked class as the declared one
		MethodParameter methodParameter = new MethodParameter(method, 0) {
			public java.lang.Class<?> getDeclaringClass() {
				return SubController.class;
			}
		};

		NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest("GET", "/foo/mapping"));
		Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

		assertThat(result).isInstanceOf(PagedResourcesAssembler.class);

		Optional<UriComponents> uriComponents = (Optional<UriComponents>) ReflectionTestUtils.getField(result, "baseUri");

		assertThat(uriComponents).hasValueSatisfying(it -> {
			assertThat(it.getPath()).isEqualTo("/foo/mapping");
		});
	}

	@Test // DATACMNS-1757
	void preservesQueryString() throws Exception {
		Method method = Controller.class.getMethod("methodWithMapping", PagedResourcesAssembler.class);
		NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest("GET", "/foo/mapping?bar=baz"));
		Object result = resolver.resolveArgument(new MethodParameter(method, 0), null, webRequest, null);

		assertThat(result).isNotNull();

		Optional<UriComponents> uriComponents = (Optional<UriComponents>) ReflectionTestUtils.getField(result, "baseUri");

		assertThat(uriComponents).hasValueSatisfying(it -> {
			assertThat(it.getPath()).isEqualTo("/foo/mapping?bar=baz");
		});
	}

	private void assertSelectsParameter(Method method, int expectedIndex) {

		MethodParameter parameter = new MethodParameter(method, 0);

		Object result = resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null);
		assertMethodParameterAwarePagedResourcesAssemblerFor(result, new MethodParameter(method, expectedIndex));
	}

	private static void assertMethodParameterAwarePagedResourcesAssemblerFor(Object result, MethodParameter parameter) {

		assertThat(result).isInstanceOf(MethodParameterAwarePagedResourcesAssembler.class);
		MethodParameterAwarePagedResourcesAssembler<?> assembler = (MethodParameterAwarePagedResourcesAssembler<?>) result;

		assertThat(assembler.getMethodParameter()).isEqualTo(parameter);
	}

	private void assertRejectsAmbiguity(String methodName) throws Exception {

		Method method = Controller.class.getMethod(methodName, PagedResourcesAssembler.class, Pageable.class,
				Pageable.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> resolver.resolveArgument(new MethodParameter(method, 0), null, TestUtils.getWebRequest(), null));
	}

	@RequestMapping("/")
	interface Controller {

		void noContext(PagedResourcesAssembler<Object> resolver);

		void unique(PagedResourcesAssembler<Object> assembler, Pageable pageable);

		void unnecessarilyQualified(@Qualifier("qualified") PagedResourcesAssembler<Object> assembler, Pageable pageable);

		void qualifiedUnique(@Qualifier("qualified") PagedResourcesAssembler<Object> assembler,
				@Qualifier("qualified") Pageable pageable);

		void qualified(@Qualifier("qualified") PagedResourcesAssembler<Object> resolver,
				@Qualifier("qualified") Pageable pageable, Pageable unqualified);

		void unqualifiedAmbiguity(PagedResourcesAssembler<Object> assembler, Pageable pageable, Pageable unqualified);

		void assemblerQualifiedAmbiguity(@Qualifier("qualified") PagedResourcesAssembler<Object> assembler,
				Pageable pageable, Pageable unqualified);

		void noMatchingQualifiers(@Qualifier("qualified") PagedResourcesAssembler<Object> assembler, Pageable pageable,
				@Qualifier("qualified2") Pageable unqualified);

		@RequestMapping("/{variable}/foo")
		void methodWithPathVariable(PagedResourcesAssembler<Object> assembler);

		@RequestMapping("/mapping")
		Object methodWithMapping(PagedResourcesAssembler<Object> pageable);
	}

	@RequestMapping("/foo")
	interface SubController extends Controller {

	}
}
