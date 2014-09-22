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
package org.springframework.data.keyvalue.hazelcast;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.data.keyvalue.core.AbstractKeyValueAdapter;
import org.springframework.util.Assert;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 * @author Christoph Strobl
 */
public class HazelcastKeyValueAdapter extends AbstractKeyValueAdapter {

	private HazelcastInstance hzInstance;

	public HazelcastKeyValueAdapter() {
		this(Hazelcast.newHazelcastInstance());
	}

	public HazelcastKeyValueAdapter(HazelcastInstance hzInstance) {

		super(new HazelcastQueryEngine());
		Assert.notNull(hzInstance, "hzInstance must not be 'null'.");
		this.hzInstance = hzInstance;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object put(Serializable id, Object item, Serializable keyspace) {

		Assert.notNull(id, "Id must not be 'null' for adding.");
		Assert.notNull(item, "Item must not be 'null' for adding.");

		return getMap(keyspace).put(id, item);
	}

	@Override
	public boolean contains(Serializable id, Serializable keyspace) {
		return getMap(keyspace).containsKey(id);
	}

	@Override
	public Object get(Serializable id, Serializable keyspace) {
		return getMap(keyspace).get(id);
	}

	@Override
	public Object delete(Serializable id, Serializable keyspace) {
		return getMap(keyspace).remove(id);
	}

	@Override
	public Collection<?> getAllOf(Serializable keyspace) {
		return getMap(keyspace).values();
	}

	@Override
	public void deleteAllOf(Serializable keyspace) {
		getMap(keyspace).clear();
	}

	@Override
	public void clear() {
		// TODO: remove all elements
	}

	@SuppressWarnings("rawtypes")
	protected IMap getMap(final Serializable keyspace) {

		Assert.isInstanceOf(String.class, keyspace, "Keyspace identifier must of of type String.");
		return hzInstance.getMap((String) keyspace);
	}

	@Override
	public void destroy() throws Exception {
		hzInstance.shutdown();
	}

}
