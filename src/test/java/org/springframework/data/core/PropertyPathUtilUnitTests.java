/*
 * Copyright 2025-present the original author or authors.
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

import java.io.Serializable;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.converter.Converter;

/**
 * Unit test {@link PropertyPathUtil}.
 *
 * @author Mark Paluch
 */
class PropertyPathUtilUnitTests {

	@Test
	void shouldResolvePropertyPath() {

		Converter<Person, String> c = convert(Person::getName);

		System.out.println(PropertyPathUtil.resolve(c));
	}

	static <T, P, C extends Converter<T, P> & Serializable> Serializable of(C mapping) {
		return mapping;
	}

	static <A, B, T extends Converter<A, B> & Serializable> T convert(T converter) {
		return converter;
	}

	static class Person {

		private String name;
		private @Nullable Integer age;

		// Getters
		public String getName() {
			return name;
		}

		public @Nullable Integer getAge() {
			return age;
		}

	}
}
