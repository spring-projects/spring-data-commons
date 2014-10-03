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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#put(java.io.Serializable, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public Object put(Serializable id, Object item, Serializable collection) {
		return getMap(collection).put(id, item);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#contains(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public boolean contains(Serializable id, Serializable collection) {
		return getMap(collection).containsKey(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#get(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object get(Serializable id, Serializable collection) {
		return getMap(collection).get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#delete(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object delete(Serializable id, Serializable collection) {
		return getMap(collection).remove(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#getAllOf(java.io.Serializable)
	 */
	@Override
	public Collection<?> getAllOf(Serializable collection) {
		return getMap(collection).values();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#deleteAllOf(java.io.Serializable)
	 */
	@Override
	public void deleteAllOf(Serializable collection) {
		getMap(collection).clear();
	}

	@Override
	public void clear() {
		// TODO: clean haszelcase instance
	}

	@SuppressWarnings("rawtypes")
	protected IMap getMap(final Serializable collection) {

		Assert.isInstanceOf(String.class, collection, "Collection identifier must of of type String.");
		return hzInstance.getMap((String) collection);
	}

	@Override
	public void destroy() throws Exception {
		this.hzInstance.shutdown();
	}

}
