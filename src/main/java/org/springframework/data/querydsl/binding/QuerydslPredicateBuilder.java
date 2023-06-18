/*
 * Copyright 2015-2023 the original author or authors.
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
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.PropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builder assembling {@link Predicate} out of {@link PropertyValues}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 1.11
 */
public class QuerydslPredicateBuilder implements QuerydslPredicateBuilderCustomizer {

	protected final ConversionService conversionService;
	protected final MultiValueBinding<Path<? extends Object>, Object> defaultBinding;
	protected final Map<PathInformation, Path<?>> paths;
	protected final EntityPathResolver resolver;

	/**
	 * Creates a new {@link QuerydslPredicateBuilder} for the given {@link ConversionService} and
	 * {@link EntityPathResolver}.
	 *
	 * @param conversionService must not be {@literal null}.
	 * @param resolver can be {@literal null}.
	 */
	public QuerydslPredicateBuilder(ConversionService conversionService, EntityPathResolver resolver) {

		Assert.notNull(conversionService, "ConversionService must not be null");

		this.defaultBinding = new QuerydslDefaultBinding();
		this.conversionService = conversionService;
		this.paths = new ConcurrentHashMap<>();
		this.resolver = resolver;
	}

	/**
	 * Creates a Querydsl {@link Predicate} for the given values, {@link QuerydslBindings} on the given
	 * {@link TypeInformation}.
	 *
	 * @param type the type to create a predicate for.
	 * @param values the values to bind.
	 * @param bindings the {@link QuerydslBindings} for the predicate.
	 * @return the {@link Predicate}.
	 */
	public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, ?> values, QuerydslBindings bindings) {

		Assert.notNull(bindings, "Context must not be null");

		BooleanBuilder builder = new BooleanBuilder();

		if (values.isEmpty()) {
			return getPredicate(builder);
		}

		for (var entry : values.entrySet()) {

			if (isSingleElementCollectionWithEmptyItem(entry.getValue())) {
				continue;
			}

			String path = entry.getKey();

			if (!bindings.isPathAvailable(path, type)) {
				continue;
			}

			PathInformation propertyPath = bindings.getPropertyPath(path, type);

			if (propertyPath == null) {
				continue;
			}

			Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath, conversionService);
			Optional<Predicate> predicate = invokeBinding(propertyPath, bindings, value, resolver, defaultBinding);

			predicate.ifPresent(builder::and);
		}

		return getPredicate(builder);
	}
}
