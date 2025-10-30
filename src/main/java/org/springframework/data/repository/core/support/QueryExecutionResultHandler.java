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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.CollectionFactory;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.core.NullableWrapper;
import org.springframework.data.core.ReactiveWrappers;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.Streamable;

/**
 * Simple domain service to convert query results into a dedicated type.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
class QueryExecutionResultHandler {

	private static final TypeDescriptor WRAPPER_TYPE = TypeDescriptor.valueOf(NullableWrapper.class);

	private static final @Nullable Class<?> FLOW_TYPE = loadIfPresent("kotlinx.coroutines.flow.Flow");

	private final GenericConversionService conversionService;

	private final Object mutex = new Object();

	// concurrent access guarded by mutex.
	private Map<MethodParameter, ReturnTypeDescriptor> descriptorCache = Collections.emptyMap();

	/**
	 * Creates a new {@link QueryExecutionResultHandler}.
	 */
	QueryExecutionResultHandler(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@SuppressWarnings("unchecked")
	public @Nullable static <T> Class<T> loadIfPresent(String type) {

		try {
			return (Class<T>) org.springframework.util.ClassUtils.forName(type,
					QueryExecutionResultHandler.class.getClassLoader());
		} catch (ClassNotFoundException | LinkageError e) {
			return null;
		}
	}

	/**
	 * Post-processes the given result of a query invocation to match the return type of the given method.
	 *
	 * @param result can be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	@Nullable
	Object postProcessInvocationResult(@Nullable Object result, MethodParameter method) {

		if (!processingRequired(result, method)) {
			return result;
		}

		ReturnTypeDescriptor descriptor = getOrCreateReturnTypeDescriptor(method);

		return postProcessInvocationResult(result, 0, descriptor);
	}

	private ReturnTypeDescriptor getOrCreateReturnTypeDescriptor(MethodParameter method) {

		Map<MethodParameter, ReturnTypeDescriptor> descriptorCache = this.descriptorCache;
		ReturnTypeDescriptor descriptor = descriptorCache.get(method);

		if (descriptor == null) {

			descriptor = ReturnTypeDescriptor.of(method);

			Map<MethodParameter, ReturnTypeDescriptor> updatedDescriptorCache;

			if (descriptorCache.isEmpty()) {
				updatedDescriptorCache = Collections.singletonMap(method, descriptor);
			} else {
				updatedDescriptorCache = new HashMap<>(descriptorCache.size() + 1, 1);
				updatedDescriptorCache.putAll(descriptorCache);
				updatedDescriptorCache.put(method, descriptor);
			}

			synchronized (mutex) {
				this.descriptorCache = updatedDescriptorCache;
			}
		}

		return descriptor;
	}

	/**
	 * Post-processes the given result of a query invocation to the given type.
	 *
	 * @param result can be {@literal null}.
	 * @param nestingLevel
	 * @param descriptor must not be {@literal null}.
	 * @return
	 */
	@Nullable
	Object postProcessInvocationResult(@Nullable Object result, int nestingLevel, ReturnTypeDescriptor descriptor) {

		TypeDescriptor returnTypeDescriptor = descriptor.getReturnTypeDescriptor(nestingLevel);

		if (returnTypeDescriptor == null) {
			return result;
		}

		Class<?> expectedReturnType = returnTypeDescriptor.getType();

		result = unwrapOptional(result);

		if (QueryExecutionConverters.supports(expectedReturnType)
				|| ReactiveWrapperConverters.supports(expectedReturnType)) {

			// For a wrapper type, try nested resolution first
			result = postProcessInvocationResult(result, nestingLevel + 1, descriptor);

			if (conversionRequired(WRAPPER_TYPE, returnTypeDescriptor)) {
				return conversionService.convert(new NullableWrapper(result), returnTypeDescriptor);
			}

			if (result != null) {

				TypeDescriptor source = TypeDescriptor.valueOf(result.getClass());

				if (conversionRequired(source, returnTypeDescriptor)) {
					return conversionService.convert(result, returnTypeDescriptor);
				}
			}
		}

		if (result != null) {

			if (ReactiveWrapperConverters.supports(expectedReturnType)) {
				return ReactiveWrapperConverters.toWrapper(result, expectedReturnType);
			}

			if (result instanceof Collection<?>) {

				TypeDescriptor elementDescriptor = descriptor.getReturnTypeDescriptor(nestingLevel + 1);
				boolean requiresConversion = requiresConversion((Collection<?>) result, expectedReturnType, elementDescriptor);

				if (!requiresConversion) {
					return result;
				}
			}

			TypeDescriptor resultDescriptor = TypeDescriptor.forObject(result);
			return conversionService.canConvert(resultDescriptor, returnTypeDescriptor)
					? conversionService.convert(result, returnTypeDescriptor)
					: result;
		}

		return Map.class.equals(expectedReturnType) //
				? CollectionFactory.createMap(expectedReturnType, 0) //
				: null;

	}

	private boolean requiresConversion(Collection<?> collection, Class<?> expectedReturnType,
			@Nullable TypeDescriptor elementDescriptor) {

		if (Streamable.class.isAssignableFrom(expectedReturnType) || !expectedReturnType.isInstance(collection)) {
			return true;
		}

		if (elementDescriptor == null || !Iterable.class.isAssignableFrom(expectedReturnType)) {
			return false;
		}

		Class<?> type = elementDescriptor.getType();

		for (Object o : collection) {

			if (!type.isInstance(o)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns whether the configured {@link ConversionService} can convert between the given {@link TypeDescriptor}s and
	 * the conversion will not be a no-op.
	 *
	 * @param source
	 * @param target
	 * @return
	 */
	private boolean conversionRequired(TypeDescriptor source, TypeDescriptor target) {

		return conversionService.canConvert(source, target) //
				&& !conversionService.canBypassConvert(source, target);
	}

	/**
	 * Unwraps the given value if it's a JDK 8 {@link Optional}.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static @Nullable Object unwrapOptional(@Nullable Object source) {

		if (source == null) {
			return null;
		}

		return source instanceof Optional<?> op//
				? op.orElse(null) //
				: source;
	}

	/**
	 * Returns whether we have to process the given source object in the first place.
	 *
	 * @param source can be {@literal null}.
	 * @param methodParameter must not be {@literal null}.
	 * @return
	 */
	private static boolean processingRequired(@Nullable Object source, MethodParameter methodParameter) {

		Class<?> targetType = methodParameter.getParameterType();

		Method method = methodParameter.getMethod();
		if (source != null && method != null && ReactiveWrappers.KOTLIN_COROUTINES_PRESENT
				&& KotlinDetector.isSuspendingFunction(method)) {

			// Spring's AOP invoker handles Publisher to Flow conversion, so we have to exempt these from post-processing.
			if (FLOW_TYPE != null && FLOW_TYPE.isAssignableFrom(targetType)) {
				return false;
			}
		}

		return !targetType.isInstance(source) //
				|| source == null //
				|| Collection.class.isInstance(source);
	}

	/**
	 * Value object capturing {@link MethodParameter} and {@link TypeDescriptor}s for top and nested levels.
	 */
	static class ReturnTypeDescriptor {

		private final MethodParameter methodParameter;
		private final @Nullable TypeDescriptor typeDescriptor;
		private final @Nullable TypeDescriptor nestedTypeDescriptor;

		private ReturnTypeDescriptor(MethodParameter methodParameter) {
			this.methodParameter = methodParameter;
			this.typeDescriptor = TypeDescriptor.nested(this.methodParameter, 0);
			this.nestedTypeDescriptor = TypeDescriptor.nested(this.methodParameter, 1);
		}

		/**
		 * Create a {@link ReturnTypeDescriptor} from a {@link MethodParameter}.
		 *
		 * @param method
		 * @return
		 */
		public static ReturnTypeDescriptor of(MethodParameter method) {
			return new ReturnTypeDescriptor(method);
		}

		/**
		 * Return the {@link TypeDescriptor} for a nested type declared within the method parameter described by
		 * {@code nestingLevel} .
		 *
		 * @param nestingLevel the nesting level. {@code 0} is the first level, {@code 1} the next inner one.
		 * @return the {@link TypeDescriptor} or {@literal null} if it could not be obtained.
		 * @see TypeDescriptor#nested(MethodParameter, int)
		 */
		@Nullable
		TypeDescriptor getReturnTypeDescriptor(int nestingLevel) {

			// optimizing for nesting level 0 and 1 (Optional<T>, List<T>)
			// nesting level 2 (Optional<List<T>>) uses the slow path.

			return switch (nestingLevel) {
				case 0 -> typeDescriptor;
				case 1 -> nestedTypeDescriptor;
				default -> TypeDescriptor.nested(this.methodParameter, nestingLevel);
			};
		}
	}
}
