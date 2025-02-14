/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.projection;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import kotlin.reflect.KFunction;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.CollectionFactory;
import org.springframework.core.KotlinDetector;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.data.util.NullableWrapper;
import org.springframework.data.util.NullableWrapperConverters;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link MethodInterceptor} to delegate the invocation to a different {@link MethodInterceptor} but creating a
 * projecting proxy in case the returned value is not of the return type of the invoked method.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Johannes Englmeier
 * @author Yanming Zhou
 * @since 1.10
 */
class ProjectingMethodInterceptor implements MethodInterceptor {

	private final ProjectionFactory factory;
	private final MethodInterceptor delegate;
	private final ConversionService conversionService;

	ProjectingMethodInterceptor(ProjectionFactory factory, MethodInterceptor delegate,
			ConversionService conversionService) {

		this.factory = factory;
		this.delegate = delegate;
		this.conversionService = conversionService;
	}

	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") @NonNull MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		TypeInformation<?> type = TypeInformation.fromReturnTypeOf(method);
		TypeInformation<?> resultType = type;
		TypeInformation<?> typeToReturn = type;

		Object result = delegate.invoke(invocation);

		boolean applyWrapper = false;

		if (NullableWrapperConverters.supports(type.getType())
				&& ((result == null) || !NullableWrapperConverters.supports(result.getClass()))) {
			resultType = NullableWrapperConverters.unwrapActualType(typeToReturn);
			applyWrapper = true;
		}

		result = potentiallyConvertResult(resultType, result);

		if (applyWrapper) {
			return conversionService.convert(new NullableWrapper(result), typeToReturn.getType());
		}

		if (result == null) {
			KFunction<?> function = KotlinDetector.isKotlinType(method.getDeclaringClass()) ?
					KotlinReflectionUtils.findKotlinFunction(method) : null;
			if (function != null && !function.getReturnType().isMarkedNullable()) {
				throw new IllegalArgumentException("Kotlin function '%s' requires non-null return value".formatted(method.toString()));
			}
		}

		return result;
	}

	@Nullable
	protected Object potentiallyConvertResult(TypeInformation<?> type, @Nullable Object result) {

		if (result == null) {
			return null;
		}

		Class<?> targetType = type.getType();

		if (type.isCollectionLike() && !ClassUtils.isPrimitiveArray(targetType)) {
			return projectCollectionElements(asCollection(result), type);
		} else if (type.isMap()) {
			return projectMapValues((Map<?, ?>) result, type);
		} else if (ClassUtils.isAssignable(targetType, result.getClass())) {
			return result;
		} else if (conversionService.canConvert(result.getClass(), targetType)) {
			return conversionService.convert(result, targetType);
		} else if (targetType.isInterface()) {
			return getProjection(result, targetType);
		} else {
			throw new UnsupportedOperationException(
					String.format("Cannot project %s to %s; Target type is not an interface and no matching Converter found",
							ClassUtils.getDescriptiveType(result), ClassUtils.getQualifiedName(targetType)));
		}
	}

	/**
	 * Creates projections of the given {@link Collection}'s elements if necessary and returns a new collection containing
	 * the projection results.
	 *
	 * @param sources must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private Object projectCollectionElements(Collection<?> sources, TypeInformation<?> type) {

		Class<?> rawType = type.getType();
		TypeInformation<?> componentType = type.getComponentType();
		Collection<Object> result = CollectionFactory.createCollection(rawType.isArray() ? List.class : rawType,
				componentType != null ? componentType.getType() : null, sources.size());

		for (Object source : sources) {
			result.add(getProjection(source, type.getRequiredComponentType().getType()));
		}

		if (rawType.isArray()) {
			return result.toArray((Object[]) Array.newInstance(type.getRequiredComponentType().getType(), result.size()));
		}

		return result;
	}

	/**
	 * Creates projections of the given {@link Map}'s values if necessary and returns an new {@link Map} with the handled
	 * values.
	 *
	 * @param sources must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private Map<Object, Object> projectMapValues(Map<?, ?> sources, TypeInformation<?> type) {

		Map<Object, Object> result = CollectionFactory.createMap(type.getType(), sources.size());

		for (Entry<?, ?> source : sources.entrySet()) {
			result.put(source.getKey(), getProjection(source.getValue(), type.getRequiredMapValueType().getType()));
		}

		return result;
	}

	@Nullable
	private Object getProjection(@Nullable Object result, Class<?> returnType) {
		return (result == null) || ClassUtils.isAssignable(returnType, result.getClass()) ? result
				: factory.createProjection(returnType, result);
	}

	/**
	 * Turns the given value into a {@link Collection}. Will turn an array into a collection an wrap all other values into
	 * a single-element collection.
	 *
	 * @param source must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	private static Collection<?> asCollection(Object source) {

		Assert.notNull(source, "Source object must not be null");

		if (source instanceof Collection) {
			return (Collection<?>) source;
		} else if (source.getClass().isArray()) {
			return Arrays.asList(ObjectUtils.toObjectArray(source));
		} else {
			return Collections.singleton(source);
		}
	}
}
