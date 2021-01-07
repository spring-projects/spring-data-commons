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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ExtensionIdAware;
import org.springframework.data.spel.spi.Function;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests {@link ExtensionAwareQueryMethodEvaluationContextProvider}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
class ExtensionAwareEvaluationContextProviderUnitTests {

	Method method;
	QueryMethodEvaluationContextProvider provider;

	@BeforeEach
	void setUp() throws Exception {

		this.method = SampleRepo.class.getMethod("findByFirstname", String.class);
		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(Collections.emptyList());
	}

	@Test // DATACMNS-533
	void usesPropertyDefinedByExtension() {

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(
				Collections.singletonList(new DummyExtension("_first", "first")));

		assertThat(evaluateExpression("key")).isEqualTo("first");
	}

	@Test // DATACMNS-533
	void secondExtensionOverridesFirstOne() {

		List<EvaluationContextExtension> extensions = new ArrayList<>();
		extensions.add(new DummyExtension("_first", "first"));
		extensions.add(new DummyExtension("_second", "second"));

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(extensions);

		assertThat(evaluateExpression("key")).isEqualTo("second");
	}

	@Test // DATACMNS-533
	void allowsDirectAccessToExtensionViaKey() {

		List<EvaluationContextExtension> extensions = new ArrayList<>();
		extensions.add(new DummyExtension("_first", "first"));
		extensions.add(new DummyExtension("_second", "second"));

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(extensions);
		assertThat(evaluateExpression("_first.key")).isEqualTo("first");
	}

	@Test // DATACMNS-533
	void exposesParametersAsVariables() {
		assertThat(evaluateExpression("#firstname")).isEqualTo("parameterValue");
	}

	@Test // DATACMNS-533
	void exposesMethodDefinedByExtension() {

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(
				Collections.singletonList(new DummyExtension("_first", "first")));

		assertThat(evaluateExpression("aliasedMethod()")).isEqualTo("methodResult");
		assertThat(evaluateExpression("extensionMethod()")).isEqualTo("methodResult");
		assertThat(evaluateExpression("_first.extensionMethod()")).isEqualTo("methodResult");
		assertThat(evaluateExpression("_first.aliasedMethod()")).isEqualTo("methodResult");
	}

	@Test // DATACMNS-533
	void exposesPropertiesDefinedByExtension() {

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(
				Collections.singletonList(new DummyExtension("_first", "first")));

		assertThat(evaluateExpression("DUMMY_KEY")).isEqualTo("dummy");
		assertThat(evaluateExpression("_first.DUMMY_KEY")).isEqualTo("dummy");
	}

	@Test // DATACMNS-533
	void exposesPageableParameter() throws Exception {

		this.method = SampleRepo.class.getMethod("findByFirstname", String.class, Pageable.class);
		PageRequest pageable = PageRequest.of(2, 3, Sort.by(Direction.DESC, "lastname"));

		assertThat(evaluateExpression("#pageable.offset", new Object[] { "test", pageable })).isEqualTo(6L);
		assertThat(evaluateExpression("#pageable.pageSize", new Object[] { "test", pageable })).isEqualTo(3);
		assertThat(evaluateExpression("#pageable.sort.toString()", new Object[] { "test", pageable }))
				.isEqualTo("lastname: DESC");
	}

	@Test // DATACMNS-533
	void exposesSortParameter() throws Exception {

		this.method = SampleRepo.class.getMethod("findByFirstname", String.class, Sort.class);
		Sort sort = Sort.by(Direction.DESC, "lastname");

		assertThat(evaluateExpression("#sort.toString()", new Object[] { "test", sort })).isEqualTo("lastname: DESC");
	}

	@Test // DATACMNS-533
	void exposesSpecialParameterEvenIfItsNull() throws Exception {

		this.method = SampleRepo.class.getMethod("findByFirstname", String.class, Sort.class);

		assertThat(evaluateExpression("#sort?.toString()", new Object[] { "test", null })).isNull();
	}

	@Test // DATACMNS-533
	void shouldBeAbleToAccessCustomRootObjectPropertiesAndFunctions() {

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(Collections.singletonList( //
				new DummyExtension("_first", "first") {
					@Override
					public CustomExtensionRootObject1 getRootObject() {
						return new CustomExtensionRootObject1();
					}
				}));

		assertThat(evaluateExpression("rootObjectInstanceField1")).isEqualTo("rootObjectInstanceF1");
		assertThat(evaluateExpression("rootObjectInstanceMethod1()")).isEqualTo(true);
		assertThat(evaluateExpression("getStringProperty()")).isEqualTo("stringProperty");
		assertThat(evaluateExpression("stringProperty")).isEqualTo("stringProperty");

		assertThat(evaluateExpression("_first.rootObjectInstanceField1")).isEqualTo("rootObjectInstanceF1");
		assertThat(evaluateExpression("_first.rootObjectInstanceMethod1()")).isEqualTo(true);
		assertThat(evaluateExpression("_first.getStringProperty()")).isEqualTo("stringProperty");
		assertThat(evaluateExpression("_first.stringProperty")).isEqualTo("stringProperty");
	}

