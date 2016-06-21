/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.Test;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link SimpleTypeInformationMapper}.
 * 
 * @author Oliver Gierke
 */
public class SimpleTypeInformationMapperUnitTests {

	TypeInformationMapper mapper = new SimpleTypeInformationMapper();

	@Test
	public void resolvesTypeByLoadingClass() {

		Optional<TypeInformation<?>> type = mapper.resolveTypeFrom(Alias.of("java.lang.String"));

		TypeInformation<?> expected = ClassTypeInformation.from(String.class);

		assertThat(type).hasValue(expected);
	}

	@Test
	public void returnsNullForNonStringKey() {
		assertThat(mapper.resolveTypeFrom(Alias.of(new Object()))).isEmpty();
	}

	@Test
	public void returnsNullForEmptyTypeKey() {
		assertThat(mapper.resolveTypeFrom(Alias.of(""))).isEmpty();
	}

	@Test
	public void returnsNullForUnloadableClass() {

		assertThat(mapper.resolveTypeFrom(Alias.of("Foo"))).isEmpty();
	}

	@Test
	public void usesFullyQualifiedClassNameAsTypeKey() {

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(String.class)))
				.isEqualTo(Alias.of(String.class.getName()));
	}
}
