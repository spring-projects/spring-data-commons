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
package org.springframework.data.repository.inmemory.map;

import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.data.util.SpelUtil;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Christoph Strobl
 */
public class SpelQueryCreator extends AbstractQueryCreator<MapQuery, String> {

	private SpelExpression expression;

	public SpelQueryCreator(PartTree tree, ParameterAccessor parameters) {

		super(tree, parameters);
		this.expression = toPredicateExpression(tree);
	}

	@Override
	protected String create(Part part, Iterator<Object> iterator) {
		return "";
	}

	@Override
	protected String and(Part part, String base, Iterator<Object> iterator) {
		return "";
	}

	@Override
	protected String or(String base, String criteria) {
		return "";
	}

	@Override
	protected MapQuery complete(String criteria, Sort sort) {

		MapQuery query = new MapQuery(this.expression);
		if (sort != null) {
			query.orderBy(sort);
		}
		return query;
	}

	protected SpelExpression toPredicateExpression(PartTree tree) {

		int parameterIndex = 0;
		StringBuilder sb = new StringBuilder();

		for (Iterator<OrPart> orPartIter = tree.iterator(); orPartIter.hasNext();) {

			OrPart orPart = orPartIter.next();

			int partCnt = 0;
			StringBuilder partBuilder = new StringBuilder();
			for (Iterator<Part> partIter = orPart.iterator(); partIter.hasNext();) {

				Part part = partIter.next();
				partBuilder.append("#it?.");
				partBuilder.append(part.getProperty().toDotPath().replace(".", ".?"));

				// TODO: check if we can have caseinsensitive search
				if (!part.shouldIgnoreCase().equals(IgnoreCaseType.NEVER)) {
					throw new InvalidDataAccessApiUsageException("Ignore case not supported");
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
						partBuilder.append(part.getProperty().toDotPath().replace(".", ".?"));
						partBuilder.append("<").append("[").append(parameterIndex++).append("]");
						partBuilder.append(")");
						break;
					case REGEX:

						partBuilder.append(" matches ").append("[").append(parameterIndex++).append("]");
						break;
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

		// not all SpEL queries can safely be compiled. To avoid crashes here we explicitly turn them off.
		SpelParserConfiguration config = SpelUtil.silentlyCreateParserConfiguration("OFF");
		return (SpelExpression) new SpelExpressionParser(config).parseExpression(sb.toString());
	}
}
