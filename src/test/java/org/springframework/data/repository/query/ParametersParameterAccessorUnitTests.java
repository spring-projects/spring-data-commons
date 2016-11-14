/*
 * Copyright 2011-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("Foo");
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(2);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	public void detectsNullValue() throws Exception {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { null, 5 });
		assertThat(accessor.hasBindableNullValue()).isTrue();

		Method method = Sample.class.getMethod("method", Pageable.class, String.class);
		DefaultParameters parameters = new DefaultParameters(method);

		accessor = new ParametersParameterAccessor(parameters, new Object[] { null, "Foo" });
		assertThat(accessor.hasBindableNullValue()).isFalse();
	}

	/**
	 * @see DATACMNS-804
	 */
	@Test
	public void iteratesonlyOverBindableValues() throws Exception {

		Method method = Sample.class.getMethod("method", Pageable.class, String.class);
		DefaultParameters parameters = new DefaultParameters(method);

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters,
				new Object[] { new PageRequest(0, 10), "Foo" });

		assertThat(accessor).hasSize(1);
	}

	interface Sample {

		void method(String string, int integer);

		void method(Pageable pageable, String string);
	}
}
