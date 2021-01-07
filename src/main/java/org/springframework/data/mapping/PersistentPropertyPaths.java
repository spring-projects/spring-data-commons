/*
 * Copyright 2018-2021 the original author or authors.
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

import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.data.util.Streamable;

/**
 * A wrapper for a collection of {@link PersistentPropertyPath}s.
 *
 * @author Oliver Gierke
 * @since 2.1
 * @soundtrack Stuart McCallum - North Star (City)
 */
public interface PersistentPropertyPaths<T, P extends PersistentProperty<P>>
		extends Streamable<PersistentPropertyPath<P>> {

	/**
	 * Returns the first {@link PersistentPropertyPath}.
	 *
	 * @return
	 */
	Optional<PersistentPropertyPath<P>> getFirst();

	/**
	 * Returns whether the given path is contained in the current {@link PersistentPropertyPaths}.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	boolean contains(String path);

	/**
	 * Returns whether the given {@link PropertyPath} is contained in the current {@link PersistentPropertyPaths}.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	boolean contains(PropertyPath path);

	/**
	 * Drops {@link PersistentPropertyPath}s that contain a path segment matching the given predicate.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return a {@link PersistentPropertyPaths} instance with all {@link PersistentPropertyPath} instances removed that
	 *         contain path segments matching the given predicate.
	 * @since 2.1.4 / 2.2.2
	 */
	PersistentPropertyPaths<T, P> dropPathIfSegmentMatches(Predicate<? super P> predicate);
}
