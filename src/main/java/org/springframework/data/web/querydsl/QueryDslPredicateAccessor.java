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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Predicate;

/**
 * Accessor assembling {@link Predicate} out of {@link MutablePropertyValues} using provided
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
public class QueryDslPredicateAccessor {

	private final TypeInformation<?> typeInfo;
	private final QueryDslPredicateBuilder defaultPredicateBuilder;
	private Map<String, QueryDslPredicateBuilder> specificBuilders;

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo) {
		this(typeInfo, null, null);
	}

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo, QueryDslPredicateBuilder defaultPredicateBuilder) {
		this(typeInfo, defaultPredicateBuilder, null);
	}

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo, QueryDslPredicateSpecification predicateSpecification) {
		this(typeInfo, null, predicateSpecification);
	}

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo, QueryDslPredicateBuilder defaultPredicateBuilder,
			QueryDslPredicateSpecification predicateSpecification) {

		Assert.notNull(typeInfo, "TypeInfo must not be null!");

		this.typeInfo = typeInfo;
		this.defaultPredicateBuilder = defaultPredicateBuilder == null ? new GenericQueryDslPredicateBuilder(typeInfo)
				: defaultPredicateBuilder;
		this.specificBuilders = new LinkedHashMap<String, QueryDslPredicateBuilder>(0);

		if (predicateSpecification != null) {
			this.specificBuilders.putAll(predicateSpecification.getSpecs());
		}
	}

	/**
	 * @param values
	 * @return
	 */
	public Predicate getPredicate(MutablePropertyValues values) {

		if (values.isEmpty()) {
			return new BooleanBuilder();
		}

		BooleanBuilder builder = new BooleanBuilder();
		Class<?> type = typeInfo.getActualType().getType();

		for (PropertyValue propertyValue : values.getPropertyValueList()) {

			PropertyPath propertyPath = PropertyPath.from(propertyValue.getName(), type);
			String dotPath = propertyPath.toDotPath();

			QueryDslPredicateBuilder predicateBuilder = specificBuilders.containsKey(dotPath) ? specificBuilders.get(dotPath)
					: defaultPredicateBuilder;

			Object value = null;
			if (propertyValue.getValue() instanceof String[]) {
				if (propertyPath.isCollection()) {
					value = Arrays.asList((String[]) propertyValue.getValue());
				} else {
					value = ((String[]) propertyValue.getValue())[0];
				}
			}

			builder.and(predicateBuilder.buildPredicate(propertyPath, value));
		}
		return builder.getValue();
	}
}
