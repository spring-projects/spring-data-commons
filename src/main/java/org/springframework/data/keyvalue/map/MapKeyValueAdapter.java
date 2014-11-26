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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.CollectionFactory;
import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link KeyValueAdapter} implementation for {@link Map}.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public class MapKeyValueAdapter extends AbstractKeyValueAdapter {

	private final Map<Serializable, Map<Serializable, Object>> data;

	@SuppressWarnings("rawtypes")//
	private final Class<? extends Map> mapType;

	/**
	 * Create new instance of {@link MapKeyValueAdapter} using {@link ConcurrentHashMap}.
	 */
	public MapKeyValueAdapter() {
		this(new ConcurrentHashMap<Serializable, Map<Serializable, Object>>());
	}

	/**
	 * Create new instance of {@link MapKeyValueAdapter} using given dataStore for persistence.
	 * 
	 * @param dataStore must not be {@literal null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MapKeyValueAdapter(Map<Serializable, Map<Serializable, Object>> dataStore) {

		Assert.notNull(dataStore, "Cannot initilalize adapter with 'null' datastore.");

		this.data = dataStore;
		this.mapType = (Class<? extends Map>) ClassUtils.getUserClass(dataStore);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#put(java.io.Serializable, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public Object put(Serializable id, Object item, Serializable keyspace) {

		Assert.notNull(id, "Cannot add item with 'null' id.");
		Assert.notNull(keyspace, "Cannot add item for 'null' collection.");

		return getKeySpaceMap(keyspace).put(id, item);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#contains(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public boolean contains(Serializable id, Serializable keyspace) {
		return get(id, keyspace) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#get(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object get(Serializable id, Serializable keyspace) {

		Assert.notNull(id, "Cannot get item with 'null' id.");
		return getKeySpaceMap(keyspace).get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#delete(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object delete(Serializable id, Serializable keyspace) {

		Assert.notNull(id, "Cannot delete item with 'null' id.");
		return getKeySpaceMap(keyspace).remove(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#getAllOf(java.io.Serializable)
	 */
	@Override
	public Collection<?> getAllOf(Serializable keyspace) {
		return getKeySpaceMap(keyspace).values();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#deleteAllOf(java.io.Serializable)
	 */
	@Override
	public void deleteAllOf(Serializable keyspace) {
		getKeySpaceMap(keyspace).clear();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#clear()
	 */
	@Override
	public void clear() {
		data.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		clear();
	}

	/**
	 * Get map associated with given keyspace.
	 * 
	 * @param keyspace must not be {@literal null}.
	 * @return
	 */
	protected Map<Serializable, Object> getKeySpaceMap(Serializable keyspace) {

		Assert.notNull(keyspace, "Collection must not be 'null' for lookup.");

		Map<Serializable, Object> map = data.get(keyspace);

		if (map != null) {
			return map;
		}

		addMapForKeySpace(keyspace);
		return data.get(keyspace);
	}

	private void addMapForKeySpace(Serializable keyspace) {
		data.put(keyspace, CollectionFactory.<Serializable, Object> createMap(mapType, 1000));
	}
}
