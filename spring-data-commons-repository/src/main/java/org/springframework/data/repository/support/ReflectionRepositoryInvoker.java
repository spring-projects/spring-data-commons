/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.repository.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Base {@link RepositoryInvoker} using reflection to invoke methods on Spring Data Repositories.
 *
 * @author Oliver Gierke
 * @author Alessandro Nistico
 * @author Johannes Englmeier
 * @since 1.10
 */
class ReflectionRepositoryInvoker implements RepositoryInvoker {

	private static final AnnotationAttribute PARAM_ANNOTATION = new AnnotationAttribute(Param.class);
	private static final String NAME_NOT_FOUND = "Unable to detect parameter names for query method %s; Use @Param or compile with -parameters on JDK 8";

	private final Object repository;
	private final CrudMethods methods;
	private final TypeDescriptor idTypeDescriptor;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link ReflectionRepositoryInvoker} for the given repository, {@link RepositoryMetadata} and
	 * {@link ConversionService}.
	 *
	 * @param repository must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public ReflectionRepositoryInvoker(Object repository, RepositoryMetadata metadata,
			ConversionService conversionService) {

		Assert.notNull(repository, "Repository must not be null");
		Assert.notNull(metadata, "RepositoryMetadata must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null");

		this.repository = repository;
		this.methods = metadata.getCrudMethods();
		TypeInformation<?> idType = metadata.getIdTypeInformation();
		this.idTypeDescriptor = idType.toTypeDescriptor();
		this.conversionService = conversionService;
	}

	@Override
	public boolean hasFindAllMethod() {
		return methods.hasFindAllMethod();
	}

	@Override
	public Iterable<Object> invokeFindAll(Sort sort) {
		return invokeFindAllReflectively(sort);
	}

	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return invokeFindAllReflectively(pageable);
	}

	@Override
	public boolean hasSaveMethod() {
		return methods.hasSaveMethod();
	}

	@Override
	public <T> T invokeSave(T object) {

		Method method = methods.getSaveMethod()//
				.orElseThrow(() -> new IllegalStateException("Repository doesn't have a save-method declared"));

		return invokeForNonNullResult(method, object);
	}

	@Override
	public boolean hasFindOneMethod() {
		return methods.hasFindOneMethod();
	}

	@Override
	public <T> Optional<T> invokeFindById(Object id) {

		Method method = methods.getFindOneMethod()//
				.orElseThrow(() -> new IllegalStateException("Repository doesn't have a find-one-method declared"));

		return returnAsOptional(invoke(method, convertId(id)));
	}

	@Override
	public boolean hasDeleteMethod() {
		return methods.hasDelete();
	}

	@Override
	public void invokeDeleteById(Object id) {

		Assert.notNull(id, "Identifier must not be null");

		Method method = methods.getDeleteMethod()
				.orElseThrow(() -> new IllegalStateException("Repository doesn't have a delete-method declared"));

		if (method.getName().endsWith("ById")) {
			invoke(method, convertId(id));
		} else {
			invoke(method, this.<Object> invokeFindById(id).orElse(null));
		}
	}

	@Override
	public Optional<Object> invokeQueryMethod(Method method, MultiValueMap<String, ?> parameters, Pageable pageable,
			Sort sort) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(parameters, "Parameters must not be null");
		Assert.notNull(pageable, "Pageable must not be null");
		Assert.notNull(sort, "Sort must not be null");

		ReflectionUtils.makeAccessible(method);

		return returnAsOptional(invoke(method, prepareParameters(method, parameters, pageable, sort)));
	}

	private Object[] prepareParameters(Method method, MultiValueMap<String, ?> rawParameters, Pageable pageable,
			Sort sort) {

		List<MethodParameter> parameters = new MethodParameters(method, Optional.of(PARAM_ANNOTATION)).getParameters();

		if (parameters.isEmpty()) {
			return new Object[0];
		}

		Object[] result = new Object[parameters.size()];
		Sort sortToUse = pageable.getSortOr(sort);

		for (int i = 0; i < result.length; i++) {

			MethodParameter param = parameters.get(i);
			Class<?> targetType = param.getParameterType();

			if (Pageable.class.isAssignableFrom(targetType)) {
				result[i] = pageable;
			} else if (Sort.class.isAssignableFrom(targetType)) {
				result[i] = sortToUse;
			} else {

				String parameterName = param.getParameterName();

				if (!StringUtils.hasText(parameterName)) {
					throw new IllegalArgumentException(String.format(NAME_NOT_FOUND, ClassUtils.getQualifiedMethodName(method)));
				}

				Object value = unwrapSingleElement(rawParameters.get(parameterName));

				result[i] = targetType.isInstance(value) ? value : convert(value, param);
			}
		}

		return result;
	}

	@Contract("null, _ -> null")
	private @Nullable Object convert(@Nullable Object value, MethodParameter parameter) {

		if (value == null) {
			return value;
		}

		try {
			return conversionService.convert(value, TypeDescriptor.forObject(value), new TypeDescriptor(parameter));
		} catch (ConversionException o_O) {
			throw new QueryMethodParameterConversionException(value, parameter, o_O);
		}
	}

	/**
	 * Invokes the given method with the given arguments on the backing repository.
	 *
	 * @param method
	 * @param arguments
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> @Nullable T invoke(Method method, @Nullable Object... arguments) {
		return (T) ReflectionUtils.invokeMethod(method, repository, arguments);
	}

	private <T> T invokeForNonNullResult(Method method, @Nullable Object... arguments) {

		T result = invoke(method, arguments);

		if (result == null) {
			throw new IllegalStateException(String.format("Invocation of method %s(%s) on %s unexpectedly returned null",
					method, Arrays.toString(arguments), repository));
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> Optional<T> returnAsOptional(@Nullable Object source) {

		return (Optional<T>) (source instanceof Optional //
				? source //
				: Optional.ofNullable(QueryExecutionConverters.unwrap(source)));
	}

	/**
	 * Converts the given id into the id type of the backing repository.
	 *
	 * @param id must not be {@literal null}.
	 * @return
	 */
	protected Object convertId(Object id) {

		Assert.notNull(id, "Id must not be null");
		TypeDescriptor idDescriptor = TypeDescriptor.forObject(id);

		if (idDescriptor.isAssignableTo(idTypeDescriptor)) {
			return id;
		}

		Object result = conversionService.convert(id, idDescriptor, idTypeDescriptor);

		if (result == null) {
			throw new IllegalStateException(
					String.format("Identifier conversion of %s to %s unexpectedly returned null", id,
							idTypeDescriptor.getType()));
		}

		return result;
	}

	protected Iterable<Object> invokeFindAllReflectively(Pageable pageable) {

		Method method = methods.getFindAllMethod()
				.orElseThrow(() -> new IllegalStateException("Repository doesn't have a find-all-method declared"));

		if (method.getParameterCount() == 0) {
			return invokeForNonNullResult(method);
		}

		Class<?>[] types = method.getParameterTypes();

		if (Pageable.class.isAssignableFrom(types[0])) {
			return invokeForNonNullResult(method, pageable);
		}

		return invokeFindAll(pageable.getSort());
	}

	protected Iterable<Object> invokeFindAllReflectively(Sort sort) {

		Method method = methods.getFindAllMethod()
				.orElseThrow(() -> new IllegalStateException("Repository doesn't have a find-all-method declared"));

		if (method.getParameterCount() == 0) {
			return invokeForNonNullResult(method);
		}

		return invokeForNonNullResult(method, sort);
	}

	/**
	 * Unwraps the first item if the given source has exactly one element.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	private static @Nullable Object unwrapSingleElement(@Nullable List<? extends Object> source) {
		return source == null ? null : source.size() == 1 ? source.get(0) : source;
	}
}
