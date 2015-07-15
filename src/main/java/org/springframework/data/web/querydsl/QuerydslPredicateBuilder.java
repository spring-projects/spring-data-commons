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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.PropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;

/**
 * Builder assembling {@link Predicate} out of {@link PropertyValues}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
class QuerydslPredicateBuilder {

	private final ConversionService conversionService;
	private final MultiValueBinding<?, ?> defaultBinding;
	private final Map<PropertyPath, Path<?>> paths;

	public QuerydslPredicateBuilder(ConversionService conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.defaultBinding = new QuerydslDefaultBinding();
		this.conversionService = conversionService;
		this.paths = new HashMap<PropertyPath, Path<?>>();
	}

	/**
	 * @param values
	 * @param context
	 * @return
	 */

	public Predicate getPredicate(MultiValueMap<String, String> values, QuerydslBindings bindings,
			TypeInformation<?> type) {

		Assert.notNull(bindings, "Context must not be null!");

		BooleanBuilder builder = new BooleanBuilder();

		if (values.isEmpty()) {
			return builder.getValue();
		}

		for (Entry<String, List<String>> entry : values.entrySet()) {

			if (isSingleElementCollectionWithoutText(entry.getValue())) {
				continue;
			}

			try {

				PropertyPath propertyPath = PropertyPath.from(entry.getKey(), type);

				if (bindings.isPathVisible(propertyPath)) {

					Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath);

					Predicate binding = invokeBinding(propertyPath, bindings, value);

					if (binding != null) {
						builder.and(binding);
					}
				}
			} catch (PropertyReferenceException o_O) {
				// not a property of the domain object, continue
			}
		}

		return builder.getValue();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Predicate invokeBinding(PropertyPath dotPath, QuerydslBindings bindings, Collection<Object> value) {

		Path<?> path = getPath(dotPath, bindings);

		MultiValueBinding binding = bindings.getBindingForPath(dotPath);
		binding = binding == null ? defaultBinding : binding;

		return binding.bind(path, value);
	}

	private Path<?> getPath(PropertyPath path, QuerydslBindings bindings) {

		Path<?> resolvedPath = bindings.getExistingPath(path);

		if (resolvedPath != null) {
			return resolvedPath;
		}

		resolvedPath = paths.get(resolvedPath);

		if (resolvedPath != null) {
			return resolvedPath;
		}

		resolvedPath = reifyPath(path, null);
		paths.put(path, resolvedPath);

		return resolvedPath;
	}

	private static Path<?> reifyPath(PropertyPath path, EntityPath<?> base) {

		EntityPath<?> entityPath = base != null ? base
				: SimpleEntityPathResolver.INSTANCE.createPath(path.getOwningType().getType());

		Field field = ReflectionUtils.findField(entityPath.getClass(), path.getSegment());
		Object value = ReflectionUtils.getField(field, entityPath);

		if (path.hasNext() && value instanceof EntityPath) {
			return reifyPath(path.next(), (EntityPath<?>) value);
		}

		return (Path<?>) value;
	}

	private Collection<Object> convertToPropertyPathSpecificType(List<String> source, PropertyPath path) {

		Class<?> targetType = path.getLeafProperty().getType();

		if (isSingleElementCollectionWithoutText(source)) {
			return Collections.emptyList();
		}

		Collection<Object> target = new ArrayList<Object>(source.size());

		for (String value : source) {
			target.add(potentiallyConvertValue(value, targetType));
		}

		return target;
	}

	private Object potentiallyConvertValue(Object source, Class<?> targetType) {
		return conversionService.canConvert(source.getClass(), targetType) ? conversionService.convert(source, targetType)
				: source;
	}

	/**
	 * Returns whether the given collection has exactly one element that doesn't contain any text. This is basically an
	 * indicator that a request parameter has been submitted but no value for it.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static boolean isSingleElementCollectionWithoutText(List<String> source) {
		return source.size() == 1 && !StringUtils.hasText(source.get(0));
	}
}
