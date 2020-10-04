/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link MultiValueBinding} creating {@link Predicate} based on the {@link Path}s type.
 * Binds:
 * <ul>
 * <li><i>{@link java.lang.Object}</i> as {@link SimpleExpression#eq()} on simple properties.</li>
 * <li><i>{@link java.lang.Object}</i> as {@link SimpleExpression#contains()} on collection properties.</li>
 * <li><i>{@link java.util.Collection}</i> as {@link SimpleExpression#in()} on simple properties.</li>
 * </ul>
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Colin Gao
 * @since 1.11
 */
class QuerydslDefaultBinding implements MultiValueBinding<Path<? extends Object>, Object> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.querydsl.QueryDslPredicateBuilder#buildPredicate(org.springframework.data.mapping.PropertyPath, java.lang.Object)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Optional<Predicate> bind(Path<?> path, Collection<? extends Object> valueAndOperation) {

		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(valueAndOperation, "Value must not be null!");

		if (valueAndOperation.isEmpty()) {
			return Optional.empty();
		}

		if (path instanceof CollectionPathBase) {

			BooleanBuilder builder = new BooleanBuilder();

			for (Object element : valueAndOperation) {
				Object value = ((Pair<Object, Ops>)element).getFirst();
				builder.and(((CollectionPathBase) path).contains(value));
			}

			return Optional.of(builder.getValue());
		}

		if (path instanceof SimpleExpression) {

			SimpleExpression expression = (SimpleExpression) path;

			if (valueAndOperation.size() > 1) {
				List<Object> values = valueAndOperation.stream().map(valueOperationPair -> ((Pair<Object, Ops>) valueOperationPair).getFirst()).collect(Collectors.toList());
				return Optional.of(expression.in(values));
			}

			Pair<Object, Ops> valueOperationPair = (Pair<Object, Ops>) valueAndOperation.iterator().next();
			if (valueOperationPair == null) {
				return Optional.of(expression.isNull());
			}

			Object value = valueOperationPair.getFirst();
			Ops operation = valueOperationPair.getSecond();

			if (value == null) {
				return Optional.of(expression.isNull());
			} else if (operation == null) {
				// If operation is null, it means the string provided does not have valid Ops value
				// As we have already logged a warn message while parsing string in QueryPredicateBuilder#getValueOpsPair,
				// ignore this and return a new BooleanBuilder
				return Optional.of(new BooleanBuilder());
			} else {
				return Optional.of(Expressions.booleanOperation(operation, path, ConstantImpl.create(value)));
			}
		}

		throw new IllegalArgumentException(
				String.format("Cannot create predicate for path '%s' with type '%s'.", path, path.getMetadata().getPathType()));
	}
}
