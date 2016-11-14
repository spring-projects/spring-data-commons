/*
 * Copyright 2012-2014 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.mapping.context.AbstractMappingContext.PersistentPropertyFilter.PropertyMatch;

/**
 * Unit tests for {@link PropertyMatch}. Introduced for DATACMNS-228.
 * 
 * @since 1.4
 * @author Oliver Gierke
 */
public class PropertyMatchUnitTests {

	@Test(expected = IllegalArgumentException.class)
	public void rejectsBothParametersBeingNull() {
		new PropertyMatch(null, null);
	}

	@Test
	public void matchesFieldByConcreteNameAndType() throws Exception {

		PropertyMatch match = new PropertyMatch("name", "java.lang.String");
		assertThat(match.matches("this$0", Object.class)).isFalse();
		assertThat(match.matches("this$1", Object.class)).isFalse();
		assertThat(match.matches("name", String.class)).isTrue();
	}

	@Test
	public void matchesFieldByNamePattern() throws Exception {

		PropertyMatch match = new PropertyMatch("this\\$.*", "java.lang.Object");
		assertThat(match.matches("this$0", Object.class)).isTrue();
		assertThat(match.matches("this$1", Object.class)).isTrue();
		assertThat(match.matches("name", String.class)).isFalse();
	}

	@Test
	public void matchesFieldByNameOnly() throws Exception {

		PropertyMatch match = new PropertyMatch("this\\$.*", null);
		assertThat(match.matches("this$0", Object.class)).isTrue();
		assertThat(match.matches("this$1", Object.class)).isTrue();
		assertThat(match.matches("name", String.class)).isFalse();
	}

	@Test
	public void matchesFieldByTypeNameOnly() throws Exception {

		PropertyMatch match = new PropertyMatch(null, "java.lang.Object");
		assertThat(match.matches("this$0", Object.class)).isTrue();
		assertThat(match.matches("this$1", Object.class)).isTrue();
		assertThat(match.matches("name", String.class)).isFalse();
	}

	static class Sample {

		public Object this$0;
		public Object this$1;
		public String name;
	}
}
