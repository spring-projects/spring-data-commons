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
package org.springframework.data.repository.inmemory.hazelcast;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.util.Assert;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * @author Christoph Strobl
 */
public class HazelcastAdapter implements InMemoryAdapter, DisposableBean {

	private HazelcastInstance hzInstance;

	public HazelcastAdapter() {
		this(Hazelcast.newHazelcastInstance());
	}

	HazelcastAdapter(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
	}

	@Override
	public Object put(Serializable id, Object item) {
		return getMap(item).put(id, item);
	}

	@Override
	public boolean contains(Serializable id, Class<?> type) {
		return getMap(type).containsKey(id);
	}

	@Override
	public <T> T get(Serializable id, Class<T> type) {
		return (T) getMap(type).get(id);
	}

	@Override
	public <T> T delete(Serializable id, Class<T> type) {
		return (T) getMap(type).remove(id);
	}

	@Override
	public <T> Collection<T> getAllOf(Class<T> type) {
		return getMap(type).values();
	}

	@Override
	public void deleteAllOf(Class<?> type) {
		getMap(type).clear();
	}

	@Override
	public void clear() {
		// TODO

	}

	protected IMap getMap(Object item) {

		Assert.notNull(item, "Item must not be 'null' for lookup.");
		return getMap(item.getClass());
	}

	protected IMap getMap(final Class<?> type) {
		return hzInstance.getMap(type.getName());
	}

	@Override
	public void destroy() throws Exception {
		this.hzInstance.shutdown();
	}

}
