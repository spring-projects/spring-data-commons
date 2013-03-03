/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository.augment;

import org.springframework.util.Assert;

/**
 * The context of a query to be executed. Holding the actual query as well as a queryMode to express in which context
 * the query shall be executed.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public class QueryContext<T> {

	/**
	 * The mode of the query execution.
	 * 
	 * @since 1.6
	 * @author Oliver Gierke
	 */
	public static enum QueryMode {

		/**
		 * To be used for queries originating from find methods of CRUD functionality.
		 */
		FIND,

		/**
		 * TO be used for queries executed for exist queries.
		 */
		EXIST,

		/**
		 * To be used for the query to be executed.
		 */
		COUNT,

		/**
		 * To be used for the execution of custom query methods.
		 */
		QUERY,

		/**
		 * To be used for the execution of the additional count query to be executed when paging.
		 */
		COUNT_FOR_PAGING;

		/**
		 * Returns whether the {@link QueryMode} is one of the given ones.
		 * 
		 * @param modes must not be {@literal null}.
		 * @return
		 */
		public boolean in(QueryMode... modes) {

			for (QueryMode queryMode : modes) {
				if (queryMode == this) {
					return true;
				}
			}

			return false;
		}
	}

	private final T query;
	private final QueryMode queryMode;

	/**
	 * Creates a new {@link QueryContext}.
	 * 
	 * @param query the query about to be executed, must not be {@literal null}.
	 * @param queryMode the queryMode in which the query shall be executed, must not be {@literal null}.
	 */
	public QueryContext(T query, QueryMode queryMode) {

		Assert.notNull(query, "Query must not be null!");
		Assert.notNull(queryMode, "QueryMode must not be null!");

		this.query = query;
		this.queryMode = queryMode;
	}

	/**
	 * Returns the query to be executed.
	 * 
	 * @return the query
	 */
	public T getQuery() {
		return query;
	}

	/**
	 * Returns the execution queryMode.
	 * 
	 * @return the queryMode
	 */
	public QueryMode getMode() {
		return queryMode;
	}
}
