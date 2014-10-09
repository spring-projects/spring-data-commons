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

import org.springframework.beans.BeanUtils;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SerializationUtils;

/**
 * {@link InMemoryAdapter} implementation for access to data stored in {@link Map}.
 * 
 * @author Christoph Strobl
 */
public class MapAdapter implements InMemoryAdapter {

	private ConcurrentMap<Serializable, Map<Serializable, Object>> data = new ConcurrentHashMap<Serializable, Map<Serializable, Object>>();

	private static final MapPersistenceStrategy DEFAULT_PERSISTENCE_STRATEGY = new CloningPersistenceStragtegy();
	private MapPersistenceStrategy persistenceStrategy;

	public MapAdapter() {
		this(DEFAULT_PERSISTENCE_STRATEGY);
	}

	public MapAdapter(MapPersistenceStrategy persistenceStrategy) {
		this.persistenceStrategy = persistenceStrategy != null ? persistenceStrategy : DEFAULT_PERSISTENCE_STRATEGY;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#put(java.io.Serializable, java.lang.Object, java.io.Serializable)
	 */
	@Override
	public Object put(Serializable id, Object item, Serializable collection) {

		Assert.notNull(id, "Cannot add item with 'null' id.");
		Assert.notNull(collection, "Cannot add item for 'null' collection.");

		return getValues(collection).put(id, persistenceStrategy.getPersistentObject(item));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#contains(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public boolean contains(Serializable id, Serializable collection) {
		return get(id, collection) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#get(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object get(Serializable id, Serializable collection) {

		Assert.notNull(id, "Cannot get item with 'null' id.");
		return getValues(collection).get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#getAllOf(java.io.Serializable)
	 */
	@Override
	public Collection<?> getAllOf(Serializable collection) {
		return getValues(collection).values();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#delete(java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public Object delete(Serializable id, Serializable collection) {

		Assert.notNull(id, "Cannot delete item with 'null' id.");
		return getValues(collection).remove(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#deleteAllOf(java.io.Serializable)
	 */
	@Override
	public void deleteAllOf(Serializable collection) {
		getValues(collection).clear();
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

	protected Map<Serializable, Object> getValues(Serializable collection) {

		Assert.notNull(collection, "Collection must not be 'null' for lookup.");

		if (!data.containsKey(collection)) {
			data.put(collection, new LinkedHashMap<Serializable, Object>());
		}
		return data.get(collection);
	}

	/**
	 * @author Christoph Strobl
	 */
	public static interface MapPersistenceStrategy {
		Object getPersistentObject(Object o);
	}

	/**
	 * Implementation of {@link MapPersistenceStrategy} using {@code clone()} or {@link SerializationUtils} for making a
	 * copy. This allows to decouple pointers to values that exist in the map.
	 * 
	 * @author Christoph Strobl
	 */
	static class CloningPersistenceStragtegy implements MapPersistenceStrategy {

		@Override
		public Object getPersistentObject(Object o) {

			if (o == null) {
				return o;
			}
			if (o instanceof Cloneable) {
				try {
					return ReflectionUtils.findMethod(o.getClass(), "clone").invoke(o);
				} catch (Exception o_O) {
					//
				}
			}

			if (o instanceof Serializable) {
				try {
					return SerializationUtils.deserialize(SerializationUtils.serialize(o));
				} catch (Exception o_O) {
					//
				}
			}

			Object target;
			try {
				target = o.getClass().newInstance();
				BeanUtils.copyProperties(o, target);
				return target;
			} catch (Exception e) {
				//
			}

			throw new UnsupportedOperationException(String.format(
					"Unable to create persistable object for %s. Please make sure it implements Cloneable or Serializable.",
					o.getClass()));
		}
	}

	public static class WriteThroughPersistenceStrategy implements MapPersistenceStrategy {

		@Override
		public Object getPersistentObject(Object o) {
			return o;
		}

	}

}
