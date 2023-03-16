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
package org.springframework.data.repository.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Window;
import org.springframework.data.domain.Slice;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link ResultProcessor} to expose metadata about query result element projection and eventually post processing raw
 * query results into projections and data transfer objects.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 1.12
 */
public class ResultProcessor {

	private final QueryMethod method;
	private final ProjectingConverter converter;
	private final ProjectionFactory factory;
	private final ReturnedType type;

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

		Assert.notNull(method, "QueryMethod must not be null");
		Assert.notNull(factory, "ProjectionFactory must not be null");
		Assert.notNull(type, "Type must not be null");

		this.method = method;
		this.type = ReturnedType.of(type, method.getDomainClass(), factory);
		this.converter = new ProjectingConverter(this.type, factory);
		this.factory = factory;
	}

	private ResultProcessor(QueryMethod method, ProjectingConverter converter, ProjectionFactory factory,
			ReturnedType type) {
		this.method = method;
		this.converter = converter;
		this.factory = factory;
		this.type = type;
	}

	/**
	 * Returns a new {@link ResultProcessor} with a new projection type obtained from the given {@link ParameterAccessor}.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	public ResultProcessor withDynamicProjection(ParameterAccessor accessor) {

		Assert.notNull(accessor, "Parameter accessor must not be null");

		Class<?> projection = accessor.findDynamicProjection();

		return projection == null //
				? this //
				: withType(projection);
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
	@Nullable
	public <T> T processResult(@Nullable Object source) {
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
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T processResult(@Nullable Object source, Converter<Object, Object> preparingConverter) {

		if (source == null || type.isInstance(source) || !type.isProjecting()) {
			return (T) source;
		}

		Assert.notNull(preparingConverter, "Preparing converter must not be null");

		ChainingConverter converter = ChainingConverter.of(type.getReturnedType(), preparingConverter).and(this.converter);

		if (source instanceof Window<?> && method.isScrollQuery()) {
			return (T) ((Window<?>) source).map(converter::convert);
		}

		if (source instanceof Slice && (method.isPageQuery() || method.isSliceQuery())) {
			return (T) ((Slice<?>) source).map(converter::convert);
		}

		if (source instanceof Collection<?> collection && method.isCollectionQuery()) {

			Collection<Object> target = createCollectionFor(collection);

			for (Object columns : collection) {
				target.add(type.isInstance(columns) ? columns : converter.convert(columns));
			}

			return (T) target;
		}

		if (source instanceof Stream && method.isStreamQuery()) {
			return (T) ((Stream<Object>) source).map(t -> type.isInstance(t) ? t : converter.convert(t));
		}

		if (ReactiveWrapperConverters.supports(source.getClass())) {
			return (T) ReactiveWrapperConverters.map(source, it -> processResult(it, preparingConverter));
		}

		return (T) converter.convert(source);
	}

	private ResultProcessor withType(Class<?> type) {

		ReturnedType returnedType = ReturnedType.of(type, method.getDomainClass(), factory);
		return new ResultProcessor(method, converter.withType(returnedType), factory, returnedType);
	}

	/**
	 * Creates a new {@link Collection} for the given source. Will try to create an instance of the source collection's
	 * type first falling back to creating an approximate collection if the former fails.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static Collection<Object> createCollectionFor(Collection<?> source) {

		try {
			return CollectionFactory.createCollection(source.getClass(), source.size());
		} catch (RuntimeException o_O) {
			return CollectionFactory.createApproximateCollection(source, source.size());
		}
	}

	private static class ChainingConverter implements Converter<Object, Object> {

		private final Class<?> targetType;
		private final Converter<Object, Object> delegate;

		private ChainingConverter(Class<?> targetType, Converter<Object, Object> delegate) {
			this.targetType = targetType;
			this.delegate = delegate;
		}

		public static ChainingConverter of(Class<?> targetType, Converter<Object, Object> delegate) {
			return new ChainingConverter(targetType, delegate);
		}

		/**
		 * Returns a new {@link ChainingConverter} that hands the elements resulting from the current conversion to the
		 * given {@link Converter}.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		public ChainingConverter and(final Converter<Object, Object> converter) {

			Assert.notNull(converter, "Converter must not be null");

			return new ChainingConverter(targetType, source -> {

				if (source == null || targetType.isInstance(source)) {
					return source;
				}

				Object intermediate = ChainingConverter.this.convert(source);

				return intermediate == null || targetType.isInstance(intermediate) ? intermediate
						: converter.convert(intermediate);
			});
		}

		@Nullable
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

		@Override
		public Object convert(Object source) {
			return source;
		}
	}

	private static class ProjectingConverter implements Converter<Object, Object> {

		private final ReturnedType type;
		private final ProjectionFactory factory;
		private final ConversionService conversionService;

		/**
		 * Creates a new {@link ProjectingConverter} for the given {@link ReturnedType} and {@link ProjectionFactory}.
		 *
		 * @param type must not be {@literal null}.
		 * @param factory must not be {@literal null}.
		 */
		ProjectingConverter(ReturnedType type, ProjectionFactory factory) {
			this(type, factory, DefaultConversionService.getSharedInstance());
		}

		public ProjectingConverter(ReturnedType type, ProjectionFactory factory, ConversionService conversionService) {
			this.type = type;
			this.factory = factory;
			this.conversionService = conversionService;
		}

		/**
		 * Creates a new {@link ProjectingConverter} for the given {@link ReturnedType}.
		 *
		 * @param type must not be {@literal null}.
		 * @return
		 */
		ProjectingConverter withType(ReturnedType type) {

			Assert.notNull(type, "ReturnedType must not be null");

			return new ProjectingConverter(type, factory, conversionService);
		}

		@Nullable
		@Override
		public Object convert(Object source) {

			Class<?> targetType = type.getReturnedType();

			if (targetType.isInterface()) {
				return factory.createProjection(targetType, getProjectionTarget(source));
			}

			return conversionService.convert(source, targetType);
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
			Map<String, Object> result = new HashMap<>(values.size());

			for (Object element : values) {
				result.put(names.get(i++), element);
			}

			return result;
		}
	}
}
