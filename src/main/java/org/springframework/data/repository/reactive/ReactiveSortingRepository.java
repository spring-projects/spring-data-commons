/*
 * Copyright 2016-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Repository fragment to provide methods to retrieve entities using the sorting abstraction. In many cases it should be
 * combined with {@link ReactiveCrudRepository} or a similar repository interface in order to add CRUD functionality.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author YongHwan Kwon
 * @since 2.0
 * @see Sort
 * @see Mono
 * @see Flux
 * @see ReactiveCrudRepository
 */
@NoRepositoryBean
public interface ReactiveSortingRepository<T, ID> extends Repository<T, ID> {

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort the {@link Sort} specification to sort the results by, can be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return all entities sorted by the given options.
	 * @throws IllegalArgumentException in case the given {@link Sort} is {@literal null}.
	 */
	Flux<T> findAll(Sort sort);

	/**
	 * Returns all entities sorted by the given options with limit.
	 *
	 * @param sort the {@link Sort} specification to sort the results by, can be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @param limit the {@link Limit} to limit the results, must not be {@literal null} or negative.
	 * @return all entities sorted by the given options.
	 * @throws IllegalArgumentException in case the given {@link Sort} is {@literal null} or {@link Limit} is
	 *           {@literal null} or negative.
	 */
	default Flux<T> findAll(Sort sort, Limit limit) {
		if (limit == null) {
			throw new IllegalArgumentException("Limit must not be null");
		}

		if (limit.isUnlimited() || limit.max() == Integer.MAX_VALUE) {
			return findAll(sort);
		}

		final int maxLimit = limit.max();
		if (maxLimit < 0) {
			throw new IllegalArgumentException("Limit value cannot be negative");
		}

		if (maxLimit == 0) {
			throw new IllegalArgumentException("Limit value cannot be zero");
		}

		return findAll(sort).take(maxLimit);
	}
}
