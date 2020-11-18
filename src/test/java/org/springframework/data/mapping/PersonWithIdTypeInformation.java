/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.data.mapping.model.ConfigurableTypeConstructor;
import org.springframework.data.mapping.model.ConfigurableTypeInformation;
import org.springframework.data.mapping.model.Field;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
public class PersonWithIdTypeInformation extends ConfigurableTypeInformation<PersonWithId> {

	private static final PersonWithIdTypeInformation INSTANCE = new PersonWithIdTypeInformation();

	private PersonWithIdTypeInformation() {
		super(PersonWithId.class, PersonTypeInformation.instance());

		// CONSTRUCTOR
		setConstructor(ConfigurableTypeConstructor.<PersonWithId> builder().args("ssn", "firstName", "lastName")
				.newInstanceFunction((args) -> new PersonWithId((Integer) args[0], (String) args[1], (String) args[2])));

		// FIELDS
		addField(Field.<PersonWithId> stringField("id").annotatedWithAtId().getter(PersonWithId::getId));
	}

	public static PersonWithIdTypeInformation instance() {
		return INSTANCE;
	}
}
