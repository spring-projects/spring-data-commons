/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.repository.util;

import org.springframework.core.convert.ConversionService;

/**
 * Defines a {@link NullableWrapper} converter to be used by {@link QueryExecutionConverters}.
 * {@link java.util.ServiceLoader} pattern can be used to register custom implementations of this interface.
 *
 * @author Darek Kaczynski
 */
public interface NullableWrapperConverter {

	/**
	 * Returns the types of wrappers supported by this converter.
	 *
	 * @return list of converter types.
	 */
	Class<?>[] getWrapperTypes();

	/**
	 * Returns the wrapped representation of {@code null} value.
	 *
	 * @return wrapped {@code null}.
	 */
	Object getNullValue();

	/**
	 * Decides if this converter can unwrap the value.
	 *
	 * @return true if unwrapping is possible, false otherwise.
	 */
	boolean canUnwrap();

	/**
	 * Performs wrapping.
	 *
	 * @param source the value to wrap.
	 * @param conversionService {@link ConversionService} to use if needed.
	 * @return wrapped value.
	 */
	Object wrap(Object source, ConversionService conversionService);

	/**
	 * Performs unwrapping.
	 *
	 * @param source wrapped value to unwrap.
	 * @return unwrapped value.
	 */
	Object unwrap(Object source);
}
