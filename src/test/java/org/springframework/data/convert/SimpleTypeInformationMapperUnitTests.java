/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.Alias;

/**
 * Unit tests for {@link SimpleTypeInformationMapper}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class SimpleTypeInformationMapperUnitTests {

	SimpleTypeInformationMapper mapper = new SimpleTypeInformationMapper();

	@Test
	void resolvesTypeByLoadingClass() {

		var type = mapper.resolveTypeFrom(Alias.of("java.lang.String"));

		TypeInformation<?> expected = TypeInformation.of(String.class);

		assertThat(type).isEqualTo(expected);
	}

	@Test // GH-2508
	void usesConfiguredClassloader() {

		mapper.setBeanClassLoader(HidingClassLoader.hide(SimpleTypeInformationMapperUnitTests.class));
		TypeInformation<?> type = mapper
				.resolveTypeFrom(Alias.of("org.springframework.data.convert.SimpleTypeInformationMapperUnitTests.User"));

		assertThat(type).isNull();
	}

	@Test
	void returnsNullForNonStringKey() {
		assertThat(mapper.resolveTypeFrom(Alias.of(new Object()))).isNull();
	}

	@Test
	void returnsNullForEmptyTypeKey() {
		assertThat(mapper.resolveTypeFrom(Alias.of(""))).isNull();
	}

	@Test
	void returnsNullForUnloadableClass() {

		assertThat(mapper.resolveTypeFrom(Alias.of("Foo"))).isNull();
	}

	@Test
	void usesFullyQualifiedClassNameAsTypeKey() {

		assertThat(mapper.createAliasFor(TypeInformation.of(String.class)))
				.isEqualTo(Alias.of(String.class.getName()));
	}

	static class User {

	}
}