	@Test // DATACMNS-533
	void shouldBeAbleToAccessCustomRootObjectPropertiesAndFunctionsInMultipleExtensions() {

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(Arrays.asList( //
				new DummyExtension("_first", "first") {
					@Override
					public CustomExtensionRootObject1 getRootObject() {
						return new CustomExtensionRootObject1();
					}
				}, //
				new DummyExtension("_second", "second") {
					@Override
					public CustomExtensionRootObject2 getRootObject() {
						return new CustomExtensionRootObject2();
					}
				}));

		assertThat(evaluateExpression("rootObjectInstanceField1")).isEqualTo("rootObjectInstanceF1");
		assertThat(evaluateExpression("rootObjectInstanceMethod1()")).isEqualTo(true);

		assertThat(evaluateExpression("rootObjectInstanceField2")).isEqualTo(42);
		assertThat(evaluateExpression("rootObjectInstanceMethod2()")).isEqualTo("rootObjectInstanceMethod2");

		assertThat(evaluateExpression("[0]")).isEqualTo("parameterValue");
	}

	@Test // DATACMNS-533
	void shouldBeAbleToAccessCustomRootObjectPropertiesAndFunctionsFromDynamicTargetSource() {

		final AtomicInteger counter = new AtomicInteger();

		this.provider = new ExtensionAwareQueryMethodEvaluationContextProvider(Collections.singletonList( //
				new DummyExtension("_first", "first") {

					@Override
					public CustomExtensionRootObject1 getRootObject() {
						counter.incrementAndGet();
						return new CustomExtensionRootObject1();
					}
				}) //
		);

		// inc counter / property access
		assertThat(evaluateExpression("rootObjectInstanceField1")).isEqualTo("rootObjectInstanceF1");

		// inc counter / function invocation
		assertThat(evaluateExpression("rootObjectInstanceMethod1()")).isEqualTo(true);

		assertThat(counter.get()).isEqualTo(2);
	}

	@Test // DATACMNS-1026
	void overloadedMethodsGetResolved() throws Exception {

		provider = createContextProviderWithOverloads();

		// from the root object
		assertThat(evaluateExpression("method()")).isEqualTo("zero");
		assertThat(evaluateExpression("method(23)")).isEqualTo("single-int");
		assertThat(evaluateExpression("method('hello')")).isEqualTo("single-string");
		assertThat(evaluateExpression("method('one', 'two')")).isEqualTo("two");

		// from the extension
		assertThat(evaluateExpression("method(1, 2)")).isEqualTo("two-ints");
		assertThat(evaluateExpression("method(1, 'two')")).isEqualTo("int-and-string");
	}

	@Test // DATACMNS-1026
	void methodFromRootObjectOverwritesMethodFromExtension() throws Exception {

		provider = createContextProviderWithOverloads();

		assertThat(evaluateExpression("ambiguous()")).isEqualTo("from-root");
	}

	@Test // DATACMNS-1026
	void aliasedMethodOverwritesMethodFromRootObject() throws Exception {

		provider = createContextProviderWithOverloads();

		assertThat(evaluateExpression("aliasedMethod()")).isEqualTo("methodResult");
	}

	@Test // DATACMNS-1026
	void exactMatchIsPreferred() throws Exception {

		provider = createContextProviderWithOverloads();

		assertThat(evaluateExpression("ambiguousOverloaded('aString')")).isEqualTo("string");
	}

	@Test // DATACMNS-1026
	void throwsExceptionWhenStillAmbiguous() throws Exception {

		provider = createContextProviderWithOverloads();

		assertThatIllegalStateException() //
				.isThrownBy(() -> evaluateExpression("ambiguousOverloaded(23)")) //
				.withMessageContaining("ambiguousOverloaded") //
				.withMessageContaining("(java.lang.Integer)");
	}

	@Test // DATACMNS-1518
	void invokesMethodWithVarArgs() {

		provider = createContextProviderWithOverloads();

		assertThat(evaluateExpression("methodWithVarArgs('one', 'two')")).isEqualTo("varargs");
	}

