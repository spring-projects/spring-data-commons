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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;
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
 * Accessor assembling {@link Predicate} out of {@link MutablePropertyValues} using provided
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
public class QueryDslPredicateAccessor {

	private final TypeInformation<?> typeInfo;
	private final QueryDslPredicateBuilder<?> defaultPredicateBuilder;
	private final ConversionService conversionService;
	private final QueryDslPredicateSpecification predicateSpec;

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo) {
		this(typeInfo, null, null);
	}

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo, QueryDslPredicateBuilder<?> defaultPredicateBuilder) {
		this(typeInfo, defaultPredicateBuilder, null);
	}

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo, QueryDslPredicateSpecification predicateSpecification) {
		this(typeInfo, null, predicateSpecification);
	}

	public QueryDslPredicateAccessor(TypeInformation<?> typeInfo, QueryDslPredicateBuilder<?> defaultPredicateBuilder,
			QueryDslPredicateSpecification predicateSpecification) {

		Assert.notNull(typeInfo, "TypeInfo must not be null!");

		this.typeInfo = typeInfo;
		this.defaultPredicateBuilder = defaultPredicateBuilder == null ? new GenericQueryDslPredicateBuilder()
				: defaultPredicateBuilder;
		this.conversionService = new DefaultConversionService();
		this.predicateSpec = predicateSpecification;
	}

	/**
	 * @param values
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate getPredicate(MutablePropertyValues values) {

		if (values.isEmpty()) {
			return new BooleanBuilder();
		}

		BooleanBuilder builder = new BooleanBuilder();

		for (PropertyValue propertyValue : values.getPropertyValueList()) {

			PropertyPath propertyPath = PropertyPath.from(propertyValue.getName(), typeInfo.getActualType().getType());
			String dotPath = propertyPath.toDotPath();

			if (predicateSpec.isPathVisible(propertyPath)) {
				Object value = convertToPropertyPathSpecificType(propertyValue.getValue(), propertyPath);

				QueryDslPredicateBuilder predicateBuilder = getPredicateBuilderForPath(dotPath);
				builder.and(predicateBuilder.buildPredicate(getPath(propertyPath), value));
			}
		}

		return builder.getValue();
	}

	private QueryDslPredicateBuilder<? extends Path<?>> getPredicateBuilderForPath(String dotPath) {

		if (predicateSpec == null) {
			return defaultPredicateBuilder;
		}

		return predicateSpec.hasSpecificsForPath(dotPath) ? predicateSpec.getBuilderForPath(dotPath)
				: defaultPredicateBuilder;
	}

	private Path<?> getPath(PropertyPath propertyPath) {

		if (predicateSpec.getPathForStringPath(propertyPath.toDotPath()) != null) {
			return predicateSpec.getPathForStringPath(propertyPath.toDotPath());
		}

		return new PropertyPathPathBuilder(typeInfo.getActualType().getType()).forProperty(propertyPath);
	}

	private Object convertToPropertyPathSpecificType(Object source, PropertyPath path) {

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
			return potentiallyConvertCollectionValues((Collection<?>) value, path.getType());
		}

		return potentiallyConvertValue(value, path.getType());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<?> potentiallyConvertCollectionValues(Collection<?> source, Class<?> targetType) {

		Collection target = new ArrayList(source.size());
		for (Object value : source) {
			target.add(potentiallyConvertValue(value, targetType));
		}

		return target;
	}

	private Object potentiallyConvertValue(Object source, Class<?> targetType) {

		return conversionService.canConvert(source.getClass(), targetType) ? conversionService.convert(source, targetType)
				: source;
	}

	/**
	 * {@link Converter} implementation creating a typed {@link Path} based on type information contained in
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
