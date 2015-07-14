/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.querydsl;

import java.util.Collection;

import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.CollectionPathBase;

/**
 * Default implementation of {@link QuerydslBinding} creating {@link Predicate} based on the {@link Path}s type. Binds:
 * <ul>
 * <li><i>{@literal null}</i> as {@link SimpleExpression#isNull()}.</li>
 * <li><i>{@link java.lang.Object}</i> as {@link SimpleExpression#eq()} on simple properties.</li>
 * <li><i>{@link java.lang.Object}</i> as {@link SimpleExpression#contains()} on collection properties.</li>
 * <li><i>{@link java.util.Collection}</i> as {@link SimpleExpression#in()} on simple properties.</li>
 * </ul>
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
class QuerydslDefaultBinding implements QuerydslBinding<Path<?>> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.querydsl.QueryDslPredicateBuilder#buildPredicate(org.springframework.data.mapping.PropertyPath, java.lang.Object)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate bind(Path<?> path, Object source) {

		if (source == null && path instanceof SimpleExpression) {
			return ((SimpleExpression) path).isNull();
		}

		if (path instanceof CollectionPathBase) {
			return ((CollectionPathBase) path).contains(source);
		}

		if (path instanceof SimpleExpression) {

			if (source instanceof Collection) {
				return ((SimpleExpression) path).in((Collection) source);
			}

			return ((SimpleExpression) path).eq(source);
		}

		throw new IllegalArgumentException(String.format("Cannot create predicate for path '%s' with type '%s'.", path,
				path.getMetadata().getPathType()));
	}

}
