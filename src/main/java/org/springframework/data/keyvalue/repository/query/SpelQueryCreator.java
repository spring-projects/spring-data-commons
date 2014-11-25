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
package org.springframework.data.keyvalue.repository.query;

import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.core.spel.SpelExpressionFactory;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * {@link AbstractQueryCreator} to create {@link SpelExpression} based {@link KeyValueQuery}s.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.10
 */
public class SpelQueryCreator extends AbstractQueryCreator<KeyValueQuery<SpelExpression>, String> {

	private final SpelExpression expression;

	/**
	 * Creates a new {@link SpelQueryCreator} for the given {@link PartTree} and {@link ParameterAccessor}.
	 * 
	 * @param tree must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 */
	public SpelQueryCreator(PartTree tree, ParameterAccessor parameters) {

		super(tree, parameters);

		this.expression = toPredicateExpression(tree);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#create(org.springframework.data.repository.query.parser.Part, java.util.Iterator)
	 */
	@Override
	protected String create(Part part, Iterator<Object> iterator) {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#and(org.springframework.data.repository.query.parser.Part, java.lang.Object, java.util.Iterator)
	 */
	@Override
	protected String and(Part part, String base, Iterator<Object> iterator) {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#or(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected String or(String base, String criteria) {
		return "";
	}

	@Override
	protected KeyValueQuery<SpelExpression> complete(String criteria, Sort sort) {

		KeyValueQuery<SpelExpression> query = new KeyValueQuery<SpelExpression>(this.expression);

		if (sort != null) {
			query.orderBy(sort);
		}

		return query;
	}

	protected SpelExpression toPredicateExpression(PartTree tree) {

		int parameterIndex = 0;
		StringBuilder sb = new StringBuilder();

		for (Iterator<OrPart> orPartIter = tree.iterator(); orPartIter.hasNext();) {

			int partCnt = 0;
			StringBuilder partBuilder = new StringBuilder();
			OrPart orPart = orPartIter.next();

			for (Iterator<Part> partIter = orPart.iterator(); partIter.hasNext();) {

				Part part = partIter.next();
				partBuilder.append("#it?.");
				partBuilder.append(part.getProperty().toDotPath().replace(".", "?."));

				// TODO: check if we can have caseinsensitive search
				if (!part.shouldIgnoreCase().equals(IgnoreCaseType.NEVER)) {
					throw new InvalidDataAccessApiUsageException("Ignore case not supported!");
				}

				switch (part.getType()) {
					case TRUE:
						partBuilder.append("?.equals(true)");
						break;
					case FALSE:
						partBuilder.append("?.equals(false)");
						break;
					case SIMPLE_PROPERTY:
						partBuilder.append("?.equals(").append("[").append(parameterIndex++).append("])");
						break;
					case IS_NULL:
						partBuilder.append(" == null");
						break;
					case IS_NOT_NULL:
						partBuilder.append(" != null");
						break;
					case LIKE:
						partBuilder.append("?.contains(").append("[").append(parameterIndex++).append("])");
						break;
					case STARTING_WITH:
						partBuilder.append("?.startsWith(").append("[").append(parameterIndex++).append("])");
						break;
					case AFTER:
					case GREATER_THAN:
						partBuilder.append(">").append("[").append(parameterIndex++).append("]");
						break;
					case GREATER_THAN_EQUAL:
						partBuilder.append(">=").append("[").append(parameterIndex++).append("]");
						break;
					case BEFORE:
					case LESS_THAN:
						partBuilder.append("<").append("[").append(parameterIndex++).append("]");
						break;
					case LESS_THAN_EQUAL:
						partBuilder.append("<=").append("[").append(parameterIndex++).append("]");
						break;
					case ENDING_WITH:
						partBuilder.append("?.endsWith(").append("[").append(parameterIndex++).append("])");
						break;
					case BETWEEN:

						int index = partBuilder.lastIndexOf("#it?.");

						partBuilder.insert(index, "(");
						partBuilder.append(">").append("[").append(parameterIndex++).append("]");
						partBuilder.append("&&");
						partBuilder.append("#it?.");
						partBuilder.append(part.getProperty().toDotPath().replace(".", "?."));
						partBuilder.append("<").append("[").append(parameterIndex++).append("]");
						partBuilder.append(")");

						break;

					case REGEX:

						partBuilder.append(" matches ").append("[").append(parameterIndex++).append("]");
						break;
					case IN:
					case CONTAINING:
					case NOT_CONTAINING:
					case NEGATING_SIMPLE_PROPERTY:
					case EXISTS:
					default:
						throw new InvalidDataAccessApiUsageException(String.format("Found invalid part '%s' in query",
								part.getType()));
				}

				if (partIter.hasNext()) {
					partBuilder.append("&&");
				}

				partCnt++;
			}

			if (partCnt > 1) {
				sb.append("(").append(partBuilder).append(")");
			} else {
				sb.append(partBuilder);
			}

			if (orPartIter.hasNext()) {
				sb.append("||");
			}
		}

		return SpelExpressionFactory.parseRaw(sb.toString());
	}
}
