/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.repository.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.util.Assert;

/**
 * Exception to represent a failed attempt to convert a source value into a query method parameter.
 *
 * @author Oliver Gierke
 * @soundtrack Dave Matthews Band - The Dreaming Tree (DMB 2009-2018 Europe)
 * @since 1.11
 */
public class QueryMethodParameterConversionException extends RuntimeException {

	private static final long serialVersionUID = -5818002272039533066L;

	private final Object source;
	private final MethodParameter parameter;

	/**
	 * Creates a new {@link QueryMethodParameterConversionException} for the given source object, {@link MethodParameter}
	 * and root cause {@link ConversionException}.
	 *
	 * @param source can be {@literal null}.
	 * @param parameter the {@link MethodParameter} the value should've been converted for, must not be {@literal null}..
	 * @param cause the original {@link ConversionException}, must not be {@literal null}.
	 */
	public QueryMethodParameterConversionException(Object source, MethodParameter parameter, ConversionException cause) {

		super(String.format("Failed to convert %s into %s!", source, parameter.getParameterType().getName()), cause);

		Assert.notNull(parameter, "Method parameter must not be null!");
		Assert.notNull(cause, "ConversionException must not be null!");

		this.parameter = parameter;
		this.source = source;
	}

	/**
	 * Returns the source value that we failed converting.
	 *
	 * @return the source can be {@literal null}.
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Returns the {@link MethodParameter} we tried to convert the source value for.
	 *
	 * @return the parameter will never be {@literal null}.
	 * @see #getSource()
	 */
	public MethodParameter getParameter() {
		return parameter;
	}
}
