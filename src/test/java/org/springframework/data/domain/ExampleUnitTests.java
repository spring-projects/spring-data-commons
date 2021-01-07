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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.ExampleMatcher.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Example}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Gierke
 */
class ExampleUnitTests {

	Person person;
	Example<Person> example;

	@BeforeEach
	void setUp() {

		person = new Person();
		person.firstname = "rand";

		example = Example.of(person);
	}

	@Test // DATACMNS-810
	void rejectsNullProbe() {
		assertThatIllegalArgumentException().isThrownBy(() -> Example.of(null));
	}

	@Test // DATACMNS-810
	void retunsSampleObjectsClassAsProbeType() {
		assertThat(example.getProbeType()).isEqualTo(Person.class);
	}

	@Test // DATACMNS-900
	void shouldCompareUsingHashCodeAndEquals() throws Exception {

		Example<Person> example = Example.of(person, matching().withIgnoreCase("firstname"));
		Example<Person> sameAsExample = Example.of(person, matching().withIgnoreCase("firstname"));

		Example<Person> different = Example.of(person,
				matching().withMatcher("firstname", GenericPropertyMatchers.contains()));

		assertThat(example.hashCode()).isEqualTo(sameAsExample.hashCode());
		assertThat(example.hashCode()).isNotEqualTo(different.hashCode());
		assertThat(example).isEqualTo(sameAsExample);
		assertThat(example).isNotEqualTo(different);
	}

	static class Person {
		String firstname;
	}
}