	@Test // DATACMNS-1534
	void contextProviderShouldLazilyLookUpExtensions() {

		ListableBeanFactory beanFactory = Mockito.mock(ListableBeanFactory.class);

		ExtensionAwareEvaluationContextProvider contextProvider = new ExtensionAwareEvaluationContextProvider(beanFactory);

		verify(beanFactory, never()).getBeansOfType(eq(EvaluationContextExtension.class), anyBoolean(), anyBoolean());

		contextProvider.getEvaluationContext(null);

		verify(beanFactory).getBeansOfType(eq(ExtensionIdAware.class), anyBoolean(), anyBoolean());
	}

	@Test // DATACMNS-1534
	void contextProviderShouldLookupExtensionsOnlyOnce() {

		ListableBeanFactory beanFactory = Mockito.mock(ListableBeanFactory.class);

		ExtensionAwareEvaluationContextProvider contextProvider = new ExtensionAwareEvaluationContextProvider(beanFactory);

		contextProvider.getEvaluationContext(null);
		contextProvider.getEvaluationContext(null);

		verify(beanFactory).getBeansOfType(eq(ExtensionIdAware.class), anyBoolean(), anyBoolean());
	}

	private static ExtensionAwareQueryMethodEvaluationContextProvider createContextProviderWithOverloads() {

		return new ExtensionAwareQueryMethodEvaluationContextProvider(Collections.singletonList( //
				new DummyExtension("_first", "first") {
					@Override
					public Object getRootObject() {
						return new RootWithOverloads();
					}
				}));
	}

	@RequiredArgsConstructor
	public static class DummyExtension implements org.springframework.data.spel.spi.EvaluationContextExtension {

		public static String DUMMY_KEY = "dummy";

		private final String key, value;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.spi.EvaluationContextExtension#getExtensionId()
		 */
		@Override
		public String getExtensionId() {
			return key;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.EvaluationContextExtensionAdapter#getProperties()
		 */
		@Override
		public Map<String, Object> getProperties() {

			Map<String, Object> properties = new HashMap<>();

			properties.put("key", value);

			return properties;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.spi.EvaluationContextExtensionSupport#getFunctions()
		 */
		@Override
		public Map<String, Function> getFunctions() {

			Map<String, Function> functions = new HashMap<>();

			try {
				functions.put("aliasedMethod", new Function(getClass().getMethod("extensionMethod")));
				return functions;
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}

		public static String extensionMethod() {
			return "methodResult";
		}

		public static String method(int i1, int i2) {
			return "two-ints";
		}

		public static String method(int i, String s) {
			return "int-and-string";
		}

		public static String ambiguous() {
			return "from-extension-type";
		}

		public static String ambiguousToo() {
			return "from-extension-type";
		}
	}

	private Object evaluateExpression(String expression) {
		return evaluateExpression(expression, new Object[] { "parameterValue" });
	}

	private Object evaluateExpression(String expression, Object[] args) {

		DefaultParameters parameters = new DefaultParameters(method);
		EvaluationContext evaluationContext = provider.getEvaluationContext(parameters, args);
		return new SpelExpressionParser().parseExpression(expression).getValue(evaluationContext);
	}

	interface SampleRepo {

		List<Object> findByFirstname(@Param("firstname") String firstname);

		List<Object> findByFirstname(@Param("firstname") String firstname, Pageable pageable);

		List<Object> findByFirstname(@Param("firstname") String firstname, Sort sort);
	}

	public static class CustomExtensionRootObject1 {

		public String rootObjectInstanceField1 = "rootObjectInstanceF1";

		public boolean rootObjectInstanceMethod1() {
			return true;
		}

		public String getStringProperty() {
			return "stringProperty";
		}
	}

	public static class CustomExtensionRootObject2 {

		public Integer rootObjectInstanceField2 = 42;

		public String rootObjectInstanceMethod2() {
			return "rootObjectInstanceMethod2";
		}
	}

	public static class RootWithOverloads {

		public String method() {
			return "zero";
		}

		public String method(String s) {
			return "single-string";
		}

		public String method(int i) {
			return "single-int";
		}

		public String method(String s1, String s2) {
			return "two";
		}

		public String ambiguous() {
			return "from-root";
		}

		public String aliasedMethod() {
			return "from-root";
		}

		public String ambiguousOverloaded(String s) {
			return "string";
		}

		public String ambiguousOverloaded(Object o) {
			return "object";
		}

		public String ambiguousOverloaded(Serializable o) {
			return "serializable";
		}

		public String methodWithVarArgs(String... args) {
			return "varargs";
		}
	}
}
