/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PropertyReferenceException}
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class PropertyReferenceExceptionUnitTests {

	static final TypeInformation<Sample> TYPE_INFO = ClassTypeInformation.from(Sample.class);
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

		PropertyReferenceException exception = new PropertyReferenceException("nme", TYPE_INFO, NO_PATHS);

		Collection<String> matches = exception.getPropertyMatches();

		assertThat(matches).containsExactly("name");
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
}
