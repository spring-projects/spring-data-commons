/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.spel.spi;

/**
 * Marker interface for Spring Data {@link org.springframework.expression.EvaluationContext} extensions.
 *
 * @author Mark Paluch
 * @since 2.4
 * @see EvaluationContextExtension
 * @see ReactiveEvaluationContextExtension
 */
public interface ExtensionIdAware {

	/**
	 * Return the identifier of the extension. The id can be leveraged by users to fully qualify property lookups and thus
	 * overcome ambiguities in case multiple extensions expose properties with the same name.
	 *
	 * @return the extension id, never {@literal null}.
	 */
	String getExtensionId();
}
