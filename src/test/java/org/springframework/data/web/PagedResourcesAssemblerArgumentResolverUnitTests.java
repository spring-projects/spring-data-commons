/*
 * Copyright 2014-2021 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Unit tests for {@link PagedResourcesAssemblerArgumentResolver}.
 *
 * @author Oliver Gierke
 * @since 1.7
 */
class PagedResourcesAssemblerArgumentResolverUnitTests {

	PagedResourcesAssemblerArgumentResolver resolver;

	@BeforeEach
	void setUp() {

		WebTestUtils.initWebTest();

		HateoasPageableHandlerMethodArgumentResolver hateoasPageableHandlerMethodArgumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		this.resolver = new PagedResourcesAssemblerArgumentResolver(hateoasPageableHandlerMethodArgumentResolver);
	}

	@Test // DATACMNS-418
	void createsPlainAssemblerWithoutContext() throws Exception {

		Method method = Controller.class.getMethod("noContext", PagedResourcesAssembler.class);
		Object result = resolver.resolveArgument(new MethodParameter(method, 0), null, null, null);

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
		Object result = resolver.resolveArgument(new MethodParameter(method, 0), null, null, null);

		assertThat(result).isNotNull();
	}

	private void assertSelectsParameter(Method method, int expectedIndex) {

		MethodParameter parameter = new MethodParameter(method, 0);

		Object result = resolver.resolveArgument(parameter, null, null, null);
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
				.isThrownBy(() -> resolver.resolveArgument(new MethodParameter(method, 0), null, null, null));
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
}
