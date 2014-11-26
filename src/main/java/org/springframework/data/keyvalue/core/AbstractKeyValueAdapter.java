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
package org.springframework.data.keyvalue.core;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.data.keyvalue.core.query.KeyValueQuery;

/**
 * Base implementation of {@link KeyValueAdapter} holds {@link QueryEngine} to delegate {@literal find} and
 * {@literal count} execution to.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public abstract class AbstractKeyValueAdapter implements KeyValueAdapter {

	private final QueryEngine<? extends KeyValueAdapter, ?, ?> engine;

	/**
	 * Creates new {@link AbstractKeyValueAdapter} with using the default query engine.
	 */
	protected AbstractKeyValueAdapter() {
		this(null);
	}

	/**
	 * Creates new {@link AbstractKeyValueAdapter} with using the default query engine.
	 * 
	 * @param engine will be defaulted to {@link SpelQueryEngine} if {@literal null}.
	 */
	protected AbstractKeyValueAdapter(QueryEngine<? extends KeyValueAdapter, ?, ?> engine) {

		this.engine = engine != null ? engine : new SpelQueryEngine<KeyValueAdapter>();
		this.engine.registerAdapter(this);
	}

	/**
	 * Get the {@link QueryEngine} used.
	 * 
	 * @return
	 */
	protected QueryEngine<? extends KeyValueAdapter, ?, ?> getQueryEngine() {
		return engine;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#find(org.springframework.data.keyvalue.core.query.KeyValueQuery, java.io.Serializable)
	 */
	@Override
	public Collection<?> find(KeyValueQuery<?> query, Serializable keyspace) {
		return engine.execute(query, keyspace);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.KeyValueAdapter#count(org.springframework.data.keyvalue.core.query.KeyValueQuery, java.io.Serializable)
	 */
	@Override
	public long count(KeyValueQuery<?> query, Serializable keyspace) {
		return engine.count(query, keyspace);
	}
}
