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

import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;

/**
 * Resolves the {@link Sort} object from given {@link KeyValueQuery} and potentially converts it into a store specific
 * representation that can be used by the {@link QueryEngine} implementation.
 * 
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 */
public interface SortAccessor<T> {

	/**
	 * Reads {@link KeyValueQuery#getSort()} of given {@link KeyValueQuery} and applies required transformation to match
	 * the desired type.
	 * 
	 * @param query can be {@literal null}.
	 * @return {@literal null} in case {@link Sort} has not been defined on {@link KeyValueQuery}.
	 */
	T resolve(KeyValueQuery<?> query);

}
