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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.EntityInformation;

import com.mysema.query.collections.CollQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;

/**
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
public class QueryDslInMemoryRepository<T, ID extends Serializable> extends BasicInMemoryRepository<T, ID> implements
		QueryDslPredicateExecutor<T> {

	private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;

	private final EntityPath<T> path;
	private final PathBuilder<T> builder;

	public QueryDslInMemoryRepository(EntityInformation<T, ID> entityInformation, InMemoryOperations inMemoryOps) {
		this(entityInformation, inMemoryOps, DEFAULT_ENTITY_PATH_RESOLVER);
	}

	public QueryDslInMemoryRepository(EntityInformation<T, ID> entityInformation, InMemoryOperations inMemoryOps,
			EntityPathResolver resolver) {

		super(entityInformation, inMemoryOps);
		this.path = resolver.createPath(entityInformation.getJavaType());
		this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
	}

	@Override
	public T findOne(Predicate predicate) {

		CollQuery cq = new CollQuery();
		cq.from(builder, findAll());
		cq.where(predicate);
		return cq.uniqueResult(builder);
	}

	@Override
	public Iterable<T> findAll(Predicate predicate) {

		CollQuery cq = prepareQuery(predicate);
		return cq.list(builder);
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

		CollQuery cq = prepareQuery(predicate);
		cq.orderBy(orders);
		return cq.list(builder);
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		CollQuery cq = prepareQuery(predicate);
		cq.offset(pageable.getOffset());
		cq.limit(pageable.getPageSize());

		if (pageable.getSort() != null) {

			// TODO: sorting
			throw new UnsupportedOperationException("Sort has not been implemented yet. sorry!");
		}
		return new PageImpl<T>(cq.list(builder), pageable, count(predicate));
	}

	@Override
	public long count(Predicate predicate) {

		CollQuery cq = prepareQuery(predicate);
		return cq.count();
	}

	private CollQuery prepareQuery(Predicate predicate) {

		CollQuery cq = new CollQuery();
		cq.from(builder, findAll());
		cq.where(predicate);

		return cq;
	}

}
