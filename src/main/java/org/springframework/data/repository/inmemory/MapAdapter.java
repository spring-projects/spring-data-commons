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
package org.springframework.data.repository.inmemory;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link InMemoryAdapter} implementation for access to data stored in {@link Map}.
 * 
 * @author Christoph Strobl
 */
public class MapAdapter implements InMemoryAdapter {

	private ConcurrentMap<Class<?>, Map<Serializable, Object>> data = new ConcurrentHashMap<Class<?>, Map<Serializable, Object>>();

	@Override
	public Object put(Serializable id, Object item) {
		return getValues(item).put(id, item);
	}

	@Override
	public boolean contains(Serializable id, Class<?> type) {
		return get(id, type) != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Serializable id, Class<T> type) {
		return (T) getValues(type).get(id);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getAllOf(Class<T> type) {
		return (Collection<T>) getValues(type).values();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T delete(Serializable id, Class<T> type) {
		return (T) getValues(type).remove(id);
	}

	@Override
	public void deleteAllOf(Class<?> type) {
		getValues(type).clear();
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
