/*
 * Copyright 2015-2024 the original author or authors.
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

import java.util.Collection;
import java.util.Optional;

import org.springframework.util.Assert;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.SimpleExpression;

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
 * @author Johannes Englmeier
 * @author Ngoc Nhan
 * @since 1.11
 */
class QuerydslDefaultBinding implements MultiValueBinding<Path<? extends Object>, Object> {

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Optional<Predicate> bind(Path<?> path, Collection<? extends Object> value) {

		Assert.notNull(path, "Path must not be null");
		Assert.notNull(value, "Value must not be null");

		if (value.isEmpty()) {
			return Optional.empty();
		}

		if (path instanceof CollectionPathBase collectionPathBase) {

			BooleanBuilder builder = new BooleanBuilder();

			for (Object element : value) {

				if (element instanceof Collection<?> nestedCollection) {

					for (Object nested : nestedCollection) {
						builder.and(collectionPathBase.contains(nested));
					}
				} else {
					builder.and(collectionPathBase.contains(element));
				}

			}

			return Optional.of(builder.getValue());
		}

		if (path instanceof SimpleExpression expression) {

			if (value.size() > 1) {
				return Optional.of(expression.in(value));
			}

			Object object = value.iterator().next();

			return Optional.of(object == null //
					? expression.isNull() //
					: expression.eq(object));
		}

		throw new IllegalArgumentException(
				String.format("Cannot create predicate for path '%s' with type '%s'", path, path.getMetadata().getPathType()));
	}
}
