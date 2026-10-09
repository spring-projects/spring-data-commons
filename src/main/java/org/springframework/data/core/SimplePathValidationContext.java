/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.data.core;

import java.util.Collections;
import java.util.Set;

/**
 * Contextual information about a path element.
 *
 * @author Christoph Strobl
 * @since 4.2
 */
class SimplePathValidationContext implements PathValidationRules.AccessValidationContext {

	private final String path;
	private final String property;
	private final TypeInformation<?> type;
	private final int index;
	private final String navigablePath;
	private final Set<String> deniedPaths;

	SimplePathValidationContext(String path, String property, TypeInformation<?> type, int index, String navigablePath,
		Set<String> deniedPaths) {

		this.path = path;
		this.property = property;
		this.type = type;
		this.index = index;
		this.navigablePath = navigablePath;
		this.deniedPaths = deniedPaths;
	}

	/**
	 * Returns the path reached so far as a chain of property names separated by {@code "."}, with keys and indexes left
	 * out, so that {@code phoneBook[WORK].city} is navigated as {@code phoneBook.city}. For a property segment this
	 * includes the segment itself, while for a key or an index it ends at the property the key or index is applied to.
	 *
	 * @return the navigable path reached so far, never {@literal null}.
	 */
	@Override
	public String navigablePath() {
		return navigablePath;
	}

	/**
	 * Returns the full property path being validated.
	 *
	 * @return the full property path being validated.
	 */
	@Override
	public String path() {
		return path;
	}

	/**
	 * Returns the property the path element belongs to, that is the property a key or index is applied to, or the one a
	 * segment follows. The segment or literal being checked is passed to the callback itself rather than exposed here.
	 *
	 * @return the owning property.
	 */
	@Override
	public String property() {
		return property;
	}

	/**
	 * Returns the type the path element is applied to, not unwrapped, so that a {@link java.util.Map} or indexed
	 * property can be told apart from its value or element type. Note that the type a check is about is passed to the
	 * callback itself; for a map {@link TypeInformation#getComponentType()} is the key and
	 * {@link TypeInformation#getMapValueType()} the value type, which is easy to mix up.
	 *
	 * @return the type the path element is applied to.
	 */
	@Override
	public TypeInformation<?> type() {
		return type;
	}

	/**
	 * Returns the index of the path element within {@link #path()}, counted in characters.
	 *
	 * @return the character index the path element starts at.
	 */
	public int index() {
		return index;
	}

	/**
	 * Returns the collection of paths that must not be accessible.
	 *
	 * @return the character index the path element starts at.
	 */
	public Set<String> deniedPaths() {
		return Collections.unmodifiableSet(deniedPaths);
	}

	/**
	 * Creates the {@link PathResolutionException} to throw for the given reason. The reason becomes part of the
	 * {@link PathResolutionException#getDetailedMessage() detailed message} and is therefore free to name types and
	 * properties; it does not reach {@link PathResolutionException#getMessage()}. Returned rather than thrown so that
	 * control flow stays visible to the compiler and the reader, as in {@code throw context.reject("…")}; a rule
	 * returning a value could not otherwise reject without a dead {@code return} behind the call.
	 *
	 * @param reason why the path element was rejected.
	 * @return the exception to throw.
	 */
	@Override
	public PathResolutionException reject(String reason) {
		return new PathResolutionException(path,
			String.format("Property path '%s' is invalid; %s", PathResolutionException.abbreviate(path), reason));
	}

	@Override
	public String toString() {
		return String.format("PathValidationContext[path='%s', property='%s', type=%s, index=%d, navigablePath='%s']",
			path, property, type.getType().getSimpleName(), index, navigablePath);
	}
}
