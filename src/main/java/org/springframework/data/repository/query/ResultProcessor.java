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
package org.springframework.data.repository.query;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.util.Assert;

/**
 * A {@link ResultProcessor} to expose metadata about query result element projection and eventually post prcessing raw
 * query results into projections and data transfer objects.
 * 
 * @author Oliver Gierke
 * @since 1.12
 */
public class ResultProcessor {

	private final QueryMethod method;
	private final ProjectingConverter converter;
	private final ProjectionFactory factory;

	private ReturnedType type;

	/**
	 * Creates a new {@link ResultProcessor} from the given {@link QueryMethod} and {@link ProjectionFactory}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	ResultProcessor(QueryMethod method, ProjectionFactory factory) {
		this(method, factory, method.getReturnedObjectType());
	}

	/**
	 * Creates a new {@link ResultProcessor} for the given {@link QueryMethod}, {@link ProjectionFactory} and type.
	 * 
	 * @param method must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 */
	private ResultProcessor(QueryMethod method, ProjectionFactory factory, Class<?> type) {

		Assert.notNull(method, "QueryMethod must not be null!");
		Assert.notNull(factory, "ProjectionFactory must not be null!");
		Assert.notNull(type, "Type must not be null!");

		this.method = method;
		this.type = ReturnedType.of(type, method.getDomainClass(), factory);
		this.converter = new ProjectingConverter(this.type, factory);
		this.factory = factory;
	}

	/**
	 * Returns a new {@link ResultProcessor} with a new projection type obtained from the given {@link ParameterAccessor}.
	 * 
	 * @param accessor can be {@literal null}.
	 * @return
	 */
	public ResultProcessor withDynamicProjection(ParameterAccessor accessor) {

		if (accessor == null) {
			return this;
		}

		Class<?> projectionType = accessor.getDynamicProjection();

		return projectionType == null ? this : new ResultProcessor(method, factory, projectionType);
	}

	/**
	 * Returns the {@link ReturnedType}.
	 * 
	 * @return
	 */
	public ReturnedType getReturnedType() {
		return type;
	}

	/**
	 * Post-processes the given query result.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	public <T> T processResult(Object source) {
		return processResult(source, NoOpConverter.INSTANCE);
	}

	/**
	 * Post-processes the given query result using the given preparing {@link Converter} to potentially prepare collection
	 * elements.
	 * 
	 * @param source can be {@literal null}.
	 * @param preparingConverter must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T processResult(Object source, Converter<Object, Object> preparingConverter) {

		if (type.isInstance(source) || !type.isProjecting()) {
			return (T) source;
		}

		Assert.notNull(preparingConverter, "Preparing converter must not be null!");

		ChainingConverter converter = ChainingConverter.of(type.getReturnedType(), preparingConverter).and(this.converter);

		if (source instanceof Page && method.isPageQuery()) {
			return (T) ((Page<?>) source).map(converter);
		}

		if (source instanceof Collection && method.isCollectionQuery()) {

			Collection<?> collection = (Collection<?>) source;
			Collection<Object> target = CollectionFactory.createCollection(collection.getClass(), collection.size());

			for (Object columns : collection) {
				target.add(type.isInstance(columns) ? columns : converter.convert(columns));
			}

			return (T) target;
		}

		return (T) converter.convert(source);
	}

	@RequiredArgsConstructor(staticName = "of")
	private static class ChainingConverter implements Converter<Object, Object> {

		private final @NonNull Class<?> targetType;
		private final @NonNull Converter<Object, Object> delegate;

		/**
		 * Returns a new {@link ChainingConverter} that hands the elements resulting from the current conversion to the
		 * given {@link Converter}.
		 * 
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public ChainingConverter and(final Converter<Object, Object> converter) {

			Assert.notNull(converter, "Converter must not be null!");

			return new ChainingConverter(targetType, new Converter<Object, Object>() {

				@Override
				public Object convert(Object source) {

					Object intermediate = ChainingConverter.this.convert(source);
					return targetType.isInstance(intermediate) ? intermediate : converter.convert(intermediate);
				}
			});
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {
			return delegate.convert(source);
		}
	}

	/**
	 * A simple {@link Converter} that will return the source value as is.
	 *
	 * @author Oliver Gierke
	 * @since 1.12
	 */
	private static enum NoOpConverter implements Converter<Object, Object> {

		INSTANCE;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {
			return source;
		}
	}

	@RequiredArgsConstructor
	private static class ProjectingConverter implements Converter<Object, Object> {

		private final @NonNull ReturnedType type;
		private final @NonNull ProjectionFactory factory;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {
			return factory.createProjection(type.getReturnedType(), getProjectionTarget(source));
		}

		private Object getProjectionTarget(Object source) {

			if (source != null && source.getClass().isArray()) {
				source = Arrays.asList((Object[]) source);
			}

			if (source instanceof Collection) {
				return toMap((Collection<?>) source, type.getInputProperties());
			}

			return source;
		}

		private static Map<String, Object> toMap(Collection<?> values, List<String> names) {

			int i = 0;
			Map<String, Object> result = new HashMap<String, Object>(values.size());

			for (Object element : values) {
				result.put(names.get(i++), element);
			}

			return result;
		}
	}
}
