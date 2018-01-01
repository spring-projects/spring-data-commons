/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.data.domain.ExampleMatcher;

/**
 * Accessor for the {@link ExampleMatcher} to use in modules that support query by example (QBE) querying.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 *
 * @since 1.12
 *
 * @deprecated use {@link org.springframework.data.support.ExampleMatcherAccessor} instead.
 */
@Deprecated
public class ExampleMatcherAccessor extends org.springframework.data.support.ExampleMatcherAccessor {


	/**
	 * Creates a new {@link ExampleMatcherAccessor} for the given {@link ExampleMatcher}.
	 *
	 * @param matcher must not be {@literal null}.
	 */
	public ExampleMatcherAccessor(ExampleMatcher matcher) {
		super(matcher);
	}
}
