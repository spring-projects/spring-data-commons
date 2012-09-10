/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mapping.context;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.mapping.context.AbstractMappingContext.FieldMatch;

/**
 * Unit tests for {@link FieldMatch}. Introduced for DATACMNS-228.
 * 
 * @since 1.4
 * @author Oliver Gierke
 */
public class FieldMatchUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsBothParametersBeingNull() {

		new FieldMatch(null, null);
	}

	@Test
	public void matchesFieldByConcreteNameAndType() throws Exception {

		FieldMatch match = new FieldMatch("name", "java.lang.String");
		assertThat(match.matches(Sample.class.getField("this$0")), is(false));
		assertThat(match.matches(Sample.class.getField("this$1")), is(false));
		assertThat(match.matches(Sample.class.getField("name")), is(true));
	}

	@Test
	public void matchesFieldByNamePattern() throws Exception {

		FieldMatch match = new FieldMatch("this\\$.*", "java.lang.Object");
		assertThat(match.matches(Sample.class.getField("this$0")), is(true));
		assertThat(match.matches(Sample.class.getField("this$1")), is(true));
		assertThat(match.matches(Sample.class.getField("name")), is(false));
	}

	@Test
	public void matchesFieldByNameOnly() throws Exception {

		FieldMatch match = new FieldMatch("this\\$.*", null);
		assertThat(match.matches(Sample.class.getField("this$0")), is(true));
		assertThat(match.matches(Sample.class.getField("this$1")), is(true));
		assertThat(match.matches(Sample.class.getField("name")), is(false));
	}

	@Test
	public void matchesFieldByTypeNameOnly() throws Exception {

		FieldMatch match = new FieldMatch(null, "java.lang.Object");
		assertThat(match.matches(Sample.class.getField("this$0")), is(true));
		assertThat(match.matches(Sample.class.getField("this$1")), is(true));
		assertThat(match.matches(Sample.class.getField("name")), is(false));
	}

	static class Sample {

		public Object this$0;
		public Object this$1;
		public String name;
	}
}
