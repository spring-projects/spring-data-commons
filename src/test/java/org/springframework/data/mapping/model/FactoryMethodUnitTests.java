/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link org.springframework.data.mapping.FactoryMethod}.
 *
 * @author Mark Paluch
 */
class FactoryMethodUnitTests {

	private static EntityInstantiators instantiators = new EntityInstantiators();

	@Test
	void shouldCreateInstanceThroughFactoryMethod() {

		var entity = new BasicPersistentEntity<>(TypeInformation.of(FactoryPerson.class));

		var result = instantiators.getInstantiatorFor(entity).createInstance(entity,
				new ParameterValueProvider() {

					@Override
					public Object getParameterValue(Parameter parameter) {

						if (parameter.getName().equals("firstname")) {
							return "Walter";
						}

						if (parameter.getName().equals("lastname")) {
							return "White";
						}
						return null;
					}
				});

		assertThat(result.firstname).isEqualTo("Walter");
		assertThat(result.lastname).isEqualTo("Mr. White");
	}

	static class FactoryPerson {

		private final String firstname, lastname;

		private FactoryPerson(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}

		@PersistenceCreator
		public static FactoryPerson of(String firstname, String lastname) {
			return new FactoryPerson(firstname, "Mr. " + lastname);
		}
	}
}
