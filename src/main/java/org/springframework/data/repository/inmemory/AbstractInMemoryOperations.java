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
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base implementation of {@link InMemoryOperations} implementing common concerns.
 * 
 * @author Christoph Strobl
 */
public abstract class AbstractInMemoryOperations<Q extends InMemoryQuery> implements InMemoryOperations {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#create(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void create(final Serializable id, final Object objectToInsert) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(objectToInsert, "Object to be inserted must not be 'null'.");

		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {

				if (adapter.contains(id, ClassUtils.getUserClass(objectToInsert))) {
					throw new InvalidDataAccessApiUsageException("Cannot insert existing object. Please use update.");
				}
				adapter.put(id, objectToInsert);
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#update(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public void update(final Serializable id, final Object objectToUpdate) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(objectToUpdate, "Object to be updated must not be 'null'. Use delete to remove.");

		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {
				adapter.put(id, objectToUpdate);
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(java.lang.Class)
	 */
	@Override
	public <T> List<T> read(final Class<T> type) {

		Assert.notNull(type, "Type to fetch must not be 'null'.");

		return execute(new InMemoryCallback<List<T>>() {

			@Override
			public List<T> doInMemory(InMemoryAdapter adapter) {
				return new ArrayList<T>(adapter.getAllOf(type));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T read(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(type, "Type to fetch must not be 'null'.");
		return execute(new InMemoryCallback<T>() {

			@Override
			public T doInMemory(InMemoryAdapter adapter) {
				return adapter.get(id, type);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#delete(java.lang.Class)
	 */
	@Override
	public void delete(final Class<?> type) {

		Assert.notNull(type, "Type to delete must not be 'null'.");
		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {

				adapter.deleteAllOf(type);
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#delete(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T delete(final Serializable id, final Class<T> type) {

		Assert.notNull(id, "Id for object to be inserted must not be 'null'.");
		Assert.notNull(type, "Type to delete must not be 'null'.");
		return execute(new InMemoryCallback<T>() {

			@Override
			public T doInMemory(InMemoryAdapter adapter) {
				return adapter.delete(id, type);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#count(java.lang.Class)
	 */
	@Override
	public long count(Class<?> type) {

		Assert.notNull(type, "Type for count must not be 'null'.");
		return read(type).size();
	}

	@Override
	public <T> T execute(InMemoryCallback<T> action) {

		Assert.notNull(action, "InMemoryCallback must not be 'null'.");

		try {
			return action.doInMemory(this.getAdapter());
		} catch (RuntimeException e) {

			// TODO: potentially convert runtime exception?
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(org.springframework.data.repository.inmemory.InMemoryQuery, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> read(InMemoryQuery query, Class<T> type) {
		return doRead((Q) query, type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#count(org.springframework.data.repository.inmemory.InMemoryQuery, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public long count(InMemoryQuery query, Class<?> type) {
		return doCount((Q) query, type);
	}

	protected abstract InMemoryAdapter getAdapter();

	protected abstract <T> List<T> doRead(Q query, Class<T> type);

	protected abstract long doCount(Q query, Class<?> type);

}
