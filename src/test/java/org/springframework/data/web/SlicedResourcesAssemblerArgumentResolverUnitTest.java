package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;

class SlicedResourcesAssemblerArgumentResolverUnitTest {
	SlicedResourcesAssemblerArgumentResolver resolver;

	private static void assertMethodParameterAwareSlicedResourcesAssemblerFor(Object result,
			MethodParameter parameter) {
		assertThat(result).isInstanceOf(MethodParameterAwareSlicedResourcesAssembler.class);

		var assembler = (MethodParameterAwareSlicedResourcesAssembler<?>) result;

		assertThat(assembler.getMethodParameter()).isEqualTo(parameter);
	}

	@BeforeEach
	void setUp() {
		WebTestUtils.initWebTest();

		var hateoasPageableHandlerMethodArgumentResolver = new HateoasPageableHandlerMethodArgumentResolver();
		this.resolver = new SlicedResourcesAssemblerArgumentResolver(hateoasPageableHandlerMethodArgumentResolver);
	}

	@Test
	void createsPlainAssemblerWithoutContext() throws Exception {
		var method = Controller.class.getMethod("noContext", SlicedResourcesAssembler.class);
		var result = resolver.resolveArgument(new MethodParameter(method, 0), null, null, null);

		assertThat(result).isInstanceOf(SlicedResourcesAssembler.class);
		assertThat(result).isNotInstanceOf(MethodParameterAwareSlicedResourcesAssembler.class);
	}

	@Test
	void selectsUniquePageableParameter() throws Exception {
		var method = Controller.class.getMethod("unique", SlicedResourcesAssembler.class, Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test
	void selectsUniquePageableParameterForQualifiedAssembler() throws Exception {
		var method = Controller.class.getMethod("unnecessarilyQualified", SlicedResourcesAssembler.class,
				Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test
	void selectsUniqueQualifiedPageableParameter() throws Exception {

		var method = Controller.class.getMethod("qualifiedUnique", SlicedResourcesAssembler.class, Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test
	void selectsQualifiedPageableParameter() throws Exception {
		var method = Controller.class.getMethod("qualified", SlicedResourcesAssembler.class, Pageable.class,
				Pageable.class);
		assertSelectsParameter(method, 1);
	}

	@Test
	void rejectsAmbiguousPageableParameters() throws Exception {
		assertRejectsAmbiguity("unqualifiedAmbiguity");
	}

	@Test
	void rejectsAmbiguousPageableParametersForQualifiedAssembler() throws Exception {
		assertRejectsAmbiguity("assemblerQualifiedAmbiguity");
	}

	@Test
	void rejectsAmbiguityWithoutMatchingQualifiers() throws Exception {
		assertRejectsAmbiguity("noMatchingQualifiers");
	}

	@Test
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
		var method = Controller.class.getMethod(methodName, SlicedResourcesAssembler.class, Pageable.class,
				Pageable.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> resolver.resolveArgument(new MethodParameter(method, 0), null, null, null));
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
