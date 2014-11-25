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
package org.springframework.data.keyvalue.ehcache.repository.query;

import java.util.Iterator;

import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.EqualTo;
import net.sf.ehcache.search.expression.GreaterThan;
import net.sf.ehcache.search.expression.ILike;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Christoph Strobl
 */
public class EhCacheQueryCreator extends AbstractQueryCreator<KeyValueQuery<Criteria>, Criteria> {

	/**
	 * Creates a new {@link EhCacheQueryCreator} for the given {@link PartTree}.
	 * 
	 * @param tree must not be {@literal null}.
	 */
	public EhCacheQueryCreator(PartTree tree) {
		super(tree);
	}

	/**
	 * Creates a new {@link EhCacheQueryCreator} for the given {@link PartTree} and {@link ParameterAccessor}. The latter
	 * is used to hand actual parameter values into the callback methods as well as to apply dynamic sorting via a
	 * {@link Sort} parameter.
	 * 
	 * @param tree must not be {@literal null}.
	 * @param parameters can be {@literal null}.
	 */
	public EhCacheQueryCreator(PartTree tree, ParameterAccessor parameters) {
		super(tree, parameters);
	}

	/*

	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#create(org.springframework.data.repository.query.parser.Part, java.util.Iterator)
	 */
	@Override
	protected Criteria create(Part part, Iterator<Object> iterator) {
		return from(part, iterator);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#and(org.springframework.data.repository.query.parser.Part, java.lang.Object, java.util.Iterator)
	 */
	@Override
	protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {

		if (base == null) {
			return create(part, iterator);
		}
		return base.and(from(part, iterator));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#or(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected Criteria or(Criteria base, Criteria criteria) {
		return base.or(criteria);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#complete(java.lang.Object, org.springframework.data.domain.Sort)
	 */
	@Override
	protected KeyValueQuery<Criteria> complete(Criteria criteria, Sort sort) {

		KeyValueQuery<Criteria> query = new KeyValueQuery<Criteria>(criteria);
		if (sort != null) {
			query.orderBy(sort);
		}
		return query;
	}

	private Criteria from(Part part, Iterator<Object> iterator) {

		// TODO: complete list of supported types
		switch (part.getType()) {
			case TRUE:
				return new EqualTo(part.getProperty().toDotPath(), true);
			case FALSE:
				return new EqualTo(part.getProperty().toDotPath(), true);
			case SIMPLE_PROPERTY:
				return new EqualTo(part.getProperty().toDotPath(), iterator.next());
			case IS_NULL:
				return new EqualTo(part.getProperty().toDotPath(), null);
			case STARTING_WITH:
			case LIKE:
				return new ILike(part.getProperty().toDotPath(), iterator.next() + "*");
			case GREATER_THAN:
				return new GreaterThan(part.getProperty().toDotPath(), iterator.next());

			default:
				throw new InvalidDataAccessApiUsageException(String.format("Found invalid part '%s' in query", part.getType()));
		}
	}
}
