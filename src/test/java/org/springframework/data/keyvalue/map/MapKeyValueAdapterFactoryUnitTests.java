/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.keyvalue.map;

import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.Test;
import org.springframework.core.CollectionFactory;

/**
 * @author Christoph Strobl
 */
public class MapKeyValueAdapterFactoryUnitTests {

	@Test
	public void shouldDefaultToConcurrentHashMapWhenTypeIsNull() {

		assertThat(new MapKeyValueAdapterFactory(null).getAdapter().getKeySpaceMap("foo"),
				instanceOf(ConcurrentHashMap.class));
	}

	@Test
	public void shouldDefaultToCollecitonUtilsDefaultForInterfaceTypes() {

		assertThat(new MapKeyValueAdapterFactory(Map.class).getAdapter().getKeySpaceMap("foo"),
				instanceOf(CollectionFactory.createMap(Map.class, 0).getClass()));
	}

	@Test
	public void shouldUseConcreteMapTypeWhenInstantiable() {

		assertThat(new MapKeyValueAdapterFactory(ConcurrentSkipListMap.class).getAdapter().getKeySpaceMap("foo"),
				instanceOf(CollectionFactory.createMap(ConcurrentSkipListMap.class, 0).getClass()));
	}

	@Test
	public void shouldPopulateAdapterWithValues() {

		MapKeyValueAdapterFactory factory = new MapKeyValueAdapterFactory();
		factory.setInitialValuesForKeyspace("foo", Collections.singletonMap("1", "STANIS"));
		factory.setInitialValuesForKeyspace("bar", Collections.singletonMap("1", "ROBERT"));

		assertThat((String) factory.getAdapter().get("1", "foo"), equalTo("STANIS"));
		assertThat((String) factory.getAdapter().get("1", "bar"), equalTo("ROBERT"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenSettingValuesForNullKeySpace() {
		new MapKeyValueAdapterFactory().setInitialValuesForKeyspace(null, Collections.<Serializable, Object> emptyMap());
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenSettingNullValuesForKeySpace() {
		new MapKeyValueAdapterFactory().setInitialValuesForKeyspace("foo", null);
	}

}
