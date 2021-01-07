/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link ConfigurableTypeInformationMapper}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ConfigurableTypeInformationMapperUnitTests<T extends PersistentProperty<T>> {

	ConfigurableTypeInformationMapper mapper;

	@BeforeEach
	void setUp() {
		mapper = new ConfigurableTypeInformationMapper(Collections.singletonMap(String.class, "1"));
	}

	@Test
	void rejectsNullTypeMap() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurableTypeInformationMapper(null));
	}

	@Test
	void rejectsNonBijectionalMap() {

		Map<Class<?>, String> map = new HashMap<>();
		map.put(String.class, "1");
		map.put(Object.class, "1");

		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurableTypeInformationMapper(map));
	}

	@Test
	void writesMapKeyForType() {

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(String.class))).isEqualTo(Alias.of("1"));
		assertThat(mapper.createAliasFor(ClassTypeInformation.from(Object.class))).isEqualTo(Alias.NONE);
	}

	@Test
	void readsTypeForMapKey() {

		assertThat(mapper.resolveTypeFrom(Alias.of("1"))).isEqualTo(ClassTypeInformation.from(String.class));
		assertThat(mapper.resolveTypeFrom(Alias.of("unmapped"))).isNull();
	}
}
