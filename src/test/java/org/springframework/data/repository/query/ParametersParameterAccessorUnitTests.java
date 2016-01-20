/*
 * Copyright 2011-2013 the original author or authors.
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
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link ParametersParameterAccessor}.
 * 
 * @author Oliver Gierke
 */
public class ParametersParameterAccessorUnitTests {

	Parameters<?, ?> parameters;

	@Before
	public void setUp() throws Exception {
		parameters = new DefaultParameters(Sample.class.getMethod("method", String.class, int.class));
	}

	@Test
	public void accessorIteratorHasNext() throws SecurityException, NoSuchMethodException {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { "Foo", 2 });

		Iterator<Object> iterator = accessor.iterator();
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is((Object) "Foo"));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is((Object) 2));
		assertThat(iterator.hasNext(), is(false));
	}

	/**
	 * @see DATACMNS-804
     */
	@Test
	public void accessorIteratorHasNextSkipsNonBindables() throws NoSuchMethodException {
		Method method = Sample.class.getMethod("method", Pageable.class, String.class, Sort.class, String.class);
		DefaultParameters parameters = new DefaultParameters(method);

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters,
				new Object[]{new PageRequest(0, 1), "Foo", new Sort("propertyA"), null});

		Iterator<Object> iterator = accessor.iterator();
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is((Object) "Foo"));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is((Object) null));
		assertThat(iterator.hasNext(), is(false));
	}

	@Test
	public void detectsNullValue() throws Exception {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { null, 5 });
		assertThat(accessor.hasBindableNullValue(), is(true));

		Method method = Sample.class.getMethod("method", Pageable.class, String.class);
		DefaultParameters parameters = new DefaultParameters(method);

		accessor = new ParametersParameterAccessor(parameters, new Object[] { null, "Foo" });
		assertThat(accessor.hasBindableNullValue(), is(false));
	}

	interface Sample {

		void method(String string, int integer);

		void method(Pageable pageable, String string);

		void method(Pageable pageable, String string, Sort sort, String otherString);
	}
}
