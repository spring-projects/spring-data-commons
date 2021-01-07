/*
 * Copyright 2014-2021 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ParsingUtils}.
 *
 * @author Oliver Gierke
 */
public class ParsingUtilsUnitTests {

	@Test // DATCMNS-486
	public void splitsCamelCaseWithAllSortsOfCharacters() {

		String sample = "prefix" + "이름" //
				+ "Anders" //
				+ "Øre" //
				+ "År" //
				+ "Property1" //
				+ "생일" //
				+ "Foo_bar" //
				+ "FOO_BAR" //
				+ "Bar$foo" //
				+ "BAR$FOO" //
				+ "Suffix";

		List<String> result = ParsingUtils.splitCamelCaseToLower(sample);
		assertThat(result).contains("prefix", "이름", "anders", "øre", "år", "property1", "생일", "foo_bar", "foo_bar",
				"bar$foo", "bar$foo", "suffix");
	}

	@Test // DATCMNS-486
	public void reconcatenatesCamelCaseString() {
		assertThat(ParsingUtils.reconcatenateCamelCase("myCamelCase", "-")).isEqualTo("my-camel-case");
	}
}
