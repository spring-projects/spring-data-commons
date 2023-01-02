/*
 * Copyright 2016-2023 the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PropertyReferenceException}
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author John Blum
 */
public class PropertyReferenceExceptionUnitTests {

	static final TypeInformation<Sample> TYPE_INFO = TypeInformation.of(Sample.class);
	static final List<PropertyPath> NO_PATHS = Collections.emptyList();

	@Test
	public void rejectsNullPropertyName() {

		assertThatIllegalArgumentException().isThrownBy(() -> new PropertyReferenceException(null, TYPE_INFO, NO_PATHS))
				.withMessageContaining("name");
	}

	@Test
	public void rejectsNullType() {

		assertThatIllegalArgumentException().isThrownBy(() -> new PropertyReferenceException("nme", null, NO_PATHS))
				.withMessageContaining("Type");
	}

	@Test
	public void rejectsNullPaths() {

		assertThatIllegalArgumentException().isThrownBy(() -> new PropertyReferenceException("nme", TYPE_INFO, null))
				.withMessageContaining("paths");
	}

	@Test // DATACMNS-801
	public void exposesPotentialMatch() {

		var exception = new PropertyReferenceException("nme", TYPE_INFO, NO_PATHS);

		var matches = exception.getPropertyMatches();

		assertThat(matches).containsExactly("name");
	}

	@Test // GH-2750
	public void formatsMessageWithTypeInfoAndHintsCorrectly() {

		var exception = new PropertyReferenceException("nme", TYPE_INFO, NO_PATHS);

		String expectedMessage = String.format("%s; %s", PropertyReferenceException.ERROR_TEMPLATE,
				PropertyReferenceException.HINTS_TEMPLATE);

		assertThat(exception)
				.hasMessage(expectedMessage,"nme", TYPE_INFO.getType().getSimpleName(), "'name'");
	}

	@Test // GH-2750
	public void formatsMessageWithTypeInfoHintsAndPathCorrectly() {

		var ctype = TypeInformation.of(C.class);

		var exception = new PropertyReferenceException("nme", TYPE_INFO,
				Collections.singletonList(PropertyPath.from("b.a", ctype)));

		String expectedMessage = String.format("%s; %s; %s", PropertyReferenceException.ERROR_TEMPLATE,
				PropertyReferenceException.HINTS_TEMPLATE, "Traversed path: C.b.a");

		assertThat(exception)
				.hasMessage(expectedMessage,"nme", TYPE_INFO.getType().getSimpleName(), "'name'");
	}

	static class Sample {

		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class A {

	}

	static class B {
		A a;
	}

	static class C {
		B b;
	}
}
