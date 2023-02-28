/*
 * Copyright 2022-2023 the original author or authors.
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Unit tests for {@link SlicedResourcesAssemblerArgumentResolver}.
 *
 * @author Michael Schout
 * @author Oliver Drotbohm
 * @since 3.1
 */
class SlicedResourcesAssemblerArgumentResolverUnitTest {

	SlicedResourcesAssemblerArgumentResolver resolver;

	@BeforeEach
	void setUp() {

		WebTestUtils.initWebTest();

		var hateoasPageableHandlerMethodArgumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		this.resolver = new SlicedResourcesAssemblerArgumentResolver(hateoasPageableHandlerMethodArgumentResolver);
	}

	@Test // GH-1307
	void createsPlainAssemblerWithoutContext() throws Exception {

		var method = Controller.class.getMethod("noContext", SlicedResourcesAssembler.class);
		var result = resolver.resolveArgument(new MethodParameter(method, 0), null, null, null);

		assertThat(result).isInstanceOf(SlicedResourcesAssembler.class);
	}

	@Test // GH-1307
	void selectsUniquePageableParameter() throws Exception {

		var method = Controller.class.getMethod("unique", SlicedResourcesAssembler.class, Pageable.class);

		assertSelectsParameter(method, 1);
	}

	@Test // GH-1307
	void selectsUniquePageableParameterForQualifiedAssembler() throws Exception {

		var method = Controller.class.getMethod("unnecessarilyQualified", SlicedResourcesAssembler.class,
				Pageable.class);

		assertSelectsParameter(method, 1);
	}

	@Test // GH-1307
	void selectsUniqueQualifiedPageableParameter() throws Exception {

		var method = Controller.class.getMethod("qualifiedUnique", SlicedResourcesAssembler.class, Pageable.class);

		assertSelectsParameter(method, 1);
	}

	@Test // GH-1307
	void selectsQualifiedPageableParameter() throws Exception {

		var method = Controller.class.getMethod("qualified", SlicedResourcesAssembler.class, Pageable.class,
				Pageable.class);

		assertSelectsParameter(method, 1);
	}

	@Test // GH-1307
	void rejectsAmbiguousPageableParameters() throws Exception {
		assertRejectsAmbiguity("unqualifiedAmbiguity");
	}

	@Test // GH-1307
	void rejectsAmbiguousPageableParametersForQualifiedAssembler() throws Exception {
		assertRejectsAmbiguity("assemblerQualifiedAmbiguity");
	}

	@Test // GH-1307
	void rejectsAmbiguityWithoutMatchingQualifiers() throws Exception {
		assertRejectsAmbiguity("noMatchingQualifiers");
	}

	@Test // GH-1307
	void doesNotFailForTemplatedMethodMapping() throws Exception {

		var method = Controller.class.getMethod("methodWithPathVariable", SlicedResourcesAssembler.class);
		var result = resolver.resolveArgument(new MethodParameter(method, 0), null, null, null);

		assertThat(result).isNotNull();
	}

	private void assertSelectsParameter(Method method, int expectedIndex) {

		var parameter = new MethodParameter(method, 0);

		var result = resolver.resolveArgument(parameter, null, null, null);
		assertMethodParameterAwareSlicedResourcesAssemblerFor(result, new MethodParameter(method, expectedIndex));
	}

	private void assertRejectsAmbiguity(String methodName) throws Exception {

		var method = Controller.class.getMethod(methodName, SlicedResourcesAssembler.class, Pageable.class, Pageable.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> resolver.resolveArgument(new MethodParameter(method, 0), null, null, null));
	}

	private static void assertMethodParameterAwareSlicedResourcesAssemblerFor(Object result, MethodParameter parameter) {

		assertThat(result).isInstanceOfSatisfying(SlicedResourcesAssembler.class, it -> {
			assertThat(ReflectionTestUtils.getField(it, "parameter")).isEqualTo(parameter);
		});
	}

	@RequestMapping("/")
	interface Controller {
		void noContext(SlicedResourcesAssembler<Object> resolver);

		void unique(SlicedResourcesAssembler<Object> assembler, Pageable pageable);

		void unnecessarilyQualified(@Qualifier("qualified") SlicedResourcesAssembler<Object> assembler,
				Pageable pageable);

		void qualifiedUnique(@Qualifier("qualified") SlicedResourcesAssembler<Object> assembler,
				@Qualifier("qualified") Pageable pageable);

		void qualified(@Qualifier("qualified") SlicedResourcesAssembler<Object> resolver,
				@Qualifier("qualified") Pageable pageable, Pageable unqualified);

		void unqualifiedAmbiguity(SlicedResourcesAssembler<Object> assembler, Pageable pageable, Pageable unqualified);

		void assemblerQualifiedAmbiguity(@Qualifier("qualified") SlicedResourcesAssembler<Object> assembler,
				Pageable pageable, Pageable unqualified);

		void noMatchingQualifiers(@Qualifier("qualified") SlicedResourcesAssembler<Object> assembler, Pageable pageable,
				@Qualifier("qualified2") Pageable unqualified);

		@RequestMapping("/{variable}/foo")
		void methodWithPathVariable(SlicedResourcesAssembler<Object> assembler);

		@RequestMapping("/mapping")
		Object methodWithMapping(SlicedResourcesAssembler<Object> pageable);
	}
}
