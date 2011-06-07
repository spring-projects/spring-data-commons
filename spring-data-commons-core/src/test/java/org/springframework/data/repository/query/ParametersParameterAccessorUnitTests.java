/*
 * Copyright 2011 the original author or authors.
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

import java.util.Iterator;

import org.junit.Test;

/**
 * Unit tests for {@link ParametersParameterAccessor}.
 *
 * @author Oliver Gierke
 */
public class ParametersParameterAccessorUnitTests {

	
	@Test
	public void accessorIteratorHasNext() throws SecurityException, NoSuchMethodException {
		
		Parameters parameters = new Parameters(Sample.class.getMethod("method", String.class, int.class));
		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { "Foo", 2});
		
		Iterator<Object> iterator = accessor.iterator();
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is((Object) "Foo"));
		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), is((Object) 2));
		assertThat(iterator.hasNext(), is(false));
	}
	
	
	interface Sample {
		
		void method(String string, int integer);
	}
}
