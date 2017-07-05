/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Unit tests for {@link ProjectingMethodInterceptor}.
 * 
 * @author Oliver Gierke
 * @author Saulo Medeiros de Araujo
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectingMethodInterceptorUnitTests {

	@Mock MethodInterceptor interceptor;
	@Mock MethodInvocation invocation;
	@Mock ProjectionFactory factory;
	ConversionService conversionService = new DefaultConversionService();

	@Test // DATAREST-221
	public void wrapsDelegateResultInProxyIfTypesDontMatch() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getHelper"));
		when(interceptor.invoke(invocation)).thenReturn("Foo");

		assertThat(methodInterceptor.invoke(invocation), is(instanceOf(Helper.class)));
	}

	@Test // DATAREST-221
	public void retunsDelegateResultAsIsIfTypesMatch() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor, conversionService);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getString"));
		when(interceptor.invoke(invocation)).thenReturn("Foo");

		assertThat(methodInterceptor.invoke(invocation), is((Object) "Foo"));
	}

	@Test // DATAREST-221
	public void returnsNullAsIs() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor, conversionService);

		when(interceptor.invoke(invocation)).thenReturn(null);

		assertThat(methodInterceptor.invoke(invocation), is(nullValue()));
	}

	@Test // DATAREST-221
	public void considersPrimitivesAsWrappers() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor, conversionService);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getPrimitive"));
		when(interceptor.invoke(invocation)).thenReturn(1L);

		assertThat(methodInterceptor.invoke(invocation), is((Object) 1L));
		verify(factory, times(0)).createProjection((Class<?>) anyObject(), anyObject());
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	public void appliesProjectionToNonEmptySets() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);
		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperCollection", Collections.singleton(mock(Helper.class))));

		assertThat(result, is(instanceOf(Set.class)));

		Set<Object> projections = (Set<Object>) result;
		assertThat(projections, hasSize(1));
		assertThat(projections, hasItem(instanceOf(HelperProjection.class)));
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	public void appliesProjectionToNonEmptyLists() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);
		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperList", Collections.singletonList(mock(Helper.class))));

		assertThat(result, is(instanceOf(List.class)));

		List<Object> projections = (List<Object>) result;

		assertThat(projections, hasSize(1));
		assertThat(projections, hasItem(instanceOf(HelperProjection.class)));
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	public void allowsMaskingAnArrayIntoACollection() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);
		Object result = methodInterceptor.invoke(mockInvocationOf("getHelperArray", new Helper[] { mock(Helper.class) }));

		assertThat(result, is(instanceOf(Collection.class)));

		Collection<Object> projections = (Collection<Object>) result;

		assertThat(projections, hasSize(1));
		assertThat(projections, hasItem(instanceOf(HelperProjection.class)));
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	public void appliesProjectionToNonEmptyMap() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperMap", Collections.singletonMap("foo", mock(Helper.class))));

		assertThat(result, is(instanceOf(Map.class)));

		Map<String, Object> projections = (Map<String, Object>) result;
		assertThat(projections.entrySet(), is(Matchers.<Entry<String, Object>> iterableWithSize(1)));
		assertThat(projections, hasEntry(is("foo"), instanceOf(HelperProjection.class)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void returnsSingleElementCollectionForTargetThatReturnsNonCollection() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		Helper reference = mock(Helper.class);
		Object result = methodInterceptor.invoke(mockInvocationOf("getHelperCollection", reference));

		assertThat(result, is((Matcher<Object>) instanceOf(Collection.class)));
		assertThat((Collection<?>) result, hasSize(1));
		assertThat((Collection<Object>) result, hasItem(instanceOf(HelperProjection.class)));
	}

	/**
	 * Mocks the {@link Helper} method of the given name to return the given value.
	 * 
	 * @param methodName
	 * @param returnValue
	 * @return
	 * @throws Throwable
	 */
	private MethodInvocation mockInvocationOf(String methodName, Object returnValue) throws Throwable {

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod(methodName));
		when(interceptor.invoke(invocation)).thenReturn(returnValue);

		return invocation;
	}

	interface Helper {

		Helper getHelper();

		String getString();

		long getPrimitive();

		Collection<HelperProjection> getHelperCollection();

		List<HelperProjection> getHelperList();

		Set<HelperProjection> getHelperSet();

		Map<String, HelperProjection> getHelperMap();

		Collection<HelperProjection> getHelperArray();
	}

	interface HelperProjection {
		Helper getHelper();

		String getString();
	}
}
