/*
 * Copyright 2014-2016 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.NotWritablePropertyException;

/**
 * Unit tests for {@link PropertyAccessingMethodInterceptor}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertyAccessingMethodInterceptorUnitTests {

	@Mock MethodInvocation invocation;

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void triggersPropertyAccessOnTarget() throws Throwable {

		Source source = new Source();
		source.firstname = "Dave";

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("getFirstname"));
		MethodInterceptor interceptor = new PropertyAccessingMethodInterceptor(source);

		assertThat(interceptor.invoke(invocation)).isEqualTo("Dave");
	}

	/**
	 * @see DATAREST-221
	 */
	@Test(expected = NotReadablePropertyException.class)
	public void throwsAppropriateExceptionIfThePropertyCannotBeFound() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("getLastname"));
		new PropertyAccessingMethodInterceptor(new Source()).invoke(invocation);
	}

	/**
	 * @see DATAREST-221
	 */
	@Test
	public void forwardsObjectMethodInvocation() throws Throwable {

		when(invocation.getMethod()).thenReturn(Object.class.getMethod("toString"));

		new PropertyAccessingMethodInterceptor(new Source()).invoke(invocation);
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsNonAccessorMethod() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("someGarbage"));

		new PropertyAccessingMethodInterceptor(new Source()).invoke(invocation);
	}

	/**
	 * @see DATACMNS-820
	 */
	@Test
	public void triggersWritePropertyAccessOnTarget() throws Throwable {

		Source source = new Source();
		source.firstname = "Dave";

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("setFirstname", String.class));
		when(invocation.getArguments()).thenReturn(new Object[] { "Carl" });

		new PropertyAccessingMethodInterceptor(source).invoke(invocation);

		assertThat(source.firstname).isEqualTo("Carl");
	}

	/**
	 * @see DATACMNS-820
	 */
	@Test(expected = IllegalStateException.class)
	public void throwsAppropriateExceptionIfTheInvocationHasNoArguments() throws Throwable {

		Source source = new Source();

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("setFirstname", String.class));
		when(invocation.getArguments()).thenReturn(new Object[0]);

		new PropertyAccessingMethodInterceptor(source).invoke(invocation);
	}

	/**
	 * @see DATACMNS-820
	 */
	@Test(expected = NotWritablePropertyException.class)
	public void throwsAppropriateExceptionIfThePropertyCannotWritten() throws Throwable {

		when(invocation.getMethod()).thenReturn(Projection.class.getMethod("setGarbage", String.class));
		when(invocation.getArguments()).thenReturn(new Object[] { "Carl" });

		new PropertyAccessingMethodInterceptor(new Source()).invoke(invocation);
	}

	static class Source {

		String firstname;
	}

	interface Projection {

		String getFirstname();

		void setFirstname(String firstname);

		void setGarbage(String garbage);

		String getLastname();

		String someGarbage();
	}
}
