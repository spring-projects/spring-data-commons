/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.lang.Nullable;

/**
 * SPI for components that can evaluate Value expressions.
 *
 * @author Mark Paluch
 * @since 3.2
 */
public interface ValueExpressionEvaluator {

	/**
	 * Evaluates the given expression.
	 *
	 * @param expression
	 * @return
	 */
	@Nullable
	<T> T evaluate(String expression);
}
