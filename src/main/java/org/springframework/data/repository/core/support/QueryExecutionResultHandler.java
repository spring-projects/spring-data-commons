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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.repository.util.NullableWrapper;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.lang.Nullable;

/**
 * Simple domain service to convert query results into a dedicated type.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class QueryExecutionResultHandler {

	private static final TypeDescriptor WRAPPER_TYPE = TypeDescriptor.valueOf(NullableWrapper.class);

	private final GenericConversionService conversionService;

	/**
	 * Creates a new {@link QueryExecutionResultHandler}.
	 */
	public QueryExecutionResultHandler() {

		GenericConversionService conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);

		this.conversionService = conversionService;
	}

	/**
	 * Post-processes the given result of a query invocation to match the return type of the given method.
	 *
	 * @param result can be {@literal null}.
	 * @param metho must not be {@literal null}.
	 * @return
	 */
	@Nullable
	public Object postProcessInvocationResult(@Nullable Object result, Method method) {

		if (method.getReturnType().isInstance(result)) {
			return result;
		}

		MethodParameter parameter = new MethodParameter(method, -1);
		TypeDescriptor methodReturnTypeDescriptor = TypeDescriptor.nested(parameter, 0);

		return postProcessInvocationResult(result, methodReturnTypeDescriptor);
	}

	/**
	 * Post-processes the given result of a query invocation to the given type.
	 *
	 * @param result can be {@literal null}.
	 * @param returnTypeDescriptor can be {@literal null}, if so, no conversion is performed.
	 * @return
	 */
	@Nullable
	Object postProcessInvocationResult(@Nullable Object result, @Nullable TypeDescriptor returnTypeDescriptor) {

		if (returnTypeDescriptor == null) {
			return result;
		}

		Class<?> expectedReturnType = returnTypeDescriptor.getType();

		// Early return if the raw value matches

		if (result != null && expectedReturnType.isInstance(result)) {
			return result;
		}

		result = unwrapOptional(result);

		// Early return if the unrwapped value matches

		if (result != null && expectedReturnType.isInstance(result)) {
			return result;
		}

		if (QueryExecutionConverters.supports(expectedReturnType)) {

			TypeDescriptor targetType = TypeDescriptor.valueOf(expectedReturnType);

			if (conversionService.canConvert(WRAPPER_TYPE, returnTypeDescriptor)
					&& !conversionService.canBypassConvert(WRAPPER_TYPE, targetType)) {

				return conversionService.convert(new NullableWrapper(result), expectedReturnType);
			}

			if (result != null
					&& conversionService.canConvert(TypeDescriptor.valueOf(result.getClass()), returnTypeDescriptor)
					&& !conversionService.canBypassConvert(TypeDescriptor.valueOf(result.getClass()), targetType)) {

				return conversionService.convert(result, expectedReturnType);
			}
		}

		if (result != null) {

			if (ReactiveWrapperConverters.supports(expectedReturnType)) {
				return ReactiveWrapperConverters.toWrapper(result, expectedReturnType);
			}

			return conversionService.canConvert(result.getClass(), expectedReturnType)
					? conversionService.convert(result, expectedReturnType)
					: result;
		}

		return Map.class.equals(expectedReturnType) //
				? CollectionFactory.createMap(expectedReturnType, 0) //
				: null;
	}

	/**
	 * Unwraps the given value if it's a JDK 8 {@link Optional}.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	private static Object unwrapOptional(@Nullable Object source) {

		if (source == null) {
			return null;
		}

		return Optional.class.isInstance(source) ? Optional.class.cast(source).orElse(null) : source;
	}
}
