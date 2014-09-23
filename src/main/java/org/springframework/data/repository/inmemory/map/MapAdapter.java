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
package org.springframework.data.repository.inmemory.map;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link InMemoryAdapter} implementation for access to data stored in {@link Map}.
 * 
 * @author Christoph Strobl
 */
public class MapAdapter implements InMemoryAdapter {

	private ConcurrentMap<Class<?>, Map<Serializable, Object>> data = new ConcurrentHashMap<Class<?>, Map<Serializable, Object>>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#put(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public Object put(Serializable id, Object item) {

		Assert.notNull(id, "Cannot add item with 'null' id.");
		return getValues(item).put(id, item);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#contains(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public boolean contains(Serializable id, Class<?> type) {
		return get(id, type) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#get(java.io.Serializable, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Serializable id, Class<T> type) {

		Assert.notNull(id, "Cannot get item with 'null' id.");
		return (T) getValues(type).get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#getAllOf(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getAllOf(Class<T> type) {
		return (Collection<T>) getValues(type).values();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#delete(java.io.Serializable, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T delete(Serializable id, Class<T> type) {

		Assert.notNull(id, "Cannot delete item with 'null' id.");
		return (T) getValues(type).remove(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#deleteAllOf(java.lang.Class)
	 */
	@Override
	public void deleteAllOf(Class<?> type) {
		getValues(type).clear();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#clear()
	 */
	@Override
	public void clear() {
		data.clear();
	}

	protected Map<Serializable, Object> getValues(Object item) {

		Assert.notNull(item, "Item must not be 'null' for lookup.");
		return getValues(item.getClass());
	}

	protected Map<Serializable, Object> getValues(Class<?> type) {

		Assert.notNull(type, "Type must not be 'null' for lookup.");
		Class<?> userType = ClassUtils.getUserClass(type);
		if (!data.containsKey(userType)) {
			data.put(userType, new LinkedHashMap<Serializable, Object>());
		}
		return data.get(userType);
	}
}
