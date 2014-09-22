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
package org.springframework.data.keyvalue.core;

import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.core.spel.SpelExpressionFactory;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * {@link CriteriaAccessor} implementation capable of {@link SpelExpression}s.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public enum SpelCriteriaAccessor implements CriteriaAccessor<SpelExpression> {

	INSTANCE;

	@Override
	public SpelExpression resolve(KeyValueQuery<?> query) {

		if (query.getCritieria() == null) {
			return null;
		}

		if (query.getCritieria() instanceof SpelExpression) {
			return (SpelExpression) query.getCritieria();
		}

		if (query.getCritieria() instanceof String) {
			return SpelExpressionFactory.parseRaw((String) query.getCritieria());
		}

		throw new IllegalArgumentException("Cannot create SpelCriteria for " + query.getCritieria());
	}
}
