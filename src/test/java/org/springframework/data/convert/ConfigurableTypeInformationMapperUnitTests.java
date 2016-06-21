/*
 * Copyright 2011-2012 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.ClassTypeInformation;

/**
 * Unit tests for {@link ConfigurableTypeMapper}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurableTypeInformationMapperUnitTests<T extends PersistentProperty<T>> {

	ConfigurableTypeInformationMapper mapper;

	@Before
	public void setUp() {
		mapper = new ConfigurableTypeInformationMapper(Collections.singletonMap(String.class, "1"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTypeMap() {
		new ConfigurableTypeInformationMapper(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonBijectionalMap() {

		Map<Class<?>, String> map = new HashMap<Class<?>, String>();
		map.put(String.class, "1");
		map.put(Object.class, "1");

		new ConfigurableTypeInformationMapper(map);
	}

	@Test
	public void writesMapKeyForType() {

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(String.class))).isEqualTo(Alias.of("1"));
		assertThat(mapper.createAliasFor(ClassTypeInformation.from(Object.class))).isEqualTo(Alias.NONE);
	}

	@Test
	public void readsTypeForMapKey() {

		assertThat(mapper.resolveTypeFrom(Alias.of("1"))).hasValue(ClassTypeInformation.from(String.class));
		assertThat(mapper.resolveTypeFrom(Alias.of("unmapped"))).isEmpty();
	}
}
