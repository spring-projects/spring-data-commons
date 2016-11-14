/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link PropertyReferenceException}
 *
 * @author Oliver Gierke
 */
public class PropertyReferenceExceptionUnitTests {

	static final TypeInformation<Sample> TYPE_INFO = ClassTypeInformation.from(Sample.class);
	static final List<PropertyPath> NO_PATHS = Collections.emptyList();

	public @Rule ExpectedException exception = ExpectedException.none();

	@Test
	public void rejectsNullPropertyName() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("name");

		new PropertyReferenceException(null, TYPE_INFO, NO_PATHS);
	}

	@Test
	public void rejectsNullType() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Type");

		new PropertyReferenceException("nme", null, NO_PATHS);
	}

	@Test
	public void rejectsNullPaths() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("paths");

		new PropertyReferenceException("nme", TYPE_INFO, null);
	}

	/**
	 * @see DATACMNS-801
	 */
	@Test
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
