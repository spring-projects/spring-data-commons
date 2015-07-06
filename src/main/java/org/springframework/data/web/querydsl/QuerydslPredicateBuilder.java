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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.PathMetadataFactory;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.CollectionPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.query.types.path.PathBuilderFactory;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

/**
 * Builder assembling {@link Predicate} out of {@link PropertyValues}.
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
class QuerydslPredicateBuilder {

	private final QuerydslBinding<?> defaultBinding;

	public QuerydslPredicateBuilder() {
		this.defaultBinding = new QuerydslDefaultBinding();
	}

	/**
	 * @param values
	 * @param context
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate getPredicate(PropertyValues values, QuerydslBindingContext context) {

		Assert.notNull(context, "Context must not be null!");

		if (values.isEmpty()) {
			return new BooleanBuilder();
		}

		BooleanBuilder builder = new BooleanBuilder();

		for (PropertyValue propertyValue : values.getPropertyValues()) {

			PropertyPath propertyPath = PropertyPath.from(propertyValue.getName(), context.getTargetType());

			if (context.isPathVisible(propertyPath)) {

				Object value = convertToPropertyPathSpecificType(propertyValue.getValue(), propertyPath, context);
				QuerydslBinding binding = getPredicateBuilderForPath(propertyPath, context);

				builder.and(binding.bind(getPath(propertyPath, context), value));
			}
		}

		return builder.getValue();
	}

	private QuerydslBinding<? extends Path<?>> getPredicateBuilderForPath(PropertyPath dotPath,
			QuerydslBindingContext context) {

		QuerydslBinding<? extends Path<?>> binding = context.getBindingForPath(dotPath);
		return binding == null ? defaultBinding : binding;
	}

	private Path<?> getPath(PropertyPath propertyPath, QuerydslBindingContext context) {

		Path<?> path = context.getPathForPropertyPath(propertyPath);
		if (path != null) {
			return path;
		}

		return new PropertyPathPathBuilder(context.getTargetType()).forProperty(propertyPath);
	}

	private Object convertToPropertyPathSpecificType(Object source, PropertyPath path, QuerydslBindingContext context) {

		Object value = source;

		if (ObjectUtils.isArray(value)) {

			List<?> list = CollectionUtils.arrayToList(value);
			if (!list.isEmpty() && list.size() == 1) {
				value = list.get(0);
			} else {
				value = list;
			}
		}

		if (value instanceof Collection) {
			return potentiallyConvertCollectionValues((Collection<?>) value, path.getType(), context);
		}

		return potentiallyConvertValue(value, path.getType(), context);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<?> potentiallyConvertCollectionValues(Collection<?> source, Class<?> targetType,
			QuerydslBindingContext context) {

		Collection target = new ArrayList(source.size());
		for (Object value : source) {
			target.add(potentiallyConvertValue(value, targetType, context));
		}

		return target;
	}

	private Object potentiallyConvertValue(Object source, Class<?> targetType, QuerydslBindingContext context) {

		return context.getConversionService().canConvert(source.getClass(), targetType)
				? context.getConversionService().convert(source, targetType) : source;
	}

	/**
	 * {@link PropertyPathPathBuilder} creates a typed {@link Path} based on type information contained in
	 * {@link PropertyPath}.
	 * 
	 * @author Christoph Strobl
	 */
	@SuppressWarnings("rawtypes")
	static class PropertyPathPathBuilder {

		final PathBuilder<?> pathBuilder;

		public PropertyPathPathBuilder(Class<?> type) {
			pathBuilder = new PathBuilderFactory().create(type);
		}

		@SuppressWarnings({ "unchecked" })
		public Path<?> forProperty(PropertyPath source) {

			PathMetadata<?> metadata = PathMetadataFactory.forVariable(source.toDotPath());

			Path<?> path = null;
			if (source.isCollection()) {
				path = pathBuilder.get(new CollectionPath(Collection.class, source.getType(), metadata));
			} else if (ClassUtils.isAssignable(String.class, source.getType())) {
				path = pathBuilder.get(new StringPath(metadata));
			} else if (ClassUtils.isAssignable(Number.class, source.getType())) {
				path = pathBuilder.get(new NumberPath(source.getType(), metadata));
			} else {
				path = pathBuilder.get(new SimplePath(source.getType(), metadata));
			}

			return path;
		}
	}

}
