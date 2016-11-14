/*
 * Copyright 2014-2105 the original author or authors.
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
package org.springframework.data.projection;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link SpelEvaluatingMethodInterceptor}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(MockitoJUnitRunner.class)
public class SpelEvaluatingMethodInterceptorUnitTests {

	@Mock MethodInterceptor delegate;
	@Mock MethodInvocation invocation;

	SpelExpressionParser parser = new SpelExpressionParser();

	/**
	 * @see DATAREST-221, DATACMNS-630
	 */
	@Test
	public void invokesMethodOnTarget() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("propertyFromTarget"));

		MethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, new Target(), null, parser,
				Projection.class);

		assertThat(interceptor.invoke(invocation)).isEqualTo("property");
	}

	/**
	 * @see DATAREST-221, DATACMNS-630
	 */
	@Test
	public void invokesMethodOnBean() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("invokeBean"));

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton("someBean", new SomeBean());

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, new Target(), factory,
				parser, Projection.class);

		assertThat(interceptor.invoke(invocation)).isEqualTo("value");
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void forwardNonAtValueAnnotatedMethodToDelegate() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("getName"));

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, new Target(),
				new DefaultListableBeanFactory(), parser, Projection.class);

		interceptor.invoke(invocation);

		verify(delegate).invoke(invocation);
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsEmptySpelExpression() throws Throwable {

		when(invocation.getMethod()).thenReturn(InvalidProjection.class.getMethod("getAddress"));

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, new Target(),
				new DefaultListableBeanFactory(), parser, InvalidProjection.class);

		interceptor.invoke(invocation);
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void allowsMapAccessViaPropertyExpression() throws Throwable {

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", "Dave");

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("propertyFromTarget"));

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(delegate, map,
				new DefaultListableBeanFactory(), parser, Projection.class);

		assertThat(interceptor.invoke(invocation)).isEqualTo("Dave");
	}

	interface Projection {

		@Value("#{target.name}")
		String propertyFromTarget();

		@Value("#{@someBean.value}")
		String invokeBean();

		String getName();

		String getAddress();
	}

	interface InvalidProjection {

		@Value("")
		String getAddress();
	}

	static class Target {

		public String getName() {
			return "property";
		}
	}

	static class SomeBean {

		public String getValue() {
			return "value";
		}
	}
}
