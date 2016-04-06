/*
 * Copyright 2014 the original author or authors.
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

import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.repository.util.NullableWrapper;
import org.springframework.data.repository.util.QueryExecutionConverters;

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
	 * Post-processes the given result of a query invocation to the given type.
	 * 
	 * @param result can be {@literal null}.
	 * @param returnTypeDescriptor can be {@literal null}, if so, no conversion is performed.
	 * @return
	 */
	public Object postProcessInvocationResult(Object result, TypeDescriptor returnTypeDescriptor) {

		if (returnTypeDescriptor == null) {
			return result;
		}

		Class<?> expectedReturnType = returnTypeDescriptor.getType();

		if (result != null && expectedReturnType.isInstance(result)) {
			return result;
		}

		if (QueryExecutionConverters.supports(expectedReturnType)) {

			TypeDescriptor targetType = TypeDescriptor.valueOf(expectedReturnType);

			if(conversionService.canConvert(WRAPPER_TYPE, returnTypeDescriptor)
				&& !conversionService.canBypassConvert(WRAPPER_TYPE, targetType)) {

				return conversionService.convert(new NullableWrapper(result), expectedReturnType);
			}

			if(result != null && conversionService.canConvert(TypeDescriptor.valueOf(result.getClass()), returnTypeDescriptor)
				&& !conversionService.canBypassConvert(TypeDescriptor.valueOf(result.getClass()), targetType)) {

				return conversionService.convert(result, expectedReturnType);
			}
		}

		if (result != null) {
			return conversionService.canConvert(result.getClass(), expectedReturnType)
					? conversionService.convert(result, expectedReturnType) : result;
		}

		if (Map.class.equals(expectedReturnType)) {
			return CollectionFactory.createMap(expectedReturnType, 0);
		}

		return null;
	}

}
