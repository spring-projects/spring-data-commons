/*
 * Copyright 2008-2013 the original author or authors.
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
package org.springframework.data.repository.query.parser;

import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.util.Assert;

/**
 * Base class for query creators that create criteria based queries from a {@link PartTree}.
 * 
 * @param T the actual query type to be created
 * @param S the intermediate criteria type
 * @author Oliver Gierke
 */
public abstract class AbstractQueryCreator<T, S> {

	private final ParameterAccessor parameters;
	private final PartTree tree;

	/**
	 * Creates a new {@link AbstractQueryCreator} for the given {@link PartTree} and {@link ParametersParameterAccessor}.
	 * The latter is used to hand actual parameter values into the callback methods as well as to apply dynamic sorting
	 * via a {@link Sort} parameter.
	 * 
	 * @param tree must not be {@literal null}.
	 * @param parameters can be {@literal null}.
	 */
	public AbstractQueryCreator(PartTree tree, ParameterAccessor parameters) {

		Assert.notNull(tree, "PartTree must not be null");

		this.tree = tree;
		this.parameters = parameters;
	}

	/**
	 * Creates a new {@link AbstractQueryCreator} for the given {@link PartTree}. This will cause {@literal null} be
	 * handed for the {@link Iterator} in the callback methods.
	 * 
	 * @param tree must not be {@literal null}.
	 */
	public AbstractQueryCreator(PartTree tree) {

		this(tree, null);
	}

	/**
	 * Creates the actual query object.
	 * 
	 * @return
	 */
	public T createQuery() {
		return createQuery(parameters.getSort());
	}

	/**
	 * Creates the actual query object applying the given {@link Sort} parameter. Use this method in case you haven't
	 * provided a {@link ParameterAccessor} in the first place but want to apply dynamic sorting nevertheless.
	 * 
	 * @param dynamicSort
	 * @return
	 */
	public T createQuery(Sort dynamicSort) {

		return complete(createCriteria(tree), tree.getSort().and(dynamicSort));
	}

	/**
	 * Actual query building logic. Traverses the {@link PartTree} and invokes callback methods to delegate actual
	 * criteria creation and concatenation.
	 * 
	 * @param tree
	 * @return
	 */
	private S createCriteria(PartTree tree) {

		S base = null;
		Iterator<Object> iterator = parameters == null ? null : parameters.iterator();

		for (OrPart node : tree) {

			S criteria = null;

			for (Part part : node) {

				criteria = criteria == null ? create(part, iterator) : and(part, criteria, iterator);
			}

			base = base == null ? criteria : or(base, criteria);
		}

		return base;
	}

	/**
	 * Creates a new atomic instance of the criteria object.
	 * 
	 * @param part
	 * @param iterator
	 * @return
	 */
	protected abstract S create(Part part, Iterator<Object> iterator);

	/**
	 * Creates a new criteria object from the given part and and-concatenates it to the given base criteria.
	 * 
	 * @param part
	 * @param base will never be {@literal null}.
	 * @param iterator
	 * @return
	 */
	protected abstract S and(Part part, S base, Iterator<Object> iterator);

	/**
	 * Or-concatenates the given base criteria to the given new criteria.
	 * 
	 * @param base
	 * @param criteria
	 * @return
	 */
	protected abstract S or(S base, S criteria);

	/**
	 * Actually creates the query object applying the given criteria object and {@link Sort} definition.
	 * 
	 * @param criteria will never be {@literal null}.
	 * @param sort might be {@literal null}.
	 * @return
	 */
	protected abstract T complete(S criteria, Sort sort);
}
