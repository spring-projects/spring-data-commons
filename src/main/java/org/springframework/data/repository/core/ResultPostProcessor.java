/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.core;

import org.springframework.lang.Nullable;

/**
 * Extension interfaces to register components that are interested in post processing repository method execution
 * results.
 * 
 * @author Oliver Gierke
 */
public interface ResultPostProcessor {

	/**
	 * A post-processor that takes a generic type so that it only gets invoked for repository method executions that
	 * return a value of the given type. Implementations cannot be defined via Lambda expressions, as they don't carry
	 * enough type information that's needed to properly detect the generic type to filter for.
	 *
	 * @author Oliver Gierke
	 */
	public interface ByType<T> extends ResultPostProcessor {

		@Nullable
		T postProcess(@Nullable T source);
	}

	/**
	 * A post-processor that only gets invoked for repository method executions that return instances of the aggregate
	 * root managed, i.e. all methods returning projections won't get the processor applied.
	 *
	 * @author Oliver Gierke
	 */
	public interface ForAggregate extends ByType<Object> {}
}
