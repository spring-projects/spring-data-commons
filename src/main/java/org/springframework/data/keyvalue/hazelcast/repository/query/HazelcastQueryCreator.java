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
package org.springframework.data.keyvalue.hazelcast.repository.query;

import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.ehcache.repository.query.EhCacheQueryCreator;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;

/**
 * @author Christoph Strobl
 */
public class HazelcastQueryCreator extends AbstractQueryCreator<KeyValueQuery<Predicate<?, ?>>, Predicate<?, ?>> {

	private final PredicateBuilder predicateBuilder;

	/**
	 * Creates a new {@link EhCacheQueryCreator} for the given {@link PartTree}.
	 * 
	 * @param tree must not be {@literal null}.
	 */
	public HazelcastQueryCreator(PartTree tree) {
		super(tree);
		this.predicateBuilder = new PredicateBuilder();
	}

	/**
	 * Creates a new {@link HazelcastQueryCreator} for the given {@link PartTree} and {@link ParameterAccessor}. The
	 * latter is used to hand actual parameter values into the callback methods as well as to apply dynamic sorting via a
	 * {@link Sort} parameter.
	 * 
	 * @param tree must not be {@literal null}.
	 * @param parameters can be {@literal null}.
	 */
	public HazelcastQueryCreator(PartTree tree, ParameterAccessor parameters) {
		super(tree, parameters);
		this.predicateBuilder = new PredicateBuilder();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#create(org.springframework.data.repository.query.parser.Part, java.util.Iterator)
	 */
	@Override
	protected Predicate<?, ?> create(Part part, Iterator<Object> iterator) {
		return from(predicateBuilder, part, iterator);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#and(org.springframework.data.repository.query.parser.Part, java.lang.Object, java.util.Iterator)
	 */
	@Override
	protected Predicate<?, ?> and(Part part, Predicate<?, ?> base, Iterator<Object> iterator) {
		return predicateBuilder.and(from(predicateBuilder, part, iterator));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#or(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected Predicate<?, ?> or(Predicate<?, ?> base, Predicate<?, ?> criteria) {
		return predicateBuilder.or(criteria);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#complete(java.lang.Object, org.springframework.data.domain.Sort)
	 */
	@Override
	protected KeyValueQuery<Predicate<?, ?>> complete(Predicate<?, ?> criteria, Sort sort) {
		return new KeyValueQuery<Predicate<?, ?>>(criteria);
	}

	private Predicate<?, ?> from(PredicateBuilder pb, Part part, Iterator<Object> iterator) {

		EntryObject e = pb.getEntryObject();
		e.get(part.getProperty().toDotPath());

		switch (part.getType()) {
			case TRUE:
				return e.equal(true);
			case FALSE:
				return e.equal(false);
			case SIMPLE_PROPERTY:
				return e.equal((Comparable<?>) iterator.next());
			case IS_NULL:
				return e.isNull();
			case GREATER_THAN:
				return e.greaterThan((Comparable<?>) iterator.next());

			default:
				throw new InvalidDataAccessApiUsageException(String.format("Found invalid part '%s' in query", part.getType()));
		}
	}
}
