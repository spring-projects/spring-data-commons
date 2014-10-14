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
import java.util.concurrent.ConcurrentMap;

import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.util.Assert;

/**
 * {@link KeyValueAdapter} implementation for {@link Map}.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public class MapKeyValueAdapter extends AbstractKeyValueAdapter {

	private ConcurrentMap<Serializable, Map<Serializable, Object>> data = new ConcurrentHashMap<Serializable, Map<Serializable, Object>>();

	public MapKeyValueAdapter() {
		super();
	}

	@Override
	public Object put(Serializable id, Object item, Serializable keyspace) {

		Assert.notNull(id, "Cannot add item with 'null' id.");
		Assert.notNull(keyspace, "Cannot add item for 'null' collection.");

		return getValues(keyspace).put(id, item);
	}

	@Override
	public boolean contains(Serializable id, Serializable keyspace) {
		return get(id, keyspace) != null;
	}

	@Override
	public Object get(Serializable id, Serializable keyspace) {

		Assert.notNull(id, "Cannot get item with 'null' id.");
		return getValues(keyspace).get(id);
	}

	@Override
	public Object delete(Serializable id, Serializable keyspace) {

		Assert.notNull(id, "Cannot delete item with 'null' id.");
		return getValues(keyspace).remove(id);
	}

	@Override
	public Collection<?> getAllOf(Serializable keyspace) {
		return getValues(keyspace).values();
	}

	@Override
	public void deleteAllOf(Serializable keyspace) {
		getValues(keyspace).clear();
	}

	@Override
	public void clear() {
		data.clear();
	}

	protected Map<Serializable, Object> getValues(Object item) {

		Assert.notNull(item, "Item must not be 'null' for lookup.");
		return getValues(item.getClass());
	}

	protected Map<Serializable, Object> getValues(Serializable collection) {

		Assert.notNull(collection, "Collection must not be 'null' for lookup.");

		Map<Serializable, Object> map = data.get(collection);
		if (map != null) {
			return map;
		}

		data.put(collection, new ConcurrentHashMap<Serializable, Object>());
		return data.get(collection);
	}

}
