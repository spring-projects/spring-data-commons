/*
 * Copyright 2013-present the original author or authors.
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

import java.util.List;

/**
 * Default implementation of {@link Parameters}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public final class DefaultParameters extends Parameters<DefaultParameters, Parameter> {

	/**
	 * Creates a new {@link DefaultParameters} instance from the given {@link ParametersSource}.
	 *
	 * @param parametersSource must not be {@literal null}.
	 * @since 3.2.1
	 */
	public DefaultParameters(ParametersSource parametersSource) {
		super(parametersSource, param -> new Parameter(param, parametersSource.getDomainTypeInformation()));
	}

	private DefaultParameters(List<Parameter> parameters) {
		super(parameters);
	}

	@Override
	protected DefaultParameters createFrom(List<Parameter> parameters) {
		return new DefaultParameters(parameters);
	}
}
