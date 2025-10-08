/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.mapping;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;

import org.springframework.data.util.TypeInformation;

/**
 * @author Mark Paluch
 */
@FunctionalInterface
public interface TypedPropertyPath<T, R> extends Serializable, PropertyPath {

	R get(T obj);

	@Override
	default TypeInformation<?> getOwningType() {
		return PropertyPathExtractor.getPropertyPathInformation(this).owner();
	}

	@Override
	default String getSegment() {
		return PropertyPathExtractor.getPropertyPathInformation(this).property().getName();
	}

	@Override
	default TypeInformation<?> getTypeInformation() {
		return PropertyPathExtractor.getPropertyPathInformation(this).propertyType();
	}

	@Override
	@Nullable
	default PropertyPath next() {
		return null;
	}

	@Override
	default boolean hasNext() {
		return false;
	}

	@Override
	default Iterator<PropertyPath> iterator() {
		return Collections.singletonList((PropertyPath) this).iterator();
	}

	default <N> TypedPropertyPath<T, N> then(TypedPropertyPath<R, N> next) {
		return new ComposedPropertyPath<>(this, next);
	}

}
