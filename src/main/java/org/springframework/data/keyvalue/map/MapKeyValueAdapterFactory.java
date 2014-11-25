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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.10
 */
public class MapKeyValueAdapterFactory {

	@SuppressWarnings("rawtypes")//
	private static final Class<? extends Map> DEFAULT_MAP_TYPE = ConcurrentHashMap.class;

	@SuppressWarnings("rawtypes")//
	private Class<? extends Map> mapType;
	private Map<Serializable, Map<? extends Serializable, ?>> initialValues;

	/**
	 * Creates a new {@link MapKeyValueAdapterFactory}.
	 * 
	 * @see MapKeyValueAdapterFactory#MapKeyValueAdapterFactory(Class)
	 */
	public MapKeyValueAdapterFactory() {
		this(null);
	}

	/**
	 * Creates a new MKVAF with the given {@link Map} type to be used to hold the values in.
	 * 
	 * @param type any {@link Class} of type {@link Map}. Can be {@literal null} and will be defaulted to
	 *          {@link ConcurrentHashMap}.
	 */
	@SuppressWarnings("rawtypes")
	public MapKeyValueAdapterFactory(Class<? extends Map> type) {

		this.mapType = type;
		this.initialValues = new HashMap<Serializable, Map<? extends Serializable, ?>>();
	}

	/**
	 * Set values for a given {@literal keyspace} that to populate the adapter after creation.
	 * 
	 * @param keyspace must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public void setInitialValuesForKeyspace(Serializable keyspace, Map<? extends Serializable, ?> values) {

		Assert.notNull(keyspace, "KeySpace must not be null!");
		Assert.notNull(values, "Values must not be null!");

		initialValues.put(keyspace, values);
	}

	/**
	 * Configures the {@link Map} type to be used as backing store.
	 * 
	 * @param mapType must not be {@literal null}.
	 */
	@SuppressWarnings("rawtypes")
	public void setMapType(Class<? extends Map> mapType) {

		Assert.notNull(mapType, "May type must not be null!");

		this.mapType = mapType;
	}

	/**
	 * Creates and populates the adapter.
	 * 
	 * @return
	 */
	public MapKeyValueAdapter getAdapter() {

		MapKeyValueAdapter adapter = createAdapter();
		populateAdapter(adapter);

		return adapter;
	}

	private MapKeyValueAdapter createAdapter() {

		Class<?> type = this.mapType == null ? DEFAULT_MAP_TYPE : this.mapType;

		MapKeyValueAdapter adapter = new MapKeyValueAdapter(
				CollectionFactory.<Serializable, Map<Serializable, Object>> createMap(type, 100));
		return adapter;
	}

	private void populateAdapter(MapKeyValueAdapter adapter) {

		if (!initialValues.isEmpty()) {
			for (Entry<Serializable, Map<? extends Serializable, ?>> entry : initialValues.entrySet()) {
				for (Entry<? extends Serializable, ?> obj : entry.getValue().entrySet()) {
					adapter.put(obj.getKey(), obj.getValue(), entry.getKey());
				}
			}
		}
	}
}
