/*
 * Copyright 2013-2018 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;

/**
 * Default implementation of {@link Parameters}.
 *
 * @author Oliver Gierke
 */
public final class DefaultParameters extends Parameters<DefaultParameters, Parameter> {

	/**
	 * Creates a new {@link DefaultParameters} instance from the given {@link Method}.
	 *
	 * @param method must not be {@literal null}.
	 */
	public DefaultParameters(Method method) {
		super(method);
	}

	private DefaultParameters(List<Parameter> parameters) {
		super(parameters);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.Parameters#createParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	protected Parameter createParameter(MethodParameter parameter) {
		return new Parameter(parameter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.Parameters#createFrom(java.util.List)
	 */
	@Override
	protected DefaultParameters createFrom(List<Parameter> parameters) {
		return new DefaultParameters(parameters);
	}
}
