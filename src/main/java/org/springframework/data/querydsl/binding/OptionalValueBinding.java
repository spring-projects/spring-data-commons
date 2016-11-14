/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import java.util.Optional;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

/**
 * {@link OptionalValueBinding} creates a {@link Predicate} out of given {@link Path} and value. Used for specific
 * parameter treatment in {@link QuerydslBindings}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
@FunctionalInterface
public interface OptionalValueBinding<T extends Path<? extends S>, S> {

	/**
	 * Returns the predicate to be applied to the given {@link Path} for the given value. The given value will be the
	 * first the first one provided for the given path and converted into the expected type.
	 * 
	 * @param path {@link Path} to the property. Will not be {@literal null}.
	 * @param value the value that should be bound. Will not be {@literal null}.
	 * @return can be {@literal null}, in which case the binding will not be incorporated in the overall {@link Predicate}
	 *         .
	 */
	Optional<Predicate> bind(T path, Optional<? extends S> value);
}
