/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.core;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Mark Paluch
 */
class MemberReferenceTests {

	@Test
	void shouldResolveMember() throws NoSuchMethodException {

		MemberReference<Person, String> reference = Person::name;

		assertThat(reference.getMember()).isEqualTo(Person.class.getMethod("name"));
	}

	@Test
	void retrofitConverter() throws NoSuchMethodException {

		Converter<Person, String> reference = convert(Person::name);

		assertThat(MemberReference.resolve(reference)).isEqualTo(Person.class.getMethod("name"));
	}

	static <A, B, T extends Converter<A, B> & Serializable> T convert(T converter) {
		return converter;
	}

	record Person(String name) {

	}
}
