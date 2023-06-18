/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.repository.query;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

/**
 * Interface to allow execution of Query by Example {@link Example} instances. This an extension to
 * {@link QueryByExampleExecutor} returning {@link List} instead of {@link Iterable} where applicable.
 *
 * @param <T> the type of entity for which this executor acts on.
 * @author Jens Schauder
 * @since 3.0
 * @see QueryByExampleExecutor
 */
public interface ListQueryByExampleExecutor<T> extends QueryByExampleExecutor<T> {

	/**
	 * Returns all entities matching the given {@link Example}. In case no match could be found an empty {@link List} is
	 * returned.
	 *
	 * @param example must not be {@literal null}.
	 * @return all entities matching the given {@link Example}.
	 */
	@Override
	<S extends T> List<S> findAll(Example<S> example);

	/**
	 * Returns all entities matching the given {@link Example} applying the given {@link Sort}. In case no match could be
	 * found an empty {@link List} is returned.
	 *
	 * @param example must not be {@literal null}.
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return all entities matching the given {@link Example}.
	 */
	@Override
	<S extends T> List<S> findAll(Example<S> example, Sort sort);

}
