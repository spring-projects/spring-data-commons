/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.repository.query.spi.EvaluationContextExtension;
import org.springframework.data.repository.query.spi.EvaluationContextExtensionSupport;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests {@link ExtensionAwareEvaluationContextProvider}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont.
 */
public class ExtensibleEvaluationContextProviderUnitTests {

	DefaultParameters parameters;

	@Before
	public void setUp() throws Exception {

		Method method = SampleRepo.class.getMethod("findByFirstname", String.class);
		this.parameters = new DefaultParameters(method);
	}

	/**
	 * @see DATACMNS-533
	 */
	@Test
	public void usesPropertyDefinedByExtension() {

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>();
		extensions.add(new DummyExtension("_first", "first"));

		ExtensionAwareEvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(extensions);
		assertThat(evaluateExpression("key", provider), is((Object) "first"));
	}

	/**
	 * @see DATACMNS-533
	 */
	@Test
	public void secondExtensionOverridesFirstOne() {

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>();
		extensions.add(new DummyExtension("_first", "first"));
		extensions.add(new DummyExtension("_second", "second"));

		ExtensionAwareEvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(extensions);
		assertThat(evaluateExpression("key", provider), is((Object) "second"));
	}

	/**
	 * @see DATACMNS-533
	 */
	@Test
	public void allowsDirectAccessToExtensionViaKey() {

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>();
		extensions.add(new DummyExtension("_first", "first"));
		extensions.add(new DummyExtension("_second", "second"));

		EvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(extensions);
		assertThat(evaluateExpression("_first.key", provider), is((Object) "first"));
	}

	/**
	 * @see DATACMNS-533
	 */
	@Test
	public void exposesParametersAsVariables() {

		ExtensionAwareEvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(
				Collections.<EvaluationContextExtension> emptyList());

		assertThat(evaluateExpression("#firstname", provider), is((Object) "parameterValue"));
	}

	/**
	 * @see DATACMNS-533
	 */
	@Test
	public void exposesMethodDefinedByExtension() {

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>();
		extensions.add(new DummyExtension("_first", "first"));

		EvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(extensions);

		assertThat(evaluateExpression("aliasedMethod()", provider), is((Object) "methodResult"));
		assertThat(evaluateExpression("extensionMethod()", provider), is((Object) "methodResult"));
		assertThat(evaluateExpression("_first.extensionMethod()", provider), is((Object) "methodResult"));
		assertThat(evaluateExpression("_first.aliasedMethod()", provider), is((Object) "methodResult"));
	}

	/**
	 * @see DATACMNS-533
	 */
	@Test
	public void exposesPropertiesDefinedByExtension() {

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>();
		extensions.add(new DummyExtension("_first", "first"));

		EvaluationContextProvider provider = new ExtensionAwareEvaluationContextProvider(extensions);

		assertThat(evaluateExpression("DUMMY_KEY", provider), is((Object) "dummy"));
		assertThat(evaluateExpression("_first.DUMMY_KEY", provider), is((Object) "dummy"));
	}

	public static class DummyExtension extends EvaluationContextExtensionSupport {

		public static String DUMMY_KEY = "dummy";

		private final String key;
		private final String value;

		/**
		 * @param value
		 */
		public DummyExtension(String key, String value) {

			this.key = key;
			this.value = value;
		}

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

			Map<String, Object> properties = new HashMap<String, Object>(super.getProperties());

			properties.put("key", value);

			return properties;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.spi.EvaluationContextExtensionSupport#getFunctions()
		 */
		@Override
		public Map<String, Method> getFunctions() {

			Map<String, Method> functions = new HashMap<String, Method>(super.getFunctions());

			try {
				functions.put("aliasedMethod", getClass().getMethod("extensionMethod"));
				return functions;
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}

		public static String extensionMethod() {
			return "methodResult";
		}
	}

	private Object evaluateExpression(String expression, EvaluationContextProvider provider) {

		EvaluationContext evaluationContext = provider.getEvaluationContext(parameters, new Object[] { "parameterValue" });
		return new SpelExpressionParser().parseExpression(expression).getValue(evaluationContext);
	}

	interface SampleRepo {

		List<Object> findByFirstname(@Param("firstname") String firstname);
	}
}
