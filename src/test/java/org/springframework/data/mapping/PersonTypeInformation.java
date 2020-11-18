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

import org.springframework.data.mapping.model.ConfigurableTypeInformation;
import org.springframework.data.mapping.model.Field;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
public class PersonTypeInformation extends ConfigurableTypeInformation<Person> {

	private static PersonTypeInformation INSTANCE = new PersonTypeInformation();

	private PersonTypeInformation() {
		super(Person.class);

		// FIELDS
		addField(Field.<Person> intField("ssn").setter(Person::setSsn).getter(Person::getSsn));
		addField(Field.<Person> stringField("firstName").setter(Person::setFirstName).getter(Person::getFirstName));
		addField(Field.<Person> stringField("lastName").setter(Person::setLastName).getter(Person::getLastName));
	}

	public static PersonTypeInformation instance() {
		return INSTANCE;
	}
}
