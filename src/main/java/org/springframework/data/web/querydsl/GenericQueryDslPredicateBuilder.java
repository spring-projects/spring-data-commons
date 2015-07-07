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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathImpl;
import com.mysema.query.types.PathMetadataFactory;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.query.types.path.PathBuilderFactory;
import com.mysema.query.types.path.SimplePath;

/**
 * Generic {@link QueryDslPredicateBuilder} implementation creating {@link Predicate} based on elements root
 * {@link TypeInformation}.
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
public class GenericQueryDslPredicateBuilder implements QueryDslPredicateBuilder {

	private final ConversionService conversionService;
	private final PathBuilder<?> pathBuilder;

	/**
	 * Create new {@link GenericQueryDslPredicateBuilder} for given {@link TypeInformation}.
	 * 
	 * @param typeInfo must not be {@literal null}.
	 */
	public GenericQueryDslPredicateBuilder(TypeInformation<?> typeInfo) {

		Assert.notNull(typeInfo, "TypeInfo must not be null!");
		pathBuilder = new PathBuilderFactory().create(typeInfo.getActualType().getType());
		this.conversionService = new DefaultConversionService();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.querydsl.QueryDslPredicateBuilder#buildPredicate(org.springframework.data.mapping.PropertyPath, java.lang.Object)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Predicate buildPredicate(PropertyPath path, Object source) {

		SimplePath simplePath = pathBuilder.get(PathConverter.INSTANCE.convert(path));

		if (source == null) {
			return simplePath.isNull();
		}

		if (source instanceof Collection) {
			return simplePath.in(potentiallyConvertCollectionValues((Collection) source, path.getType()));
		}

		Object value = potentiallyConvertValue(source, path.getType());

		if (path.isCollection()) {
			return new ListPath(path.getType(), pathBuilder.getType(), simplePath.getMetadata()).contains(value);
		}

		return simplePath.eq(value);
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
	enum PathConverter implements Converter<PropertyPath, com.mysema.query.types.Path> {
		INSTANCE;

		@SuppressWarnings({ "unchecked" })
		@Override
		public Path convert(PropertyPath source) {
			return new PathImpl(source.getType(), PathMetadataFactory.forVariable(source.toDotPath()));
		}
	}

}
