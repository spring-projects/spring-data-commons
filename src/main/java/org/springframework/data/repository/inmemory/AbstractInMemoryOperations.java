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
public abstract class AbstractInMemoryOperations implements InMemoryOperations {

	@Override
	public void create(final Serializable id, final Object objectToInsert) {

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

	@Override
	public void update(final Serializable id, final Object objectToUpdate) {

		Assert.notNull(objectToUpdate, "Object to be updated must not be 'null'.");

		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {
				adapter.put(id, objectToUpdate);
				return null;
			}
		});
	}

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

	@Override
	public <T> T read(final Serializable id, final Class<T> type) {

		Assert.notNull(type, "Type to fetch must not be 'null'.");
		return execute(new InMemoryCallback<T>() {

			@Override
			public T doInMemory(InMemoryAdapter adapter) {
				return adapter.get(id, type);
			}
		});
	}

	@Override
	public void delete(final Class<?> type) {

		Assert.notNull(type, "Type to fetch must not be 'null'.");
		execute(new InMemoryCallback<Void>() {

			@Override
			public Void doInMemory(InMemoryAdapter adapter) {

				adapter.deleteAllOf(type);
				return null;
			}
		});
	}

	@Override
	public <T> T delete(final Serializable id, final Class<T> type) {

		Assert.notNull(type, "Type to fetch must not be 'null'.");
		return execute(new InMemoryCallback<T>() {

			@Override
			public T doInMemory(InMemoryAdapter adapter) {
				return adapter.delete(id, type);
			}
		});
	}

	@Override
	public long count(Class<?> type) {
		return read(type).size();
	}

	@Override
	public <T> T execute(InMemoryCallback<T> action) {

		Assert.notNull(action);

		try {
			return action.doInMemory(this.getAdapter());
		} catch (RuntimeException e) {
			throw e;
		}
	}

	protected abstract InMemoryAdapter getAdapter();
}
