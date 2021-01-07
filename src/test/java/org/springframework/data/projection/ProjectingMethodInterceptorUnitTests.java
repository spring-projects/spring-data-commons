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
package org.springframework.data.projection;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Unit tests for {@link ProjectingMethodInterceptor}.
 *
 * @author Oliver Gierke
 * @author Saulo Medeiros de Araujo
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ProjectingMethodInterceptorUnitTests {

	@Mock MethodInterceptor interceptor;
	@Mock MethodInvocation invocation;
	@Mock ProjectionFactory factory;
	ConversionService conversionService = new DefaultConversionService();

	@Test // DATAREST-221
	void wrapsDelegateResultInProxyIfTypesDontMatch() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getHelper"));
		when(interceptor.invoke(invocation)).thenReturn("Foo");

		assertThat(methodInterceptor.invoke(invocation)).isInstanceOf(Helper.class);
	}

	@Test // DATAREST-221
	void retunsDelegateResultAsIsIfTypesMatch() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor, conversionService);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getString"));
		when(interceptor.invoke(invocation)).thenReturn("Foo");

		assertThat(methodInterceptor.invoke(invocation)).isEqualTo("Foo");
	}

	@Test // DATAREST-221, DATACMNS-1762
	void returnsNullAsIs() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor, conversionService);
		mockInvocationOf("getString", null);

		assertThat(methodInterceptor.invoke(invocation)).isNull();
	}

	@Test // DATAREST-221
	void considersPrimitivesAsWrappers() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(factory, interceptor, conversionService);

		when(invocation.getMethod()).thenReturn(Helper.class.getMethod("getPrimitive"));
		when(interceptor.invoke(invocation)).thenReturn(1L);

		assertThat(methodInterceptor.invoke(invocation)).isEqualTo(1L);
		verify(factory, times(0)).createProjection((Class<?>) any(), any());
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	void appliesProjectionToNonEmptySets() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);
		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperCollection", Collections.singleton(mock(Helper.class))));

		assertThat(result).isInstanceOf(Set.class);

		Set<Object> projections = (Set<Object>) result;
		assertThat(projections).hasSize(1).hasOnlyElementsOfType(HelperProjection.class);
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	void appliesProjectionToNonEmptyLists() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);
		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperList", Collections.singletonList(mock(Helper.class))));

		assertThat(result).isInstanceOf(List.class);

		List<Object> projections = (List<Object>) result;

		assertThat(projections).hasSize(1).hasOnlyElementsOfType(HelperProjection.class);
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	void allowsMaskingAnArrayIntoACollection() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);
		Object result = methodInterceptor.invoke(mockInvocationOf("getHelperArray", new Helper[] { mock(Helper.class) }));

		assertThat(result).isInstanceOf(Collection.class);

		Collection<Object> projections = (Collection<Object>) result;

		assertThat(projections).hasSize(1).hasOnlyElementsOfType(HelperProjection.class);
	}

	@Test // DATAREST-394, DATAREST-408
	@SuppressWarnings("unchecked")
	void appliesProjectionToNonEmptyMap() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperMap", Collections.singletonMap("foo", mock(Helper.class))));

		assertThat(result).isInstanceOf(Map.class);

		Map<String, Object> projections = (Map<String, Object>) result;

		assertThat(projections).hasSize(1).matches(map -> map.get("foo") instanceof HelperProjection);
	}

	@Test
	void returnsSingleElementCollectionForTargetThatReturnsNonCollection() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		Helper reference = mock(Helper.class);
		Object result = methodInterceptor.invoke(mockInvocationOf("getHelperCollection", reference));

		assertThat(result).isInstanceOf(Collection.class);

		Collection<?> collection = (Collection<?>) result;

		assertThat(collection).hasSize(1).hasOnlyElementsOfType(HelperProjection.class);
	}

	@Test // DATACMNS-1598
	void returnsEnumSet() throws Throwable {

		MethodInterceptor methodInterceptor = new ProjectingMethodInterceptor(new ProxyProjectionFactory(), interceptor,
				conversionService);

		Object result = methodInterceptor
				.invoke(mockInvocationOf("getHelperEnumSet", Collections.singletonList(HelperEnum.Helpful)));

		assertThat(result).isInstanceOf(EnumSet.class);

		Collection<HelperEnum> collection = (Collection<HelperEnum>) result;
		assertThat(collection).containsOnly(HelperEnum.Helpful);
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

		EnumSet<HelperEnum> getHelperEnumSet();
	}

	interface HelperProjection {
		Helper getHelper();

		String getString();
	}

	enum HelperEnum {
		Helpful, NotSoMuch;
	}
}
