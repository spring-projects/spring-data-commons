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
package org.springframework.data.keyvalue.repository;

import static org.springframework.data.querydsl.QueryDslUtils.*;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.EntityInformation;

import com.mysema.query.collections.CollQuery;
import com.mysema.query.support.ProjectableQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;

/**
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 * @param <ID>
 */
public class QueryDslKeyValueRepository<T, ID extends Serializable> extends BasicKeyValueRepository<T, ID> implements
		QueryDslPredicateExecutor<T> {

	private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;

	private final EntityPath<T> path;
	private final PathBuilder<T> builder;

	public QueryDslKeyValueRepository(EntityInformation<T, ID> entityInformation, KeyValueOperations operations) {
		this(entityInformation, operations, DEFAULT_ENTITY_PATH_RESOLVER);
	}

	public QueryDslKeyValueRepository(EntityInformation<T, ID> entityInformation, KeyValueOperations operations,
			EntityPathResolver resolver) {

		super(entityInformation, operations);
		this.path = resolver.createPath(entityInformation.getJavaType());
		this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
	}

	@Override
	public T findOne(Predicate predicate) {

		ProjectableQuery<?> query = prepareQuery(predicate);
		return query.uniqueResult(builder);
	}

	@Override
	public Iterable<T> findAll(Predicate predicate) {

		ProjectableQuery<?> query = prepareQuery(predicate);
		return query.list(builder);
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

		ProjectableQuery<?> query = prepareQuery(predicate);
		query.orderBy(orders);
		return query.list(builder);
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		ProjectableQuery<?> query = prepareQuery(predicate);

		if (pageable != null) {
			query.offset(pageable.getOffset());
			query.limit(pageable.getPageSize());

			if (pageable.getSort() != null) {
				query.orderBy(toOrderSpecifier(pageable.getSort(), builder));
			}
		}

		return new PageImpl<T>(query.list(builder), pageable, count(predicate));
	}

	@Override
	public long count(Predicate predicate) {

		ProjectableQuery<?> query = prepareQuery(predicate);
		return query.count();
	}

	/**
	 * Creates executable query for given {@link Predicate}.
	 * 
	 * @param predicate
	 * @return
	 */
	protected ProjectableQuery<?> prepareQuery(Predicate predicate) {

		CollQuery query = new CollQuery();
		query.from(builder, findAll());
		query.where(predicate);

		return query;
	}

}
