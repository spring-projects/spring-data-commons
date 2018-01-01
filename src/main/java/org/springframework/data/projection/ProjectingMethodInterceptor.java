/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.projection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link MethodInterceptor} to delegate the invocation to a different {@link MethodInterceptor} but creating a
 * projecting proxy in case the returned value is not of the return type of the invoked method.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
@RequiredArgsConstructor
class ProjectingMethodInterceptor implements MethodInterceptor {

	private final @NonNull ProjectionFactory factory;
	private final @NonNull MethodInterceptor delegate;
	private final @NonNull ConversionService conversionService;

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") @Nonnull MethodInvocation invocation) throws Throwable {

		Object result = delegate.invoke(invocation);

		if (result == null) {
			return null;
		}

		TypeInformation<?> type = ClassTypeInformation.fromReturnTypeOf(invocation.getMethod());
		Class<?> rawType = type.getType();

		if (type.isCollectionLike() && !ClassUtils.isPrimitiveArray(rawType)) {
			return projectCollectionElements(asCollection(result), type);
		} else if (type.isMap()) {
			return projectMapValues((Map<?, ?>) result, type);
		} else if (conversionRequiredAndPossible(result, rawType)) {
			return conversionService.convert(result, rawType);
		} else {
			return getProjection(result, rawType);
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
		Collection<Object> result = CollectionFactory.createCollection(rawType.isArray() ? List.class : rawType,
				sources.size());

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
	private Object getProjection(Object result, Class<?> returnType) {
		return result == null || ClassUtils.isAssignable(returnType, result.getClass()) ? result
				: factory.createProjection(returnType, result);
	}

	/**
	 * Returns whether the source object needs to be converted to the given target type and whether we can convert it at
	 * all.
	 *
	 * @param source can be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return
	 */
	private boolean conversionRequiredAndPossible(Object source, Class<?> targetType) {

		if (source == null || targetType.isInstance(source)) {
			return false;
		}

		return conversionService.canConvert(source.getClass(), targetType);
	}

	/**
	 * Turns the given value into a {@link Collection}. Will turn an array into a collection an wrap all other values into
	 * a single-element collection.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static Collection<?> asCollection(Object source) {

		Assert.notNull(source, "Source object must not be null!");

		if (source instanceof Collection) {
			return (Collection<?>) source;
		} else if (source.getClass().isArray()) {
			return Arrays.asList(ObjectUtils.toObjectArray(source));
		} else {
			return Collections.singleton(source);
		}
	}
}
